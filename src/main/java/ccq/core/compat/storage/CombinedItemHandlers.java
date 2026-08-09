package ccq.core.compat.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Read-only composition of multiple {@link IItemHandler}s for capability exposure.
 */
public final class CombinedItemHandlers implements IItemHandler {
    private final IItemHandler[] handlers;
    private final int[] slotOffsets;
    private final int totalSlots;

    public CombinedItemHandlers(IItemHandler... handlers) {
        this.handlers = Arrays.stream(handlers).filter(handler -> handler != null && handler.getSlots() > 0).toArray(IItemHandler[]::new);
        this.slotOffsets = new int[this.handlers.length];
        int slots = 0;
        for (int i = 0; i < this.handlers.length; i++) {
            slotOffsets[i] = slots;
            slots += this.handlers[i].getSlots();
        }
        this.totalSlots = slots;
    }

    @Override
    public int getSlots() {
        return totalSlots;
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
        if (slot < 0 || slot >= totalSlots) {
            return null;
        }
        for (int i = handlers.length - 1; i >= 0; i--) {
            if (slot >= slotOffsets[i]) {
                return new Resolved(handlers[i], slot - slotOffsets[i]);
            }
        }
        return null;
    }

    private record Resolved(IItemHandler handler, int slot) {
    }
}
