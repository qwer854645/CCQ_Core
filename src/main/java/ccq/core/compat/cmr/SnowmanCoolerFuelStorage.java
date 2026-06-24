package ccq.core.compat.cmr;

import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

final class SnowmanCoolerFuelStorage implements IItemHandlerModifiable {
    private final SnowmanCoolerBlockEntity cooler;

    SnowmanCoolerFuelStorage(SnowmanCoolerBlockEntity cooler) {
        this.cooler = cooler;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == 0 ? SnowmanCoolerItems.fuelDisplayStack(cooler) : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
}
