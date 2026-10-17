package ccq.core.compat.dndesires;

/** Pure calculations shared by the mixins and regression tests. */
public final class OmniSpeedMath {
    private OmniSpeedMath() {}

    public static float outputRatio(int target, float theoreticalInput) {
        if (theoreticalInput == 0 || !Float.isFinite(theoreticalInput)) return 0;
        float ratio = target / theoreticalInput;
        return Float.isFinite(ratio) ? ratio : 0;
    }

    public static float conveyedOutput(int target, float theoreticalInput, float receiverModifier) {
        if (theoreticalInput == 0 || !Float.isFinite(theoreticalInput)
                || receiverModifier == 0 || !Float.isFinite(receiverModifier)) return 0;
        float result = target / receiverModifier;
        return Float.isFinite(result) && result != 0 ? result : 0;
    }
}
