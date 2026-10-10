package ccq.core.compat.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinedItemHandlersTest {
    @Test
    void growingAndShrinkingFirstInventoryUpdatesSecondInventoryOffset() {
        MutableHandler first = new MutableHandler(2, 100);
        MutableHandler second = new MutableHandler(3, 200);
        CombinedItemHandlers combined = new CombinedItemHandlers(first, second);
        assertEquals(5, combined.getSlots());
        assertEquals(200, combined.getSlotLimit(2));
        first.slots = 4;
        assertEquals(7, combined.getSlots());
        assertEquals(103, combined.getSlotLimit(3));
        assertEquals(200, combined.getSlotLimit(4));
        first.slots = 0;
        assertEquals(3, combined.getSlots());
        assertEquals(200, combined.getSlotLimit(0));
        assertEquals(0, combined.getSlotLimit(3));
    }

    @Test
    void initiallyEmptyInventoryCanGainSlotsAndInvalidSlotsAreRejected() {
        MutableHandler first = new MutableHandler(0, 100);
        CombinedItemHandlers combined = new CombinedItemHandlers(first);
        assertEquals(0, combined.getSlots());
        first.slots = 2;
        assertEquals(2, combined.getSlots());
        assertEquals(101, combined.getSlotLimit(1));
        assertEquals(0, combined.getSlotLimit(-1));
        assertEquals(0, combined.getSlotLimit(2));
    }

    private static class MutableHandler implements IItemHandler {
        int slots;
        final int limitBase;
        MutableHandler(int slots, int limitBase) {
            this.slots = slots;
            this.limitBase = limitBase;
        }
        public int getSlots() { return slots; }
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= slots) throw new IndexOutOfBoundsException(slot);
            return limitBase + slot;
        }
        public ItemStack getStackInSlot(int slot) { throw new UnsupportedOperationException(); }
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { throw new UnsupportedOperationException(); }
        public ItemStack extractItem(int slot, int amount, boolean simulate) { throw new UnsupportedOperationException(); }
        public boolean isItemValid(int slot, ItemStack stack) { return false; }
    }
}
