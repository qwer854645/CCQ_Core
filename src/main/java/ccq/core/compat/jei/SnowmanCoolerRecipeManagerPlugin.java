package ccq.core.compat.jei;

import ccq.core.compat.cmr.SnowmanCoolerItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.advanced.IRecipeManagerPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

final class SnowmanCoolerRecipeManagerPlugin implements IRecipeManagerPlugin {
    static final RecipeType<RecipeHolder<BasinRecipe>> MIXING = RecipeType.createRecipeHolderType(
            ResourceLocation.fromNamespaceAndPath("create", "mixing")
    );

    @Override
    public <V> List<RecipeType<?>> getRecipeTypes(IFocus<V> focus) {
        if (!isSnowmanCoolerFocus(focus)) {
            return List.of();
        }
        return List.of(MIXING);
    }

    @Override
    public <T, V> List<T> getRecipes(IRecipeCategory<T> recipeCategory, IFocus<V> focus) {
        if (!isSnowmanCoolerFocus(focus)) {
            return List.of();
        }
        if (!MIXING.getUid().equals(recipeCategory.getRecipeType().getUid())) {
            return List.of();
        }

        List<T> recipes = new ArrayList<>();
        Class<?> recipeClass = recipeCategory.getRecipeType().getRecipeClass();
        for (RecipeHolder<?> holder : CmrJeiRecipes.snowmanCoolerRecipes()) {
            if (recipeClass.isInstance(holder)) {
                @SuppressWarnings("unchecked")
                T cast = (T) holder;
                recipes.add(cast);
            }
        }
        return recipes;
    }

    @Override
    public <T> List<T> getRecipes(IRecipeCategory<T> recipeCategory) {
        return List.of();
    }

    private static <V> boolean isSnowmanCoolerFocus(IFocus<V> focus) {
        return focus.checkedCast(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)
                .map(itemFocus -> SnowmanCoolerItems.isSnowmanCoolerItem(itemFocus.getTypedValue().getIngredient()))
                .orElse(false);
    }
}
