package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoeItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CcqCoreMod.MOD_ID);

    public static final DeferredItem<OreVeinScannerItem> ORE_VEIN_SCANNER =
            ITEMS.register("ore_vein_scanner", () -> new OreVeinScannerItem(new Item.Properties().stacksTo(1)));

    private CoeItems() {
    }
}
