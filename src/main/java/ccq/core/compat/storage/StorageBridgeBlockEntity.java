package ccq.core.compat.storage;

import ccq.core.compat.storage.mixin.StorageNetworkAccessor;
import com.buuz135.functionalstorage.block.tile.ItemControllableDrawerTile;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.controller.StorageInterfaceEntity;
import net.fxnt.fxntstorage.simple_storage.SimpleStorageBoxEntity;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StorageBridgeBlockEntity extends BlockEntity {
    private static final int TRANSFER_INTERVAL = 8;
    private static final int MAX_TRANSFER_PER_PULSE = 64;
    private static final int SLOT_INVALIDATE_COOLDOWN = 20;
    private static final int AUTO_LINK_INTERVAL = 40;

    private @Nullable BlockPos fsControllerPos;
    private @Nullable BlockPos fxntControllerPos;
    private int tickCounter;
    private int lastReportedFxntSlots = -1;
    private long nextSlotInvalidateGameTime;
    private long nextAutoLinkGameTime;

    private int cacheGeneration;
    private @Nullable IItemHandler cachedFsDrawers;
    private long cachedFsDrawersKey = Long.MIN_VALUE;
    private @Nullable IItemHandler cachedExposed;
    private long cachedExposedKey = Long.MIN_VALUE;

    public StorageBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBridgeBlocks.STORAGE_BRIDGE_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StorageBridgeBlockEntity bridge) {
        bridge.tickCounter++;
        if (bridge.tickCounter < TRANSFER_INTERVAL) {
            return;
        }
        bridge.tickCounter = 0;
        bridge.validateLinks();
        bridge.maybeAutoLinkNeighbors(level);
        bridge.refreshFunctionalStorageSelectors();
        // When both networks proxy through this bridge, skip bulk transfer to avoid loops.
        if (bridge.fxntControllerPos != null && bridge.fsControllerPos != null) {
            return;
        }
        bridge.transferPulse();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && fsControllerPos != null) {
            StorageBridgeRegistry.register(level, fsControllerPos, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && fsControllerPos != null) {
            StorageBridgeRegistry.unregister(level, fsControllerPos, worldPosition);
        }
        clearCaches();
        super.setRemoved();
    }

    public void setFxntController(StorageControllerEntity controller) {
        if (controller == null) {
            return;
        }
        BlockPos next = controller.getBlockPos();
        if (next.equals(this.fxntControllerPos)) {
            return;
        }
        this.fxntControllerPos = next;
        clearCaches();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
        // Structure change on FXNT side — refresh FS selectors (slots may appear).
        invalidateFunctionalStorageSlotsOnly();
    }

    public void forgetFxntController() {
        if (this.fxntControllerPos == null) {
            return;
        }
        this.fxntControllerPos = null;
        clearCaches();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
        invalidateFunctionalStorageSlotsOnly();
    }

    public void setFsController(BlockPos controllerPos) {
        BlockPos next = controllerPos.immutable();
        if (next.equals(this.fsControllerPos)) {
            StorageBridgeRegistry.register(level, next, worldPosition);
            return;
        }
        BlockPos previous = this.fsControllerPos;
        this.fsControllerPos = next;
        if (level != null) {
            StorageBridgeRegistry.rebind(level, previous, next, worldPosition);
        }
        clearCaches();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public void clearFsController() {
        if (this.fsControllerPos == null) {
            return;
        }
        if (level != null) {
            StorageBridgeRegistry.unregister(level, fsControllerPos, worldPosition);
        }
        this.fsControllerPos = null;
        clearCaches();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public @Nullable BlockPos getFsControllerPos() {
        return fsControllerPos;
    }

    /**
     * FS caches slot selectors at invalidate time. When FXNT slot count changes, refresh selectors
     * only — avoid full ConnectedDrawers.rebuild() on every jitter.
     */
    private void refreshFunctionalStorageSelectors() {
        if (fsControllerPos == null || level == null) {
            return;
        }
        int slots = getStorageForFunctionalStorage().getSlots();
        if (slots == lastReportedFxntSlots) {
            return;
        }
        lastReportedFxntSlots = slots;
        if (level.getGameTime() < nextSlotInvalidateGameTime) {
            return;
        }
        nextSlotInvalidateGameTime = level.getGameTime() + SLOT_INVALIDATE_COOLDOWN;
        invalidateFunctionalStorageSlotsOnly();
    }

    private void invalidateFunctionalStorageSlotsOnly() {
        if (level == null || fsControllerPos == null) {
            return;
        }
        if (level.getBlockEntity(fsControllerPos) instanceof StorageControllerTile<?> controller
                && controller.inventoryHandler != null) {
            controller.inventoryHandler.invalidateSlots();
        }
    }

    /**
     * Full rebuild when membership must be restored (link/unlink). Prefer invalidate for slot churn.
     */
    private void rebuildFunctionalStorageConnections() {
        if (level == null || fsControllerPos == null) {
            return;
        }
        if (level.getBlockEntity(fsControllerPos) instanceof StorageControllerTile<?> controller) {
            controller.getConnectedDrawers().rebuild();
            if (controller.inventoryHandler != null) {
                controller.inventoryHandler.invalidateSlots();
            }
        }
    }

    private void maybeAutoLinkNeighbors(Level level) {
        if (fsControllerPos != null) {
            return;
        }
        if (level.getGameTime() < nextAutoLinkGameTime) {
            return;
        }
        nextAutoLinkGameTime = level.getGameTime() + AUTO_LINK_INTERVAL;
        tryAutoLinkFunctionalStorage();
    }

    /**
     * When this bridge joins an FXNT network, also latch onto an adjacent FS controller.
     */
    public void tryAutoLinkFunctionalStorage() {
        if (level == null || fsControllerPos != null) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            if (!(level.getBlockEntity(neighbor) instanceof StorageControllerTile<?> controller)) {
                continue;
            }
            setFsController(neighbor);
            long packed = worldPosition.asLong();
            List<Long> connected = controller.getConnectedDrawers().getConnectedDrawers();
            if (!connected.contains(packed)) {
                connected.add(packed);
            }
            rebuildFunctionalStorageConnections();
            controller.markForUpdate();
            return;
        }
    }

    public Component describeLinks() {
        boolean fxnt = resolveNativeFxntHandler() != null || resolveAdjacentFxntHandler() != null;
        boolean fs = resolveNativeFsDrawers() != null || resolveAdjacentFsDrawerHandler() != null;
        return Component.translatable(
                "message.ccq_core.storage_bridge.status",
                fxnt ? Component.translatable("message.ccq_core.storage_bridge.side_ok")
                        : Component.translatable("message.ccq_core.storage_bridge.side_missing"),
                fs ? Component.translatable("message.ccq_core.storage_bridge.side_ok")
                        : Component.translatable("message.ccq_core.storage_bridge.side_missing")
        );
    }

    /**
     * Inventory Functional Storage sees when this bridge is linked.
     * Must be native FXNT only (never the bridged wrapper), or recursion happens.
     */
    public IItemHandler getStorageForFunctionalStorage() {
        IItemHandler fxnt = resolveNativeFxntHandler();
        if (fxnt == null) {
            fxnt = resolveAdjacentFxntHandler();
        }
        return fxnt != null ? fxnt : EmptyItemHandler.INSTANCE;
    }

    /**
     * Inventory FXNT network sees through this bridge.
     * Must be FS drawers only (never the FS controller aggregate that includes this bridge).
     */
    public IItemHandler getStorageForFxntNetwork() {
        IItemHandler fs = resolveNativeFsDrawers();
        if (fs == null) {
            fs = resolveAdjacentFsDrawerHandler();
        }
        return fs != null ? fs : EmptyItemHandler.INSTANCE;
    }

    /** Bumps whenever this bridge's proxy targets may have changed. */
    public int getCacheGeneration() {
        return cacheGeneration;
    }

    /** Cheap fingerprint of the linked FS drawer membership list. */
    public long getFsDrawersListKey() {
        if (level == null || fsControllerPos == null) {
            return -1L;
        }
        if (!(level.getBlockEntity(fsControllerPos) instanceof StorageControllerTile<?> controller)) {
            return -2L;
        }
        return computeConnectedDrawersKey(controller.getConnectedDrawers().getConnectedDrawers());
    }

    public IItemHandler getExposedItemHandler() {
        long key = ((long) cacheGeneration << 32)
                ^ (fxntControllerPos == null ? 0 : fxntControllerPos.hashCode())
                ^ (fsControllerPos == null ? 0 : fsControllerPos.hashCode());
        if (cachedExposed != null && key == cachedExposedKey) {
            return cachedExposed;
        }
        IItemHandler fxnt = getStorageForFunctionalStorage();
        IItemHandler fs = getStorageForFxntNetwork();
        boolean hasFxnt = fxnt.getSlots() > 0;
        boolean hasFs = fs.getSlots() > 0;
        IItemHandler exposed;
        if (hasFxnt && hasFs) {
            exposed = new CombinedItemHandlers(fxnt, fs);
        } else if (hasFxnt) {
            exposed = fxnt;
        } else if (hasFs) {
            exposed = fs;
        } else {
            exposed = EmptyItemHandler.INSTANCE;
        }
        cachedExposed = exposed;
        cachedExposedKey = key;
        return exposed;
    }

    private void validateLinks() {
        if (level == null) {
            return;
        }
        if (fxntControllerPos != null && !(level.getBlockEntity(fxntControllerPos) instanceof StorageControllerEntity)) {
            forgetFxntController();
        }
        if (fsControllerPos != null && !(level.getBlockEntity(fsControllerPos) instanceof StorageControllerTile<?>)) {
            clearFsController();
        }
    }

    private void transferPulse() {
        IItemHandler fxnt = resolveAdjacentFxntHandler();
        IItemHandler fs = resolveAdjacentFsDrawerHandler();
        if (fxnt == null || fs == null || fxnt == fs) {
            return;
        }
        moveSome(fxnt, fs, MAX_TRANSFER_PER_PULSE / 2);
        moveSome(fs, fxnt, MAX_TRANSFER_PER_PULSE / 2);
    }

    private static void moveSome(IItemHandler from, IItemHandler to, int budget) {
        int remaining = budget;
        int slots = from.getSlots();
        for (int slot = 0; slot < slots && remaining > 0; slot++) {
            ItemStack extracted = from.extractItem(slot, remaining, true);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(to, extracted, true);
            int movable = extracted.getCount() - leftover.getCount();
            if (movable <= 0) {
                continue;
            }
            ItemStack taken = from.extractItem(slot, movable, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack rejected = ItemHandlerHelper.insertItemStacked(to, taken, false);
            remaining -= taken.getCount() - rejected.getCount();
            if (!rejected.isEmpty()) {
                ItemHandlerHelper.insertItemStacked(from, rejected, false);
            }
        }
    }

    private @Nullable IItemHandler resolveNativeFxntHandler() {
        if (level == null || fxntControllerPos == null) {
            return null;
        }
        if (!(level.getBlockEntity(fxntControllerPos) instanceof StorageControllerEntity controller)) {
            return null;
        }
        StorageNetwork network = controller.getConnectedNetwork();
        if (network == null) {
            return null;
        }
        return ((StorageNetworkAccessor) (Object) network).ccq$getNativeItemHandler();
    }

    /**
     * Linked FS drawers only — skips other storage bridges to prevent recursion.
     */
    private @Nullable IItemHandler resolveNativeFsDrawers() {
        if (level == null || fsControllerPos == null) {
            return null;
        }
        if (!(level.getBlockEntity(fsControllerPos) instanceof StorageControllerTile<?> controller)) {
            return null;
        }
        List<Long> connected = controller.getConnectedDrawers().getConnectedDrawers();
        long key = computeConnectedDrawersKey(connected);
        if (cachedFsDrawers != null && key == cachedFsDrawersKey) {
            return cachedFsDrawers;
        }

        List<IItemHandler> handlers = new ArrayList<>();
        for (Long packed : connected) {
            BlockPos pos = BlockPos.of(packed);
            if (pos.equals(worldPosition)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageBridgeBlockEntity) {
                continue;
            }
            if (be instanceof ItemControllableDrawerTile<?> drawer) {
                IItemHandler storage = drawer.getStorage();
                if (storage != null && storage.getSlots() > 0) {
                    handlers.add(storage);
                }
            }
        }

        IItemHandler result;
        if (handlers.isEmpty()) {
            result = null;
        } else if (handlers.size() == 1) {
            result = handlers.getFirst();
        } else {
            result = new CombinedItemHandlers(handlers.toArray(IItemHandler[]::new));
        }
        cachedFsDrawers = result;
        cachedFsDrawersKey = key;
        return result;
    }

    private static long computeConnectedDrawersKey(List<Long> connected) {
        long key = connected.size();
        for (Long packed : connected) {
            key = 31L * key + packed;
        }
        return key;
    }

    private @Nullable IItemHandler resolveAdjacentFxntHandler() {
        if (level == null) {
            return null;
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof StorageControllerEntity controller) {
                StorageNetwork network = controller.getConnectedNetwork();
                if (network != null) {
                    return ((StorageNetworkAccessor) (Object) network).ccq$getNativeItemHandler();
                }
                // Prefer native; wrapped getItemHandler is last resort.
                IItemHandler handler = controller.getItemHandler();
                if (handler instanceof BridgedFxntNetworkHandler bridged) {
                    return bridged.getNativeHandler();
                }
                return handler;
            }
            if (be instanceof StorageInterfaceEntity storageInterface) {
                IItemHandler handler = storageInterface.getItemHandler();
                if (handler.getSlots() > 0) {
                    return handler;
                }
            }
            if (be instanceof SimpleStorageBoxEntity box) {
                return box.getItemHandler();
            }
            if (isFxntNamespace(be)) {
                IItemHandler cap = level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, direction.getOpposite());
                if (cap != null) {
                    return cap;
                }
            }
        }
        return null;
    }

    private @Nullable IItemHandler resolveAdjacentFsDrawerHandler() {
        if (level == null) {
            return null;
        }
        List<IItemHandler> handlers = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(neighbor);
            if (be instanceof StorageControllerTile<?> controller) {
                if (fsControllerPos == null) {
                    setFsController(neighbor);
                    long packed = worldPosition.asLong();
                    List<Long> connected = controller.getConnectedDrawers().getConnectedDrawers();
                    if (!connected.contains(packed)) {
                        connected.add(packed);
                    }
                    rebuildFunctionalStorageConnections();
                }
                IItemHandler drawers = resolveNativeFsDrawers();
                if (drawers != null) {
                    return drawers;
                }
            }
            if (be instanceof ItemControllableDrawerTile<?> drawer && !(be instanceof StorageBridgeBlockEntity)) {
                IItemHandler storage = drawer.getStorage();
                if (storage != null && storage.getSlots() > 0) {
                    handlers.add(storage);
                }
            }
        }
        if (handlers.isEmpty()) {
            return null;
        }
        if (handlers.size() == 1) {
            return handlers.getFirst();
        }
        return new CombinedItemHandlers(handlers.toArray(IItemHandler[]::new));
    }

    private void clearCaches() {
        cacheGeneration++;
        cachedFsDrawers = null;
        cachedFsDrawersKey = Long.MIN_VALUE;
        cachedExposed = null;
        cachedExposedKey = Long.MIN_VALUE;
        lastReportedFxntSlots = -1;
    }

    private static boolean isFxntNamespace(@Nullable BlockEntity be) {
        return be != null && be.getType().builtInRegistryHolder().key().location().getNamespace().equals("fxntstorage");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (fsControllerPos != null) {
            tag.putLong("FsController", fsControllerPos.asLong());
        }
        if (fxntControllerPos != null) {
            tag.putLong("FxntController", fxntControllerPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fsControllerPos = tag.contains("FsController") ? BlockPos.of(tag.getLong("FsController")) : null;
        fxntControllerPos = tag.contains("FxntController") ? BlockPos.of(tag.getLong("FxntController")) : null;
        clearCaches();
    }
}
