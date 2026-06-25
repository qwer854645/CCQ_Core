package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoeMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CcqCoreMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<OreVeinScannerMenu>> ORE_VEIN_SCANNER =
            MENUS.register("ore_vein_scanner", () -> IMenuTypeExtension.create(
                    (windowId, inv, data) -> new OreVeinScannerMenu(
                            windowId,
                            inv,
                            data.readEnum(InteractionHand.class),
                            CoeScannerStorage.readOpenBuffer(data)
                    )
            ));

    private CoeMenus() {
    }
}
