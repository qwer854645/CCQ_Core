package ccq.core.compat.farmersrespite.mixin;

import ccq.core.CcqCoreMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Farmer's Respite 3.0.0 registers {@code farmersrespite:kettle} twice:
 * {@code FRContainerTypes.MENU_TYPES} and {@code FRRecipeTypes.MENUS}.
 * NeoForge 21.1.237+ throws on duplicate registry keys. Skip the second bus
 * attach; both DeferredHolders look up the same ResourceKey, so kettle menus
 * and screens still resolve.
 */
@Mixin(targets = "com.farmersrespite.core.FarmersRespite", remap = false)
public abstract class FarmersRespiteDuplicateMenuMixin {
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/registries/DeferredRegister;register(Lnet/neoforged/bus/api/IEventBus;)V"
            )
    )
    private void ccq$skipDuplicateKettleMenu(DeferredRegister<?> register, IEventBus bus) {
        if (ccq$isDuplicateKettleMenuRegister(register)) {
            CcqCoreMod.LOGGER.info("Skipped Farmer's Respite duplicate kettle menu registration");
            return;
        }
        register.register(bus);
    }

    @Unique
    private static boolean ccq$isDuplicateKettleMenuRegister(DeferredRegister<?> register) {
        try {
            Class<?> recipeTypes = Class.forName("com.farmersrespite.core.registry.FRRecipeTypes");
            return register == recipeTypes.getField("MENUS").get(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
