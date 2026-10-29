package ccq.core.compat.cmr;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoolantTankTest {
    @BeforeAll
    static void bootstrap() {
        // Standalone unit tests have no loader session or client resource bundle.
        // The test-only empty language file supplies the resource Bootstrap expects.
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private CoolantTank tank() {
        // Use vanilla fluids to exercise tier replacement without a running mod registry.
        return new CoolantTank(4000, stack -> stack.getFluid() == Fluids.WATER ? 0
                : stack.getFluid() == Fluids.LAVA ? 1 : -1);
    }

    @Test
    void simulatedUpgradeMatchesExecutionWithoutChangingWater() {
        CoolantTank tank = tank();
        tank.fill(new FluidStack(Fluids.WATER, 4000), FluidAction.EXECUTE);
        FluidStack coolant = new FluidStack(Fluids.LAVA, 1000);
        assertEquals(1000, tank.fill(coolant, FluidAction.SIMULATE));
        assertEquals(Fluids.WATER, tank.getFluid().getFluid());
        assertEquals(4000, tank.getFluidAmount());
        assertEquals(1000, tank.fill(coolant, FluidAction.EXECUTE));
        assertEquals(Fluids.LAVA, tank.getFluid().getFluid());
        assertEquals(1000, tank.getFluidAmount());
        assertEquals(1000, coolant.getAmount());
    }

    @Test
    void upgradeRespectsCapacityAndLowerTierCannotReplaceCoolant() {
        CoolantTank tank = tank();
        tank.fill(new FluidStack(Fluids.WATER, 500), FluidAction.EXECUTE);
        FluidStack coolant = new FluidStack(Fluids.LAVA, 5000);
        assertEquals(4000, tank.fill(coolant, FluidAction.SIMULATE));
        assertEquals(4000, tank.fill(coolant, FluidAction.EXECUTE));
        FluidStack water = new FluidStack(Fluids.WATER, 1000);
        assertEquals(0, tank.fill(water, FluidAction.SIMULATE));
        assertEquals(0, tank.fill(water, FluidAction.EXECUTE));
        assertEquals(4000, tank.getFluidAmount());
        assertEquals(Fluids.LAVA, tank.getFluid().getFluid());
    }

    @Test
    void emptyOrSameTierFillsKeepNormalTankBehavior() {
        CoolantTank tank = tank();
        assertEquals(0, tank.fill(FluidStack.EMPTY, FluidAction.EXECUTE));
        assertEquals(3500, tank.fill(new FluidStack(Fluids.WATER, 3500), FluidAction.EXECUTE));
        assertEquals(500, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.SIMULATE));
        assertEquals(3500, tank.getFluidAmount());
        assertEquals(500, tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
    }

    @Test
    void lowerTierWaitsForActiveCoolingToExpire() {
        assertFalse(CoolantTank.shouldConsume(1, 2, 500, 500));
        assertFalse(CoolantTank.shouldConsume(1, 2, 1, 500));
        assertTrue(CoolantTank.shouldConsume(1, 2, 0, 500));
        assertTrue(CoolantTank.shouldConsume(1, 1, 500, 500));
        assertFalse(CoolantTank.shouldConsume(1, 1, 501, 500));
        assertTrue(CoolantTank.shouldConsume(2, 1, 1600, 500));
    }
}
