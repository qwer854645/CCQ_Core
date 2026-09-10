package ccq.core.compat.fluid.mixin;

import ccq.core.compat.fluid.ContraptionBlockEntityHelper;
import com.adonis.fluid.block.GutterOutlet.GutterOutletBlockEntity;
import com.adonis.fluid.block.GutterOutlet.GutterOutletMountedStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GutterOutletMountedStorage.class)
public abstract class GutterOutletMountedStorageMixin {
    @Inject(method = "afterSync", at = @At("HEAD"), cancellable = true)
    private void ccq$safeAfterSync(Contraption contraption, BlockPos localPos, CallbackInfo ci) {
        ci.cancel();

        BlockEntity be = ContraptionBlockEntityHelper.getBlockEntity(contraption, localPos);
        if (!(be instanceof GutterOutletBlockEntity gutter)) {
            return;
        }

        GutterOutletMountedStorage self = (GutterOutletMountedStorage) (Object) this;
        FluidTank inv = gutter.getTankInventory();
        if (inv != null) {
            inv.setFluid(self.getFluid().copy());
        }

        float fillLevel = (float) self.getFluid().getAmount() / self.getCapacity();
        LerpedFloat fluidLevel = gutter.getFluidLevel();
        if (fluidLevel != null) {
            fluidLevel.chase(fillLevel, 0.5, LerpedFloat.Chaser.EXP);
        }

        if (gutter.tankBehaviour != null) {
            SmartFluidTankBehaviour.TankSegment primaryTank = gutter.tankBehaviour.getPrimaryTank();
            if (primaryTank != null) {
                primaryTank.getFluidLevel().chase(fillLevel, 0.5, LerpedFloat.Chaser.EXP);
            }
        }
    }
}
