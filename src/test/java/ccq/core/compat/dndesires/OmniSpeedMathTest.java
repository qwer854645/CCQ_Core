package ccq.core.compat.dndesires;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OmniSpeedMathTest {
    @Test
    void fractionalAndSignedInputsReachExactRequestedOutput() {
        for (float input : new float[]{-256, -64, -32, -0.3f, 0.3f, 32, 64, 256}) {
            for (int target : new int[]{-256, -16, 0, 16, 256}) {
                assertEquals((float) target, OmniSpeedMath.conveyedOutput(target, input, 1));
                assertEquals((float) -target, OmniSpeedMath.conveyedOutput(target, input, -1));
                assertTrue(Float.isFinite(OmniSpeedMath.outputRatio(target, input)));
            }
        }
    }

    @Test
    void stoppedAndUninitialisedInputsNeverProduceInfinityOrNaN() {
        for (float input : new float[]{0, -0f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0f, OmniSpeedMath.outputRatio(16, input));
            assertEquals(0f, OmniSpeedMath.conveyedOutput(16, input, 1));
        }
        assertEquals(0f, OmniSpeedMath.conveyedOutput(16, 64, 0));
        assertEquals(0f, OmniSpeedMath.conveyedOutput(16, 64, Float.NaN));
    }

    @Test
    void savedCrashInputRetainsRequestedRatioWithoutUsingOverstressedEffectiveSpeed() {
        assertEquals(0.25f, OmniSpeedMath.outputRatio(-16, -64));
        assertEquals(-16f, OmniSpeedMath.conveyedOutput(-16, -64, 1));
    }
}
