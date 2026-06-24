package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CcqCompatBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CcqCoreMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CcqCoreMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CcqCoreMod.MOD_ID);

    public static final DeferredBlock<LiquidSnowmanCoolerBlock> LIQUID_SNOWMAN_COOLER = BLOCKS.register(
            "liquid_snowman_cooler",
            () -> new LiquidSnowmanCoolerBlock(liquidCoolerProperties())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiquidSnowmanCoolerBlockEntity>> LIQUID_SNOWMAN_COOLER_BE =
            BLOCK_ENTITIES.register("liquid_snowman_cooler", () -> BlockEntityType.Builder.of(
                    LiquidSnowmanCoolerBlockEntity::new,
                    LIQUID_SNOWMAN_COOLER.get()
            ).build(null));

    private CcqCompatBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register("liquid_snowman_cooler", () -> new BlockItem(LIQUID_SNOWMAN_COOLER.get(), new Item.Properties()));
    }

    private static BlockBehaviour.Properties liquidCoolerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.ICE)
                .strength(0.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false);
    }
}
