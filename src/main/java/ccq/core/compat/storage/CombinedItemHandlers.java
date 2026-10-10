package ccq.core.compat.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Composition of multiple {@link IItemHandler}s for capability exposure.
 * {@link #insertItem} stays slot-strict so {@code ItemHandlerHelper.insertItemStacked}
 * (used by Create funnels) can walk slots correctly across both networks.
 */
public final class CombinedItemHandlers implements IItemHandler {
    private final IItemHandler[] handlers;

    public CombinedItemHandlers(IItemHandler... handlers) {
        this.handlers = Arrays.stream(handlers).filter(handler -> handler != null).toArray(IItemHandler[]::new);
    }

    @Override
    public int getSlots() {
        int slots = 0;
        for (IItemHandler handler : handlers) {
            slots += handler.getSlots();
        }
        return slots;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        Resolved resolved = resolve(slot);
        return resolved == null ? ItemStack.EMPTY : resolved.handler.getStackInSlot(resolved.slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        Resolved resolved = resolve(slot);
        return resolved == null ? stack : resolved.handler.insertItem(resolved.slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        Resolved resolved = resolve(slot);
        return resolved == null ? ItemStack.EMPTY : resolved.handler.extractItem(resolved.slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        Resolved resolved = resolve(slot);
        return resolved == null ? 0 : resolved.handler.getSlotLimit(resolved.slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        Resolved resolved = resolve(slot);
        return resolved != null && resolved.handler.isItemValid(resolved.slot, stack);
    }

    private Resolved resolve(int slot) {
        if (slot < 0) {
            return null;
        }
        for (IItemHandler handler : handlers) {
            int slots = handler.getSlots();
            if (slot < slots) {
                return new Resolved(handler, slot);
            }
            slot -= slots;
        }
        return null;
    }

    private record Resolved(IItemHandler handler, int slot) {
    }
}
