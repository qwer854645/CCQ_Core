package ccq.core.compat.fluid.mixin;

import ccq.core.compat.fluid.ContraptionBlockEntityHelper;
import com.adonis.fluid.block.GutterOutlet.GutterOutletBlockEntity;
import com.adonis.fluid.block.GutterOutlet.GutterOutletMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GutterOutletMovementBehaviour.class)
public abstract class GutterOutletMovementBehaviourMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ccq$safeTick(MovementContext context, CallbackInfo ci) {
        ci.cancel();
        if (!context.world.isClientSide) {
            return;
        }

        BlockEntity be = ContraptionBlockEntityHelper.getBlockEntity(context.contraption, context.localPos);
        if (!(be instanceof GutterOutletBlockEntity gutter)) {
            return;
        }

        LerpedFloat fluidLevel = gutter.getFluidLevel();
        if (fluidLevel != null) {
            fluidLevel.tickChaser();
        }

        if (gutter.tankBehaviour != null) {
            SmartFluidTankBehaviour.TankSegment primaryTank = gutter.tankBehaviour.getPrimaryTank();
            if (primaryTank != null) {
                LerpedFloat tankLevel = primaryTank.getFluidLevel();
                if (tankLevel != null) {
                    tankLevel.tickChaser();
                }
            }
        }
    }
}
