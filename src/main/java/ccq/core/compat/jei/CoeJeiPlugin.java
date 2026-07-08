package ccq.core.compat.jei;

import ccq.core.CcqCoreMod;
import ccq.core.compat.coe.CoeCompat;
import ccq.core.compat.coe.CoeItems;
import ccq.core.compat.coe.CoeScannerConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public final class CoeJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "coe_ore_vein_scanner");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!CoeCompat.isEnabled()) {
            return;
        }

        ItemStack scanner = new ItemStack(CoeItems.ORE_VEIN_SCANNER.get());
        registration.addIngredientInfo(
                scanner,
                VanillaTypes.ITEM_STACK,
                Component.translatable("jei.ccq_core.ore_vein_scanner"),
                Component.translatable(
                        "jei.ccq_core.ore_vein_scanner.range",
                        CoeScannerConfig.CHUNK_RADIUS * 16
                )
        );
    }
}
