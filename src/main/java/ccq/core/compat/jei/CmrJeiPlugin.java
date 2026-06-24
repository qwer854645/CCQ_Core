package ccq.core.compat.jei;

import ccq.core.CcqCoreMod;
import ccq.core.compat.cmr.CmrFixes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IAdvancedRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class CmrJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "cmr_snowman_cooler");
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        if (!CmrFixes.isEnabled()) {
            return;
        }
        registration.addRecipeManagerPlugin(new SnowmanCoolerRecipeManagerPlugin());
    }
}
