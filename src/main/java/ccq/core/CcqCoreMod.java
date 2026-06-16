package ccq.core;

import ccq.core.tacz.CcqGunPackBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CcqCoreMod.MOD_ID)
public class CcqCoreMod {
    public static final String MOD_ID = "ccq_core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CcqCoreMod(IEventBus modEventBus) {
        CcqBlocks.register(modEventBus);
        CcqGunPackBootstrap.register(modEventBus);
    }
}
