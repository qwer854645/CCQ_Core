package ccq.core.compat.cmr.mixin;

import ccq.core.compat.cmr.CmrGoggleTooltips;
import fr.iglee42.cmr.blockspout.BlockSpoutBlockEntity;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(BlockSpoutBlockEntity.class)
public abstract class BlockSpoutBlockEntityMixin {
    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"))
    private void ccq$appendProcessing(
            List<Component> tooltip,
            boolean isPlayerSneaking,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CmrGoggleTooltips.appendBlockSpout((BlockSpoutBlockEntity) (Object) this, tooltip)) {
            cir.setReturnValue(true);
        }
    }
}
