package ccq.core.compat.storage;

import ccq.core.compat.storage.mixin.StorageNetworkAccessor;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FXNT network inventory plus Functional Storage inventories exposed by linked bridges.
 */
public final class BridgedFxntNetworkHandler implements IItemHandlerModifiable {
    private final IItemHandlerModifiable nativeHandler;
    private final StorageNetwork network;

    private @Nullable List<BlockPos> cachedBridgePositions;
    private int cachedPositionsNetworkVersion = Integer.MIN_VALUE;
    private @Nullable List<IItemHandler> cachedBridgeHandlers;
    private long cachedBridgeGenKey = Long.MIN_VALUE;

    public BridgedFxntNetworkHandler(IItemHandlerModifiable nativeHandler, StorageNetwork network) {
        this.nativeHandler = nativeHandler;
        this.network = network;
    }

    public IItemHandlerModifiable getNativeHandler() {
        return nativeHandler;
    }

    private List<BlockPos> bridgePositions() {
        StorageNetworkAccessor accessor = (StorageNetworkAccessor) (Object) network;
        int networkVersion = accessor.ccq$getNetworkVersion();
        if (cachedBridgePositions != null && cachedPositionsNetworkVersion == networkVersion) {
            return cachedBridgePositions;
        }

        List<BlockPos> positions = new ArrayList<>();
        Set<BlockPos> components = accessor.ccq$getComponents();
        StorageControllerEntity controller = accessor.ccq$getController();
        Level level = controller == null ? null : controller.getLevel();
        if (components != null && level != null) {
            for (BlockPos pos : components) {
                if (level.getBlockEntity(pos) instanceof StorageBridgeBlockEntity) {
                    positions.add(pos);
                }
            }
        }
        cachedBridgePositions = positions;
        cachedPositionsNetworkVersion = networkVersion;
        cachedBridgeHandlers = null;
        return positions;
    }

    private List<IItemHandler> bridgeHandlers() {
        StorageNetworkAccessor accessor = (StorageNetworkAccessor) (Object) network;
        StorageControllerEntity controller = accessor.ccq$getController();
        Level level = controller == null ? null : controller.getLevel();
        List<BlockPos> positions = bridgePositions();

        long genKey = positions.size();
        if (level != null) {
            for (BlockPos pos : positions) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StorageBridgeBlockEntity bridge) {
                    genKey = 31L * genKey + bridge.getCacheGeneration();
                    genKey = 31L * genKey + bridge.getFsDrawersListKey();
                }
            }
        }

        if (cachedBridgeHandlers != null && cachedBridgeGenKey == genKey) {
            return cachedBridgeHandlers;
        }

        List<IItemHandler> handlers = new ArrayList<>();
        if (level != null) {
            for (BlockPos pos : positions) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StorageBridgeBlockEntity bridge) {
                    IItemHandler fs = bridge.getStorageForFxntNetwork();
                    if (fs != null && fs.getSlots() > 0) {
                        handlers.add(fs);
                    }
                }
            }
        }

        cachedBridgeHandlers = handlers;
        cachedBridgeGenKey = genKey;
        return handlers;
    }

    private int nativeSlots() {
        return nativeHandler.getSlots();
    }

    private Resolved resolve(int slot, List<IItemHandler> bridges) {
        int nativeSlots = nativeSlots();
        if (slot < nativeSlots) {
            return new Resolved(nativeHandler, slot, true);
        }
        int index = slot - nativeSlots;
        for (IItemHandler handler : bridges) {
            int size = handler.getSlots();
            if (index < size) {
                return new Resolved(handler, index, false);
            }
            index -= size;
        }
        return null;
    }

    @Override
    public int getSlots() {
        int total = nativeSlots();
        for (IItemHandler handler : bridgeHandlers()) {
            total += handler.getSlots();
        }
        return total;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        List<IItemHandler> bridges = bridgeHandlers();
        Resolved resolved = resolve(slot, bridges);
        return resolved == null ? ItemStack.EMPTY : resolved.handler.getStackInSlot(resolved.slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        List<IItemHandler> bridges = bridgeHandlers();
        ItemStack remainder = stack;
        if (nativeSlots() > 0) {
            // FXNT NetworkItemHandler routes by item type, not by absolute slot.
            remainder = nativeHandler.insertItem(0, remainder, simulate);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        Resolved resolved = resolve(slot, bridges);
        if (resolved != null && !resolved.nativeSide) {
            remainder = resolved.handler.insertItem(resolved.slot, remainder, simulate);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (IItemHandler handler : bridges) {
            remainder = ItemHandlerHelper.insertItemStacked(handler, remainder, simulate);
            if (remainder.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        Resolved resolved = resolve(slot, bridgeHandlers());
        return resolved == null ? ItemStack.EMPTY : resolved.handler.extractItem(resolved.slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        Resolved resolved = resolve(slot, bridgeHandlers());
        return resolved == null ? 0 : resolved.handler.getSlotLimit(resolved.slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        Resolved resolved = resolve(slot, bridgeHandlers());
        return resolved != null && resolved.handler.isItemValid(resolved.slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        Resolved resolved = resolve(slot, bridgeHandlers());
        if (resolved == null) {
            return;
        }
        if (resolved.handler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(resolved.slot, stack);
            return;
        }
        resolved.handler.extractItem(resolved.slot, Integer.MAX_VALUE, false);
        if (!stack.isEmpty()) {
            resolved.handler.insertItem(resolved.slot, stack, false);
        }
    }

    private record Resolved(IItemHandler handler, int slot, boolean nativeSide) {
    }
}
