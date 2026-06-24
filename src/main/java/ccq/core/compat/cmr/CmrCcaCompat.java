package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CmrCcaCompat {
    public static final ResourceLocation SNOWMAN_COOLER_BLOCK = ResourceLocation.fromNamespaceAndPath("cmr", "snowman_cooler");
    public static final ResourceLocation STRAW_ITEM = ResourceLocation.fromNamespaceAndPath("createaddition", "straw");

    private static boolean enabled;

    private CmrCcaCompat() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void register(IEventBus modEventBus) {
        enabled = ModList.get().isLoaded("cmr") && ModList.get().isLoaded("createaddition");
        if (!enabled) {
            return;
        }

        CcqCompatBlocks.register(modEventBus);
        modEventBus.addListener(CmrCcaCompat::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(CmrCcaCompat::onStrawUse);
        CcqCoreMod.LOGGER.info("CMR + CCA loaded — liquid snowman cooler straw compatibility enabled");
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                CcqCompatBlocks.LIQUID_SNOWMAN_COOLER_BE.get(),
                (be, side) -> be.getFluidHandler()
        );
    }

    private static void onStrawUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        Level level = event.getLevel();
        BlockState state = level.getBlockState(pos);
        if (!BuiltInRegistries.BLOCK.getOptional(SNOWMAN_COOLER_BLOCK).map(state::is).orElse(false)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getOptional(STRAW_ITEM).map(stack::is).orElse(false)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SnowmanCoolerBlockEntity cooler)) {
            return;
        }

        BlockState newState = CcqCompatBlocks.LIQUID_SNOWMAN_COOLER.get().defaultBlockState()
                .setValue(SnowmanCoolerBlock.FACING, state.getValue(SnowmanCoolerBlock.FACING))
                .setValue(SnowmanCoolerBlock.HEAT_LEVEL, state.getValue(SnowmanCoolerBlock.HEAT_LEVEL));
        level.setBlock(pos, newState, Block.UPDATE_ALL);

        BlockEntity converted = level.getBlockEntity(pos);
        if (converted instanceof LiquidSnowmanCoolerBlockEntity liquidCooler) {
            liquidCooler.copyFrom(cooler);
        }

        if (event.getHand() == InteractionHand.MAIN_HAND) {
            player.swing(InteractionHand.MAIN_HAND);
        } else {
            player.swing(InteractionHand.OFF_HAND);
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }

        level.playSound(
                null,
                pos,
                SoundEvents.BAMBOO_PLACE,
                SoundSource.BLOCKS,
                1.0F,
                0.85F + level.random.nextFloat() * 0.2F
        );

        event.setCanceled(true);
    }
}
