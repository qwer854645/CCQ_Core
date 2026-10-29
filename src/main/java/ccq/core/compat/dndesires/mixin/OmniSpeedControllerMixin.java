package ccq.core.compat.dndesires.mixin;

import ccq.core.compat.dndesires.OmniSpeedAccess;
import ccq.core.compat.dndesires.OmniSpeedMath;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.lopyluna.dndesires.content.blocks.kinetics.omni_speed_controller.OmniSpeedControllerBE")
public abstract class OmniSpeedControllerMixin extends SplitShaftBlockEntity implements OmniSpeedAccess {
    @Shadow public ScrollValueBehaviour targetSpeed;

    protected OmniSpeedControllerMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int ccq$getTargetSpeed() {
        return targetSpeed == null ? 0 : targetSpeed.getValue();
    }

    @Inject(method = "getRotationSpeedModifier", at = @At("HEAD"), cancellable = true)
    private void ccq$stableOutputRatio(Direction face, CallbackInfoReturnable<Float> cir) {
        if (!hasSource() || face == getSourceFacing()) {
            cir.setReturnValue(1f);
        } else {
            // Use theoretical input: effective getSpeed() is zero under overstress.
            // Outbound RPM itself is applied in OmniRotationPropagatorMixin (SC-style).
            cir.setReturnValue(OmniSpeedMath.outputRatio(ccq$getTargetSpeed(), getTheoreticalSpeed()));
        }
    }
}
