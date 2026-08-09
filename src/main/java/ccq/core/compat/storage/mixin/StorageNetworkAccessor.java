package ccq.core.compat.storage.mixin;

import net.fxnt.fxntstorage.controller.StorageControllerEntity;
import net.fxnt.fxntstorage.storage_network.StorageNetwork;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(StorageNetwork.class)
public interface StorageNetworkAccessor {
    @Accessor(value = "itemHandler", remap = false)
    IItemHandlerModifiable ccq$getNativeItemHandler();

    @Accessor(value = "components", remap = false)
    Set<BlockPos> ccq$getComponents();

    @Accessor(value = "controller", remap = false)
    StorageControllerEntity ccq$getController();

    @Accessor(value = "networkVersion", remap = false)
    int ccq$getNetworkVersion();
}
