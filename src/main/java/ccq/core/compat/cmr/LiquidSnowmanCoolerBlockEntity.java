package ccq.core.compat.cmr;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

public class LiquidSnowmanCoolerBlockEntity extends SnowmanCoolerBlockEntity implements IHaveGoggleInformation {
    private static final int TANK_CAPACITY = 4000;
    private static final int INSERTION_THRESHOLD = 500;

    static final TagKey<net.minecraft.world.level.material.Fluid> REGULAR_COOLANT = TagKey.create(
            Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath("ccq_core", "liquid_cooling_regular")
    );
    static final TagKey<net.minecraft.world.level.material.Fluid> SPECIAL_COOLANT = TagKey.create(
            Registries.FLUID,
            ResourceLocation.fromNamespaceAndPath("ccq_core", "liquid_cooling_special")
    );

    private final FluidTank tank = new FluidTank(TANK_CAPACITY, this::isCoolantFluid);
    private final LiquidSnowmanCoolerFluidHandler fluidHandler = new LiquidSnowmanCoolerFluidHandler();

    public LiquidSnowmanCoolerBlockEntity(BlockPos pos, BlockState state) {
        super(CcqCompatBlocks.LIQUID_SNOWMAN_COOLER_BE.get(), pos, state);
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = CmrGoggleTooltips.appendSnowmanCooler(this, tooltip, isPlayerSneaking);
        return containedFluidTooltip(tooltip, isPlayerSneaking, getFluidHandler()) || added;
    }

    void copyFrom(SnowmanCoolerBlockEntity source) {
        SnowmanCoolerAccess.copyState(source, this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || isCreative) {
            return;
        }
        if (remainingBurnTime > INSERTION_THRESHOLD || tank.isEmpty()) {
            return;
        }
        FluidStack drained = tank.drain(100, IFluidHandler.FluidAction.EXECUTE);
        if (!drained.isEmpty()) {
            applyFluidCooling(drained);
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag tankTag = new CompoundTag();
        tank.writeToNBT(registries, tankTag);
        tag.put("FluidTank", tankTag);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("FluidTank")) {
            tank.readFromNBT(registries, tag.getCompound("FluidTank"));
        }
    }

    private boolean isCoolantFluid(FluidStack stack) {
        return tierFor(stack) != null;
    }

    private void applyFluidCooling(FluidStack stack) {
        CoolingTier tier = tierFor(stack);
        if (tier == null) {
            return;
        }

        int burnTime = Math.max(1, tier.burnPerBucket() * stack.getAmount() / 1000);
        FuelType targetFuel = tier == CoolingTier.SPECIAL ? FuelType.SPECIAL : FuelType.NORMAL;

        if (targetFuel.ordinal() < activeFuel.ordinal()) {
            return;
        }

        if (activeFuel == targetFuel) {
            if (remainingBurnTime <= INSERTION_THRESHOLD) {
                burnTime += remainingBurnTime;
            } else if (remainingBurnTime < MAX_HEAT_CAPACITY) {
                burnTime = Math.min(remainingBurnTime + burnTime, MAX_HEAT_CAPACITY);
            } else {
                return;
            }
        }

        activeFuel = targetFuel;
        remainingBurnTime = burnTime;
        updateBlockState();
        setChanged();
    }

    private static CoolingTier tierFor(FluidStack stack) {
        Holder<net.minecraft.world.level.material.Fluid> holder = BuiltInRegistries.FLUID.wrapAsHolder(stack.getFluid());
        if (holder.is(SPECIAL_COOLANT)) {
            return CoolingTier.SPECIAL;
        }
        if (holder.is(REGULAR_COOLANT) || stack.getFluid() == Fluids.WATER) {
            return CoolingTier.REGULAR;
        }
        return null;
    }

    private enum CoolingTier {
        REGULAR(1600),
        SPECIAL(3200);

        private final int burnPerBucket;

        CoolingTier(int burnPerBucket) {
            this.burnPerBucket = burnPerBucket;
        }

        int burnPerBucket() {
            return burnPerBucket;
        }
    }

    private final class LiquidSnowmanCoolerFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return isCoolantFluid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int filled = tank.fill(resource, action);
            if (filled > 0) {
                setChanged();
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
