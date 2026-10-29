package ccq.core.compat.dndesires;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OmniSpeedMathTest {
    @Test
    void outboundConveyMatchesScrolledTargetLikeVanillaSpeedController() {
        for (int target : new int[]{-256, -16, 0, 16, 256}) {
            assertEquals((float) target, OmniSpeedMath.conveyedOutput(target, 1));
            assertEquals((float) -target, OmniSpeedMath.conveyedOutput(target, -1));
        }
    }

    @Test
    void outboundConveyDoesNotDependOnInputSpeed() {
        // Previously gated on theoretical input == 0, which could suppress the
        // scrolled target even though SpeedController still emits it outbound.
        assertEquals(16f, OmniSpeedMath.conveyedOutput(16, 1));
        assertEquals(-16f, OmniSpeedMath.conveyedOutput(16, -1));
        assertEquals(0f, OmniSpeedMath.conveyedOutput(0, 1));
        assertEquals(0f, OmniSpeedMath.conveyedOutput(16, 0));
        assertEquals(0f, OmniSpeedMath.conveyedOutput(16, Float.NaN));
    }

    @Test
    void ratioUsesTheoreticalInputAndStaysFinite() {
        for (float input : new float[]{-256, -64, -0.3f, 0.3f, 32, 64, 256}) {
            for (int target : new int[]{-256, -16, 0, 16, 256}) {
                assertTrue(Float.isFinite(OmniSpeedMath.outputRatio(target, input)));
            }
        }
        for (float input : new float[]{0, -0f, Float.NaN, Float.POSITIVE_INFINITY}) {
            assertEquals(0f, OmniSpeedMath.outputRatio(16, input));
        }
        assertEquals(0.25f, OmniSpeedMath.outputRatio(-16, -64));
    }
}
