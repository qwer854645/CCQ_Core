package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class CmrFixes {
    public static final ResourceLocation EMPTY_SNOWMAN_COOLER_BLOCK =
            ResourceLocation.fromNamespaceAndPath("cmr", "empty_snowman_cooler");
    public static final ResourceLocation WRENCH_ITEM =
            ResourceLocation.fromNamespaceAndPath("create", "wrench");

    private static boolean enabled;

    private CmrFixes() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void register(IEventBus modEventBus) {
        enabled = ModList.get().isLoaded("cmr") && ModList.get().isLoaded("create");
        if (!enabled) {
            return;
        }

        NeoForge.EVENT_BUS.addListener(CmrFixes::onEmptyCoolerWrench);
        CcqCoreMod.LOGGER.info("CMR fixes enabled — empty snowman cooler loot table, wrench pickup, and goggle tooltips");
    }

    private static void onEmptyCoolerWrench(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !event.getEntity().isShiftKeyDown()) {
            return;
        }

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!BuiltInRegistries.BLOCK.getOptional(EMPTY_SNOWMAN_COOLER_BLOCK).map(state::is).orElse(false)) {
            return;
        }

        ItemStack stack = event.getEntity().getItemInHand(event.getHand());
        if (stack.isEmpty() || !BuiltInRegistries.ITEM.getOptional(WRENCH_ITEM).map(stack::is).orElse(false)) {
            return;
        }

        UseOnContext context = new UseOnContext(
                event.getLevel(),
                event.getEntity(),
                event.getHand(),
                stack,
                event.getHitVec()
        );
        InteractionResult result = WrenchBreak.INSTANCE.onSneakWrenched(state, context);
        if (result.consumesAction()) {
            event.setCancellationResult(result);
            event.setCanceled(true);
        }
    }

    private enum WrenchBreak implements IWrenchable {
        INSTANCE
    }
}
