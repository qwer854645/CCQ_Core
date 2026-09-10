package ccq.core.compat.fluid.mixin;

import ccq.core.compat.fluid.ContraptionBlockEntityHelper;
import com.adonis.fluid.block.CopperSink.CopperSinkBlockEntity;
import com.adonis.fluid.block.CopperSink.CopperSinkMovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperSinkMovementBehaviour.class)
public abstract class CopperSinkMovementBehaviourMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void ccq$safeTick(MovementContext context, CallbackInfo ci) {
        ci.cancel();
        if (!context.world.isClientSide) {
            return;
        }

        BlockEntity be = ContraptionBlockEntityHelper.getBlockEntity(context.contraption, context.localPos);
        if (be instanceof CopperSinkBlockEntity sink) {
            LerpedFloat fluidLevel = sink.getFluidLevel();
            if (fluidLevel != null) {
                fluidLevel.tickChaser();
            }
        }
    }
}
