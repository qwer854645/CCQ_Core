package ccq.core.compat.jade;

import ccq.core.compat.cmr.CmrFixes;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlock;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class CmrJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        if (!CmrFixes.isEnabled()) {
            return;
        }
        registration.registerBlockDataProvider(SnowmanCoolerJadeProvider.INSTANCE, SnowmanCoolerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        if (!CmrFixes.isEnabled()) {
            return;
        }
        registration.registerBlockComponent(SnowmanCoolerJadeProvider.INSTANCE, SnowmanCoolerBlock.class);
    }
}
