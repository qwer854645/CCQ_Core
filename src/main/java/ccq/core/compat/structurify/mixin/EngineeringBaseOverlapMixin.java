package ccq.core.compat.structurify.mixin;

import ccq.core.testground.EngineeringStructures;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.faboslav.structurify.common.world.level.structure.checks.StructureOverlapCheck",remap=false)
public abstract class EngineeringBaseOverlapMixin {
    @Inject(method="canDoOverlapCheck",at=@At("HEAD"),cancellable=true,remap=false)
    private static void ccq$allowBaseSections(@Coerce Object check,@Coerce Object options,CallbackInfoReturnable<Boolean> result) {
        var start=((StructureCheckDataAccessor)check).ccq$getStructureStart();
        // One base has three adjacent starts. Structurify's padding otherwise
        // treats its own sections as competing structures and rejects the ends.
        // This dimension has only this base; other structures retain their rules.
        if(start.getStructure() instanceof EngineeringStructures.Base)result.setReturnValue(false);
    }
}
