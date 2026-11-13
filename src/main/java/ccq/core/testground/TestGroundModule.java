package ccq.core.testground;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;


public final class TestGroundModule {
    public static final String ID = "ccq_testground";
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(ID, "testing_ground"));
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    public static final DeferredBlock<EngineeringPortalBlock> PORTAL = BLOCKS.register("engineering_portal",
            () -> new EngineeringPortalBlock(BlockBehaviour.Properties.of().noCollission().noOcclusion()
                    .strength(-1, 3600000).noLootTable().lightLevel(state -> 11)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        EngineeringStructures.register(bus);
        NeoForge.EVENT_BUS.addListener(SpawnFacilityAnchor::started);
        NeoForge.EVENT_BUS.addListener(SpawnFacilityAnchor::stopped);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(TrainPortalCompat::register));
        NeoForge.EVENT_BUS.addListener(PortalTravel::activate);
        NeoForge.EVENT_BUS.addListener(PortalTravel::commands);
        NeoForge.EVENT_BUS.addListener(PortalTravel::tickPlayer);
        NeoForge.EVENT_BUS.addListener(PortalTravel::tickEntity);
    }
}
