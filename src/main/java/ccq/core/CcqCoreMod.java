package ccq.core;

import ccq.core.compat.cmr.CmrCcaCompat;
import ccq.core.compat.cmr.CmrFixes;
import ccq.core.compat.cmr.CmrSnowmanCapabilities;
import ccq.core.resourcepack.CcaResourcePackBootstrap;
import ccq.core.tacz.CcqGunPackBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CcqCoreMod.MOD_ID)
public class CcqCoreMod {
    public static final String MOD_ID = "ccq_core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CcqCoreMod(IEventBus modEventBus, ModContainer modContainer) {
        CcqBlocks.register(modEventBus);
        CcqGunPackBootstrap.register(modEventBus);
        CcaResourcePackBootstrap.register(modEventBus);
        CmrFixes.register(modEventBus);
        CmrCcaCompat.register(modEventBus);
        CmrSnowmanCapabilities.register(modEventBus);
    }
}
