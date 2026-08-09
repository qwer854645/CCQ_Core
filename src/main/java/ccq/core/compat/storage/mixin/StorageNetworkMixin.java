package ccq.core.compat.storage.mixin;

import ccq.core.compat.storage.BridgedFxntNetworkHandler;
import ccq.core.compat.storage.StorageBridgeBlockEntity;
import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(StorageNetwork.class)
public abstract class StorageNetworkMixin {
    @Shadow
    @Final
    private StorageControllerEntity controller;

    @Unique
    private IItemHandlerModifiable ccq$bridgedHandler;

    @Inject(method = "getBoxes", at = @At("TAIL"), remap = false)
    private void ccq$claimStorageBridges(Level level, Set<BlockPos> components, CallbackInfo ci) {
        if (level == null || components == null || controller == null) {
            return;
        }
        for (BlockPos pos : components) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageBridgeBlockEntity bridge) {
                bridge.setFxntController(controller);
                bridge.tryAutoLinkFunctionalStorage();
            }
        }
    }

    @Inject(method = "getItemHandler", at = @At("RETURN"), cancellable = true, remap = false)
    private void ccq$exposeFunctionalStorage(CallbackInfoReturnable<IItemHandlerModifiable> cir) {
        IItemHandlerModifiable nativeHandler = cir.getReturnValue();
        if (nativeHandler == null) {
            return;
        }
        if (ccq$bridgedHandler == null) {
            ccq$bridgedHandler = new BridgedFxntNetworkHandler(nativeHandler, (StorageNetwork) (Object) this);
        }
        cir.setReturnValue(ccq$bridgedHandler);
    }
}
