package ccq.core.compat.jei;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;

final class CmrJeiRecipes {
    private CmrJeiRecipes() {
    }

    @SuppressWarnings("unchecked")
    static List<RecipeHolder<BasinRecipe>> snowmanCoolerRecipes() {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return List.of();
        }

        List<RecipeHolder<BasinRecipe>> recipes = new ArrayList<>();
        for (RecipeHolder<?> holder : connection.getRecipeManager().getAllRecipesFor(AllRecipeTypes.MIXING.getType())) {
            if (!(holder.value() instanceof BasinRecipe basinRecipe)) {
                continue;
            }
            if (requiresSnowmanCooler(basinRecipe)) {
                recipes.add((RecipeHolder<BasinRecipe>) holder);
            }
        }
        return recipes;
    }

    private static boolean requiresSnowmanCooler(BasinRecipe recipe) {
        HeatCondition heat = recipe.getRequiredHeat();
        return heat != HeatCondition.NONE && heat != HeatCondition.HEATED && heat != HeatCondition.SUPERHEATED;
    }
}
