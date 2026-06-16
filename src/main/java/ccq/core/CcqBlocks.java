package ccq.core;

import ccq.core.block.PonderCableBlock;
import ccq.core.block.PonderPartBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class CcqBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CcqCoreMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CcqCoreMod.MOD_ID);

    public static final DeferredBlock<PonderCableBlock> PONDER_GLASS_CABLE = registerCable("ponder_glass_cable");
    public static final DeferredBlock<PonderCableBlock> PONDER_COVERED_CABLE = registerCable("ponder_covered_cable");
    public static final DeferredBlock<PonderCableBlock> PONDER_SMART_CABLE = registerCable("ponder_smart_cable");
    public static final DeferredBlock<PonderCableBlock> PONDER_DENSE_CABLE = registerCable("ponder_dense_cable");
    public static final DeferredBlock<PonderCableBlock> PONDER_DENSE_SMART_CABLE = registerCable("ponder_dense_smart_cable");

    public static final DeferredBlock<PonderPartBlock> PONDER_TERMINAL = registerPart("ponder_terminal");
    public static final DeferredBlock<PonderPartBlock> PONDER_CRAFTING_TERMINAL = registerPart("ponder_crafting_terminal");
    public static final DeferredBlock<PonderPartBlock> PONDER_PATTERN_ENCODING_TERMINAL = registerPart("ponder_pattern_encoding_terminal");
    public static final DeferredBlock<PonderPartBlock> PONDER_STORAGE_BUS = registerPart("ponder_storage_bus");
    public static final DeferredBlock<PonderPartBlock> PONDER_CONVERSION_MONITOR = registerPart("ponder_conversion_monitor");
    public static final DeferredBlock<PonderPartBlock> PONDER_TOGGLE_BUS = registerPart("ponder_toggle_bus");
    public static final DeferredBlock<PonderPartBlock> PONDER_INVERTED_TOGGLE_BUS = registerPart("ponder_inverted_toggle_bus");
    public static final DeferredBlock<PonderPartBlock> PONDER_IMPORT_BUS = registerPart("ponder_import_bus");
    public static final DeferredBlock<PonderPartBlock> PONDER_EXPORT_BUS = registerPart("ponder_export_bus");
    public static final DeferredBlock<PonderPartBlock> PONDER_LEVEL_EMITTER = registerPart("ponder_level_emitter");
    public static final DeferredBlock<PonderPartBlock> PONDER_STORAGE_MONITOR = registerPart("ponder_storage_monitor");
    public static final DeferredBlock<PonderPartBlock> PONDER_ME_P2P_TUNNEL = registerPart("ponder_me_p2p_tunnel");
    public static final DeferredBlock<PonderPartBlock> PONDER_ANNIHILATION_PLANE = registerPart("ponder_annihilation_plane");
    public static final DeferredBlock<PonderPartBlock> PONDER_FORMATION_PLANE = registerPart("ponder_formation_plane");
    public static final DeferredBlock<PonderPartBlock> PONDER_PATTERN_ACCESS_TERMINAL = registerPart("ponder_pattern_access_terminal");
    public static final DeferredBlock<PonderPartBlock> PONDER_SOURCE_ACCEPTOR = registerPart("ponder_source_acceptor");
    public static final DeferredBlock<PonderPartBlock> PONDER_SOURCE_P2P_TUNNEL = registerPart("ponder_source_p2p_tunnel");
    public static final DeferredBlock<PonderPartBlock> PONDER_SPELL_P2P_TUNNEL = registerPart("ponder_spell_p2p_tunnel");
    public static final DeferredBlock<PonderCableBlock> PONDER_QUARTZ_FIBER = registerCable("ponder_quartz_fiber");

    private CcqBlocks() {
    }

    private static DeferredBlock<PonderCableBlock> registerCable(String name) {
        return registerBlockWithItem(name, () -> new PonderCableBlock(ponderProperties()));
    }

    private static DeferredBlock<PonderPartBlock> registerPart(String name) {
        return registerBlockWithItem(name, () -> new PonderPartBlock(ponderProperties()));
    }

    private static <T extends Block> DeferredBlock<T> registerBlockWithItem(String name, Supplier<T> block) {
        DeferredBlock<T> registeredBlock = BLOCKS.register(name, block);
        registerItem(name, registeredBlock);
        return registeredBlock;
    }

    private static <T extends Block> DeferredItem<BlockItem> registerItem(String name, DeferredBlock<T> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    private static BlockBehaviour.Properties ponderProperties() {
        return BlockBehaviour.Properties.of()
                .strength(-1.0F, 3600000.0F)
                .noOcclusion()
                .noCollission()
                .noLootTable()
                .isValidSpawn((state, level, pos, type) -> false)
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
