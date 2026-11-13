package ccq.core.compat.ballistics.mixin;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ballistics 1.3.0a restores its cached sensitivity every frame with a GUI open. */
@Mixin(targets = "net.felis.cbc_ballistics.event.ModClientEvents", remap = false)
public class RangefinderSensitivityMixin {
    @Shadow private boolean isScoped;
    @Shadow private double sensitivity;

    @Inject(method = "onComputerFovModifierEvent", at = @At("HEAD"), cancellable = true)
    private void ccq$preserveScreenSensitivity(ComputeFovModifierEvent event, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) return;
        // Restore once on leaving the rangefinder, then accept GUI edits as the new baseline.
        if (isScoped) client.options.sensitivity().set(sensitivity);
        sensitivity = client.options.sensitivity().get();
        isScoped = false;
        ci.cancel();
    }
}
