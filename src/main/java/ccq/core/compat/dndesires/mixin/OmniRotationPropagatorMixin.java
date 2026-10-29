package ccq.core.compat.dndesires.mixin;

import ccq.core.compat.dndesires.OmniSpeedAccess;
import ccq.core.compat.dndesires.OmniSpeedMath;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/**
 * Omni is a split-shaft speed controller. Mirror Create's
 * {@code SpeedControllerBlockEntity} handling: outbound faces carry the scrolled
 * target RPM, and output-side feedback must not replace an established input.
 */
@Mixin(RotationPropagator.class)
public abstract class OmniRotationPropagatorMixin {
    @Shadow
    private static float getAxisModifier(KineticBlockEntity be, Direction direction) {
        throw new AssertionError();
    }

    @Inject(method = "getConnectedNeighbours", at = @At("RETURN"), cancellable = true)
    private static void ccq$keepEstablishedInput(KineticBlockEntity current,
                                                CallbackInfoReturnable<List<KineticBlockEntity>> cir) {
        List<KineticBlockEntity> neighbours = cir.getReturnValue();
        List<KineticBlockEntity> filtered = null;
        for (KineticBlockEntity neighbour : neighbours) {
            // Create's fallback can still replace the controller source when reverse
            // convey is zero — drop the edge entirely, like isolating the SC output cog.
            if (ccq$isOutputFeedbackIntoOmni(neighbour, current)) {
                if (filtered == null) filtered = new ArrayList<>(neighbours);
                filtered.remove(neighbour);
            }
        }
        if (filtered != null) cir.setReturnValue(filtered);
    }

    @Inject(method = "getConveyedSpeed", at = @At("HEAD"), cancellable = true)
    private static void ccq$speedControllerStyleConvey(KineticBlockEntity from, KineticBlockEntity to,
                                                       CallbackInfoReturnable<Float> cir) {
        if (!(from instanceof OmniSpeedAccess) && !(to instanceof OmniSpeedAccess)) return;
        Direction face = ccq$adjacentShaftFace(from, to);
        if (face == null) return;

        // Output -> Omni: same role as large-cog -> Speed Controller when the cog is
        // not the source — do not report an amplified reverse speed.
        if (ccq$isOutputFeedbackIntoOmni(to, from)) {
            cir.setReturnValue(0f);
            return;
        }

        // Omni -> output: emit scrolled target (SpeedController getDesiredOutputSpeed).
        if (from instanceof OmniSpeedAccess omni && from.hasSource()
                && !to.getBlockPos().equals(from.source)) {
            float receiver = getAxisModifier(to, face.getOpposite());
            // Retain custom split-shaft ratios. Ordinary shafts/gearboxes use +/-1.
            if (receiver == 1f || receiver == -1f) {
                cir.setReturnValue(OmniSpeedMath.conveyedOutput(omni.ccq$getTargetSpeed(), receiver));
            }
        }
    }

    /** {@code omni} already takes input elsewhere; {@code other} is on an output shaft face. */
    @Unique
    private static boolean ccq$isOutputFeedbackIntoOmni(KineticBlockEntity omni, KineticBlockEntity other) {
        return omni instanceof OmniSpeedAccess
                && omni.hasSource()
                && !other.getBlockPos().equals(omni.source)
                && ccq$adjacentShaftFace(other, omni) != null;
    }

    @Unique
    private static Direction ccq$adjacentShaftFace(KineticBlockEntity from, KineticBlockEntity to) {
        var delta = to.getBlockPos().subtract(from.getBlockPos());
        if (Math.abs(delta.getX()) + Math.abs(delta.getY()) + Math.abs(delta.getZ()) != 1) return null;
        Direction face = Direction.getNearest(delta.getX(), delta.getY(), delta.getZ());
        var a = from.getBlockState();
        var b = to.getBlockState();
        if (!(a.getBlock() instanceof IRotate ra) || !(b.getBlock() instanceof IRotate rb)) return null;
        if (!ra.hasShaftTowards(from.getLevel(), from.getBlockPos(), a, face)
                || !rb.hasShaftTowards(to.getLevel(), to.getBlockPos(), b, face.getOpposite())) return null;
        return face;
    }
}
