package ccq.core.compat.coe;

import ccq.core.compat.coe.network.OreVeinScanResultsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CoeClientNetwork {
    private CoeClientNetwork() {
    }

    public static void handleScanResults(OreVeinScanResultsPayload payload) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (player.containerMenu instanceof OreVeinScannerMenu menu
                && payload.containerId() >= 0
                && menu.containerId == payload.containerId()) {
            menu.applyState(
                    payload.filterRecipeId().orElse(null),
                    payload.entries(),
                    true,
                    false
            );
        }

        if (payload.notifyPlayer()) {
            showCompleteToast(payload.entries().size());
        }
    }

    private static void showCompleteToast(int count) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.displayClientMessage(
                Component.translatable("gui.ccq_core.ore_vein_scanner.complete_message", count),
                false
        );
        minecraft.gui.setOverlayMessage(
                Component.translatable("gui.ccq_core.ore_vein_scanner.complete_title"),
                true
        );
    }
}
