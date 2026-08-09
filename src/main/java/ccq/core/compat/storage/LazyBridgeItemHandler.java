package ccq.core.compat.storage;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Live-delegating handler so FS can keep a stable reference while FXNT network slots change.
 */
public final class LazyBridgeItemHandler implements IItemHandler {
    private final Supplier<IItemHandler> supplier;

    public LazyBridgeItemHandler(Supplier<IItemHandler> supplier) {
        this.supplier = supplier;
    }

    private IItemHandler delegate() {
        IItemHandler handler = supplier.get();
        return handler == null ? EmptyItemHandler.INSTANCE : handler;
    }

    @Override
    public int getSlots() {
        return delegate().getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return delegate().getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return delegate().insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return delegate().extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return delegate().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return delegate().isItemValid(slot, stack);
    }
}
