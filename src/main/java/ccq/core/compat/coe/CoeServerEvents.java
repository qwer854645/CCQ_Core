package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CcqCoreMod.MOD_ID)
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
}
