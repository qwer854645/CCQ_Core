package ccq.core.compat.fluid.mixin;

import ccq.core.compat.fluid.ContraptionBlockEntityHelper;
import com.adonis.fluid.block.CopperSink.CopperSinkBlockEntity;
import com.adonis.fluid.block.CopperSink.CopperSinkMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(CopperSinkMountedStorage.class)
public abstract class CopperSinkMountedStorageMixin {
    @Unique
    private static final Field CCQ$WRAPPED = ccq$findWrappedField();

    @Inject(method = "afterSync", at = @At("HEAD"), cancellable = true)
    private void ccq$safeAfterSync(Contraption contraption, BlockPos localPos, CallbackInfo ci) {
        ci.cancel();

        BlockEntity be = ContraptionBlockEntityHelper.getBlockEntity(contraption, localPos);
        if (!(be instanceof CopperSinkBlockEntity sink)) {
            return;
        }

        IFluidHandler handler = ccq$wrappedHandler();
        if (handler == null) {
            return;
        }

        FluidStack fluid = handler instanceof FluidTank tank
                ? tank.getFluid().copy()
                : handler.getFluidInTank(0).copy();
        sink.getTank().setFluid(fluid);

        int amount = handler instanceof FluidTank tank
                ? tank.getFluidAmount()
                : handler.getFluidInTank(0).getAmount();
        int capacity = Math.max(1, handler instanceof FluidTank tank
                ? tank.getCapacity()
                : handler.getTankCapacity(0));
        float fillLevel = (float) amount / capacity;
        LerpedFloat fluidLevel = sink.getFluidLevel();
        if (fluidLevel != null) {
            fluidLevel.chase(fillLevel, 0.5, LerpedFloat.Chaser.EXP);
        }
    }

    @Unique
    private IFluidHandler ccq$wrappedHandler() {
        if (CCQ$WRAPPED == null) {
            return null;
        }
        try {
            Object value = CCQ$WRAPPED.get(this);
            return value instanceof IFluidHandler handler ? handler : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @Unique
    private static Field ccq$findWrappedField() {
        for (Class<?> type = CopperSinkMountedStorage.class; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField("wrapped");
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
