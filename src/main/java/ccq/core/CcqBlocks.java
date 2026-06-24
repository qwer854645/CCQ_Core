package ccq.core;

import ccq.core.block.PonderCableBlock;
import ccq.core.block.PonderPartBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class CcqBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CcqCoreMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CcqCoreMod.MOD_ID);

    public static DeferredBlock<PonderCableBlock> PONDER_GLASS_CABLE;
    public static DeferredBlock<PonderCableBlock> PONDER_COVERED_CABLE;
    public static DeferredBlock<PonderCableBlock> PONDER_SMART_CABLE;
    public static DeferredBlock<PonderCableBlock> PONDER_DENSE_CABLE;
    public static DeferredBlock<PonderCableBlock> PONDER_DENSE_SMART_CABLE;

    public static DeferredBlock<PonderPartBlock> PONDER_TERMINAL;
    public static DeferredBlock<PonderPartBlock> PONDER_CRAFTING_TERMINAL;
    public static DeferredBlock<PonderPartBlock> PONDER_PATTERN_ENCODING_TERMINAL;
    public static DeferredBlock<PonderPartBlock> PONDER_STORAGE_BUS;
    public static DeferredBlock<PonderPartBlock> PONDER_CONVERSION_MONITOR;
    public static DeferredBlock<PonderPartBlock> PONDER_TOGGLE_BUS;
    public static DeferredBlock<PonderPartBlock> PONDER_INVERTED_TOGGLE_BUS;
    public static DeferredBlock<PonderPartBlock> PONDER_IMPORT_BUS;
    public static DeferredBlock<PonderPartBlock> PONDER_EXPORT_BUS;
    public static DeferredBlock<PonderPartBlock> PONDER_LEVEL_EMITTER;
    public static DeferredBlock<PonderPartBlock> PONDER_STORAGE_MONITOR;
    public static DeferredBlock<PonderPartBlock> PONDER_ME_P2P_TUNNEL;
    public static DeferredBlock<PonderPartBlock> PONDER_ANNIHILATION_PLANE;
    public static DeferredBlock<PonderPartBlock> PONDER_FORMATION_PLANE;
    public static DeferredBlock<PonderPartBlock> PONDER_PATTERN_ACCESS_TERMINAL;
    public static DeferredBlock<PonderPartBlock> PONDER_SOURCE_ACCEPTOR;
    public static DeferredBlock<PonderPartBlock> PONDER_SOURCE_P2P_TUNNEL;
    public static DeferredBlock<PonderPartBlock> PONDER_SPELL_P2P_TUNNEL;
    public static DeferredBlock<PonderCableBlock> PONDER_QUARTZ_FIBER;

    private CcqBlocks() {
    }

    public static boolean ponderBlocksEnabled() {
        return ModList.get().isLoaded("ae2");
    }

    public static void register(IEventBus modEventBus) {
        if (ponderBlocksEnabled()) {
            registerPonderBlocks();
            CcqCoreMod.LOGGER.info("AE2 detected — registering ponder decorative blocks");
        } else {
            CcqCoreMod.LOGGER.info("AE2 not loaded — skipping ponder decorative blocks");
        }
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }

    private static void registerPonderBlocks() {
        PONDER_GLASS_CABLE = registerCable("ponder_glass_cable");
        PONDER_COVERED_CABLE = registerCable("ponder_covered_cable");
        PONDER_SMART_CABLE = registerCable("ponder_smart_cable");
        PONDER_DENSE_CABLE = registerCable("ponder_dense_cable");
        PONDER_DENSE_SMART_CABLE = registerCable("ponder_dense_smart_cable");

        PONDER_TERMINAL = registerPart("ponder_terminal");
        PONDER_CRAFTING_TERMINAL = registerPart("ponder_crafting_terminal");
        PONDER_PATTERN_ENCODING_TERMINAL = registerPart("ponder_pattern_encoding_terminal");
        PONDER_STORAGE_BUS = registerPart("ponder_storage_bus");
        PONDER_CONVERSION_MONITOR = registerPart("ponder_conversion_monitor");
        PONDER_TOGGLE_BUS = registerPart("ponder_toggle_bus");
        PONDER_INVERTED_TOGGLE_BUS = registerPart("ponder_inverted_toggle_bus");
        PONDER_IMPORT_BUS = registerPart("ponder_import_bus");
        PONDER_EXPORT_BUS = registerPart("ponder_export_bus");
        PONDER_LEVEL_EMITTER = registerPart("ponder_level_emitter");
        PONDER_STORAGE_MONITOR = registerPart("ponder_storage_monitor");
        PONDER_ME_P2P_TUNNEL = registerPart("ponder_me_p2p_tunnel");
        PONDER_ANNIHILATION_PLANE = registerPart("ponder_annihilation_plane");
        PONDER_FORMATION_PLANE = registerPart("ponder_formation_plane");
        PONDER_PATTERN_ACCESS_TERMINAL = registerPart("ponder_pattern_access_terminal");
        PONDER_SOURCE_ACCEPTOR = registerPart("ponder_source_acceptor");
        PONDER_SOURCE_P2P_TUNNEL = registerPart("ponder_source_p2p_tunnel");
        PONDER_SPELL_P2P_TUNNEL = registerPart("ponder_spell_p2p_tunnel");
        PONDER_QUARTZ_FIBER = registerCable("ponder_quartz_fiber");
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
}
