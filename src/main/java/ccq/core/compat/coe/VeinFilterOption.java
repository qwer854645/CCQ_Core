package ccq.core.compat.coe;

import com.tom.createores.CreateOreExcavation;
import com.tom.createores.recipe.VeinRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public record VeinFilterOption(@Nullable ResourceLocation recipeId, Component name, ItemStack icon) {
    public static VeinFilterOption all() {
        return new VeinFilterOption(
                null,
                Component.translatable("gui.ccq_core.ore_vein_scanner.filter.all"),
                new ItemStack(Items.COMPASS)
        );
    }

    public static VeinFilterOption of(RecipeHolder<VeinRecipe> recipe) {
        VeinRecipe vein = recipe.value();
        return new VeinFilterOption(recipe.id(), vein.getName(), vein.getIcon().copy());
    }

    public static List<VeinFilterOption> buildClientOptions() {
        List<VeinFilterOption> options = new ArrayList<>();
        options.add(all());

        var minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return options;
        }

        minecraft.level.getRecipeManager()
                .getAllRecipesFor(CreateOreExcavation.VEIN_RECIPES.getRecipeType())
                .stream()
                .sorted(Comparator.comparing(holder -> holder.value().getName().getString()))
                .map(VeinFilterOption::of)
                .forEach(options::add);
        return options;
    }
}
