package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = CcqCoreMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CoeClientCompat {
    private CoeClientCompat() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        if (!CoeCompat.isEnabled()) {
            return;
        }
        event.register(CoeMenus.ORE_VEIN_SCANNER.get(), OreVeinScannerScreen::new);
    }
}
