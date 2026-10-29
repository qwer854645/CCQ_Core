package ccq.core.compat.coe;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CoeServerEvents {
    private CoeServerEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!CoeCompat.isEnabled()) {
            return;
        }
        CoeScannerSessions.tickAll(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!CoeCompat.isEnabled()) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            CoeScannerSessions.handleLogout(player);
        }
    }
}
