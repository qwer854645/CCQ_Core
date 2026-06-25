package ccq.core.compat.coe;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class OreVeinScannerItem extends Item {
    public OreVeinScannerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(createMenuProvider(hand), buf -> {
                buf.writeEnum(hand);
                CoeScannerStorage.writeOpenBuffer(buf, CoeScannerSessions.readStateForOpen(serverPlayer, hand));
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private static MenuProvider createMenuProvider(InteractionHand hand) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("item.ccq_core.ore_vein_scanner");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new OreVeinScannerMenu(containerId, inventory, hand, CoeScannerStorage.ScannerSavedState.empty());
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ccq_core.ore_vein_scanner"));
        tooltip.add(Component.translatable(
                "tooltip.ccq_core.ore_vein_scanner.range",
                CoeScannerConfig.CHUNK_RADIUS * 16
        ));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    static int cooldownTicks() {
        return CoeScannerConfig.COOLDOWN_TICKS;
    }
}
