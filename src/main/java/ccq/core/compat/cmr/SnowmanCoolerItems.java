package ccq.core.compat.cmr;

import ccq.core.CcqCoreMod;
import fr.iglee42.cmr.cooler.SnowmanCoolerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class SnowmanCoolerItems {
    public static final ResourceLocation SNOWMAN_COOLER_ITEM =
            ResourceLocation.fromNamespaceAndPath("cmr", "snowman_cooler");
    public static final ResourceLocation LIQUID_SNOWMAN_COOLER_ITEM =
            ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "liquid_snowman_cooler");
    public static final ResourceLocation CMR_COOLER_BE =
            ResourceLocation.fromNamespaceAndPath("cmr", "blaze_heater");

    public static final TagKey<Item> REGULAR_FUEL = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("cmr", "snowman_cooler_fuel/regular")
    );
    public static final TagKey<Item> SPECIAL_FUEL = TagKey.create(
            net.minecraft.core.registries.Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("cmr", "snowman_cooler_fuel/special")
    );

    private SnowmanCoolerItems() {
    }

    public static boolean isSnowmanCoolerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return BuiltInRegistries.ITEM.getOptional(SNOWMAN_COOLER_ITEM).map(stack::is).orElse(false)
                || BuiltInRegistries.ITEM.getOptional(LIQUID_SNOWMAN_COOLER_ITEM).map(stack::is).orElse(false);
    }

    public static ItemStack fuelDisplayStack(SnowmanCoolerBlockEntity cooler) {
        if (cooler.getRemainingBurnTime() <= 0) {
            return ItemStack.EMPTY;
        }

        return switch (cooler.getActiveFuel()) {
            case SPECIAL -> firstTaggedItem(SPECIAL_FUEL, new ItemStack(Items.CAKE));
            case NORMAL -> firstTaggedItem(REGULAR_FUEL, new ItemStack(Items.SNOW_BLOCK));
            case NONE -> ItemStack.EMPTY;
        };
    }

    private static ItemStack firstTaggedItem(TagKey<Item> tag, ItemStack fallback) {
        return BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(tagHolder -> tagHolder.stream().findFirst())
                .map(item -> new ItemStack(item.value()))
                .orElse(fallback);
    }
}
