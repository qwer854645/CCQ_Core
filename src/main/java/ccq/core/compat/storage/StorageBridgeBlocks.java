package ccq.core.compat.storage;

import ccq.core.CcqCoreMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class StorageBridgeBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CcqCoreMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CcqCoreMod.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CcqCoreMod.MOD_ID);

    public static final DeferredBlock<StorageBridgeBlock> STORAGE_BRIDGE = BLOCKS.register(
            "storage_bridge",
            () -> new StorageBridgeBlock(bridgeProperties())
    );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageBridgeBlockEntity>> STORAGE_BRIDGE_BE =
            BLOCK_ENTITIES.register("storage_bridge", () -> BlockEntityType.Builder.of(
                    StorageBridgeBlockEntity::new,
                    STORAGE_BRIDGE.get()
            ).build(null));

    private StorageBridgeBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ITEMS.register("storage_bridge", () -> new BlockItem(STORAGE_BRIDGE.get(), new Item.Properties()));
    }

    private static BlockBehaviour.Properties bridgeProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(1.25F, 6.0F)
                .sound(SoundType.WOOD)
                .requiresCorrectToolForDrops();
    }
}
