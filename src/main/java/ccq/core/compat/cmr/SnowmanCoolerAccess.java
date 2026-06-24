package ccq.core.compat.cmr;

import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;

import java.lang.reflect.Field;

final class SnowmanCoolerAccess {
    private static Field activeFuelField;
    private static Field remainingBurnTimeField;
    private static Field gogglesField;
    private static Field hatField;
    private static Field isCreativeField;
    private static Field headAngleField;
    private static boolean copyFieldsInitialized;
    private static boolean headAngleInitialized;

    private SnowmanCoolerAccess() {
    }

    static float getHeadAngle(SnowmanCoolerBlockEntity blockEntity, float partialTick) {
        ensureHeadAngleInitialized();
        try {
            LerpedFloat headAngle = (LerpedFloat) headAngleField.get(blockEntity);
            return headAngle.getValue(partialTick);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to read snowman cooler head angle", exception);
        }
    }

    static void copyState(SnowmanCoolerBlockEntity from, SnowmanCoolerBlockEntity to) {
        ensureCopyFieldsInitialized();
        try {
            activeFuelField.set(to, activeFuelField.get(from));
            remainingBurnTimeField.set(to, remainingBurnTimeField.get(from));
            gogglesField.set(to, gogglesField.get(from));
            hatField.set(to, hatField.get(from));
            isCreativeField.set(to, isCreativeField.get(from));
            to.updateBlockState();
            to.setChanged();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to copy snowman cooler state", exception);
        }
    }

    private static void ensureCopyFieldsInitialized() {
        if (copyFieldsInitialized) {
            return;
        }
        copyFieldsInitialized = true;
        try {
            activeFuelField = SnowmanCoolerBlockEntity.class.getDeclaredField("activeFuel");
            remainingBurnTimeField = SnowmanCoolerBlockEntity.class.getDeclaredField("remainingBurnTime");
            gogglesField = SnowmanCoolerBlockEntity.class.getDeclaredField("goggles");
            hatField = SnowmanCoolerBlockEntity.class.getDeclaredField("hat");
            isCreativeField = SnowmanCoolerBlockEntity.class.getDeclaredField("isCreative");
            activeFuelField.setAccessible(true);
            remainingBurnTimeField.setAccessible(true);
            gogglesField.setAccessible(true);
            hatField.setAccessible(true);
            isCreativeField.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize snowman cooler copy reflection", exception);
        }
    }

    private static void ensureHeadAngleInitialized() {
        if (headAngleInitialized) {
            return;
        }
        headAngleInitialized = true;
        try {
            headAngleField = SnowmanCoolerBlockEntity.class.getDeclaredField("headAngle");
            headAngleField.setAccessible(true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to initialize snowman cooler head angle reflection", exception);
        }
    }
}
