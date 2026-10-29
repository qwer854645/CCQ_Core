package ccq.core.compat.cmr;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.ToIntFunction;

/** A higher coolant tier can replace the current contents, including during simulation. */
final class CoolantTank extends FluidTank {
    private final ToIntFunction<FluidStack> tier;

    CoolantTank(int capacity, ToIntFunction<FluidStack> tier) {
        super(capacity, stack -> tier.applyAsInt(stack) >= 0);
        this.tier = tier;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !isFluidValid(resource)) {
            return 0;
        }
        if (!isEmpty() && tier.applyAsInt(resource) > tier.applyAsInt(getFluid())) {
            int accepted = Math.min(getCapacity(), resource.getAmount());
            if (action.execute()) {
                setFluid(resource.copyWithAmount(accepted));
                onContentsChanged();
            }
            return accepted;
        }
        return super.fill(resource, action);
    }

    static boolean shouldConsume(int incomingTier, int activeTier, int remainingTicks, int threshold) {
        if (incomingTier < activeTier) {
            return remainingTicks <= 0;
        }
        return incomingTier > activeTier || remainingTicks <= threshold;
    }
}
