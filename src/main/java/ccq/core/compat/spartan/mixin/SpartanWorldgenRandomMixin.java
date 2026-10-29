package ccq.core.compat.spartan.mixin;

import ccq.core.CcqCoreMod;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Spartan Weaponry rolls spawn weapons from the {@link RandomSource} passed into
 * {@code populateDefaultEquipmentSlots}. C2ME runs that call on a worker thread, but the
 * random still belongs to the server thread, so {@code CheckedThreadLocalRandom} aborts
 * feature placement and the chunk never finishes. Swap in a thread-local random only on
 * those workers. Priority is below Spartan's default 1000 because higher mixin priorities
 * are applied first, and this method does not exist until Spartan's mixin adds it.
 */
@Mixin(value = Mob.class, priority = 500)
public abstract class SpartanWorldgenRandomMixin {
    @Unique
    private static boolean ccq$loggedC2meWorkerRandom;

    @ModifyVariable(
            method = "spartanWeaponry$attemptReplacingMainHandItemRandom(Lnet/minecraft/tags/TagKey;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/DifficultyInstance;ZFF)V",
            at = @At("HEAD"),
            argsOnly = true,
            remap = false
    )
    private RandomSource ccq$c2meWorkerRandom(RandomSource random) {
        if (!Thread.currentThread().getName().startsWith("c2me-worker")) {
            return random;
        }
        if (!ccq$loggedC2meWorkerRandom) {
            ccq$loggedC2meWorkerRandom = true;
            CcqCoreMod.LOGGER.info("Using a thread-local random for Spartan Weaponry spawn weapons on C2ME worker threads");
        }
        return RandomSource.create();
    }
}
