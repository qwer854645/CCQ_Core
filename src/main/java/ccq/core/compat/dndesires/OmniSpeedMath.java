package ccq.core.compat.dndesires;

/**
 * Pure calculations shared by the mixins and regression tests.
 * Output-side convey mirrors {@code SpeedControllerBlockEntity}: emit the scrolled
 * target RPM directly, instead of {@code input * (target / input)} which drifts.
 */
public final class OmniSpeedMath {
    private OmniSpeedMath() {}

    /** Face ratio used when Create walks the Omni via the normal modifier path. */
    public static float outputRatio(int target, float theoreticalInput) {
        if (theoreticalInput == 0 || !Float.isFinite(theoreticalInput)) return 0;
        float ratio = target / theoreticalInput;
        return Float.isFinite(ratio) ? ratio : 0;
    }

    /**
     * Speed assigned to an ordinary ±1 receiver on an Omni output face.
     * Same idea as {@code SpeedControllerBlockEntity.getDesiredOutputSpeed}: the
     * scrolled target is the output speed. {@code receiverModifier} is only ±1 and
     * applies shaft-orientation sign ({@code target / modifier}).
     * Does not require a non-zero input speed — Create's controller also returns
     * the target on the outbound path without that gate.
     */
    public static float conveyedOutput(int target, float receiverModifier) {
        if (target == 0) return 0;
        if (receiverModifier == 0 || !Float.isFinite(receiverModifier)) return 0;
        float result = target / receiverModifier;
        return Float.isFinite(result) ? result : 0;
    }
}
