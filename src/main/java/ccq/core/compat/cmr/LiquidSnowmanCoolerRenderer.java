package ccq.core.compat.cmr;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import fr.iglee42.cmr.cooler.SnowmanCoolerRenderer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class LiquidSnowmanCoolerRenderer extends SnowmanCoolerRenderer {
    public LiquidSnowmanCoolerRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(
            SnowmanCoolerBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        SnowmanCoolerBlock.HeatLevel heatLevel = blockEntity.getHeatLevelFromBlock();
        float headAnimation = blockEntity.headAnimation.getValue(partialTicks) * 0.175F;
        float headAngle = AngleHelper.rad(SnowmanCoolerAccess.getHeadAngle(blockEntity, partialTicks));
        boolean activeFlame = heatLevel.isAtLeast(SnowmanCoolerBlock.HeatLevel.FADING);
        PartialModel hat = blockEntity.hat
                ? AllPartialModels.TRAIN_HAT
                : blockEntity.stockKeeper ? AllPartialModels.LOGISTICS_HAT : null;

        SnowmanCoolerRenderer.renderShared(
                poseStack,
                null,
                bufferSource,
                blockEntity.getLevel(),
                SnowmanCoolerRenderSupport.renderBlockState(blockEntity.getBlockState()),
                heatLevel,
                headAnimation,
                headAngle,
                activeFlame,
                blockEntity.goggles,
                hat,
                blockEntity.hashCode()
        );
    }
}
