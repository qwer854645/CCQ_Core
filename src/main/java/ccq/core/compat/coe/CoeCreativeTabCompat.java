package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@EventBusSubscriber(modid = CcqCoreMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CoeCreativeTabCompat {
    private static final ResourceKey<CreativeModeTab> CREATE_ORE_EXCAVATION_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath("createoreexcavation", "create_ore_excavation")
    );

    private static final ResourceLocation VEIN_FINDER_ID =
            ResourceLocation.fromNamespaceAndPath("createoreexcavation", "vein_finder");

    private CoeCreativeTabCompat() {
    }

    @SubscribeEvent
    public static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!CoeCompat.isEnabled() || event.getTabKey() != CREATE_ORE_EXCAVATION_TAB) {
            return;
        }

        ItemStack scanner = new ItemStack(CoeItems.ORE_VEIN_SCANNER.get());
        BuiltInRegistries.ITEM.getOptional(VEIN_FINDER_ID).ifPresentOrElse(
                veinFinder -> event.insertAfter(
                        new ItemStack(veinFinder),
                        scanner,
                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
                ),
                () -> event.accept(scanner, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS)
        );
    }
}
