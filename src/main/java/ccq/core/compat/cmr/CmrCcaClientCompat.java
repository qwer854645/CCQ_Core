package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import fr.iglee42.cmr.cooler.SnowmanCoolerVisual;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CcqCoreMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CmrCcaClientCompat {
    private static final ResourceLocation CMR_CAGE_MODEL =
            ResourceLocation.fromNamespaceAndPath("cmr", "block/snowman_cooler/block");
    private static final ResourceLocation LIQUID_CAGE_MODEL =
            ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "block/liquid_snowman_cooler/block");

    private CmrCcaClientCompat() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (!CmrCcaCompat.isEnabled()) {
            return;
        }

        event.registerBlockEntityRenderer(
                CcqCompatBlocks.LIQUID_SNOWMAN_COOLER_BE.get(),
                LiquidSnowmanCoolerRenderer::new
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!CmrCcaCompat.isEnabled()) {
            return;
        }

        event.enqueueWork(CmrCcaClientCompat::registerFlywheelVisual);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        if (!CmrCcaCompat.isEnabled()) {
            return;
        }

        var models = event.getModels();
        var cmrModel = models.get(ModelResourceLocation.standalone(CMR_CAGE_MODEL));
        if (cmrModel != null) {
            models.put(ModelResourceLocation.standalone(LIQUID_CAGE_MODEL), cmrModel);
        }
    }

    private static void registerFlywheelVisual() {
        VisualizerRegistry.setVisualizer(
                CcqCompatBlocks.LIQUID_SNOWMAN_COOLER_BE.get(),
                new BlockEntityVisualizer<LiquidSnowmanCoolerBlockEntity>() {
                    @Override
                    public BlockEntityVisual<? super LiquidSnowmanCoolerBlockEntity> createVisual(
                            VisualizationContext context,
                            LiquidSnowmanCoolerBlockEntity blockEntity,
                            float partialTick
                    ) {
                        return new SnowmanCoolerVisual(context, blockEntity, partialTick);
                    }

                    @Override
                    public boolean skipVanillaRender(LiquidSnowmanCoolerBlockEntity blockEntity) {
                        return BackendManager.isBackendOn();
                    }
                }
        );
    }
}
