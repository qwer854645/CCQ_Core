package ccq.core.compat.storage.mixin;

import ccq.core.compat.storage.LazyBridgeItemHandler;
import ccq.core.compat.storage.StorageBridgeBlockEntity;
import ccq.core.compat.storage.StorageBridgeRegistry;
import com.buuz135.functionalstorage.block.tile.StorageControllerTile;
import com.buuz135.functionalstorage.util.ConnectedDrawers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vanilla rebuild() drops non-drawer positions. Snapshot + registry restore bridges
 * without scanning the whole controller AABB every rebuild.
 * <p>
 * If several bridges share the same FXNT controller, only the first (lowest BlockPos)
 * contributes an item handler so the FXNT inventory is not exposed twice.
 */
@Mixin(ConnectedDrawers.class)
public abstract class ConnectedDrawersMixin {
    @Shadow
    @Final
    private StorageControllerTile<?> controllerTile;

    @Shadow
    private Level level;

    @Shadow
    private List<Long> connectedDrawers;

    @Shadow
    private List<IItemHandler> itemHandlers;

    @Shadow(remap = false)
    public abstract void rebuildShapes();

    @Unique
    private final Set<Long> ccq$bridgeSnapshot = new HashSet<>();

    @Inject(method = "rebuild", at = @At("HEAD"), remap = false)
    private void ccq$snapshotBridges(CallbackInfo ci) {
        ccq$bridgeSnapshot.clear();
        if (level == null || connectedDrawers == null) {
            return;
        }
        for (Long packed : connectedDrawers) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockEntity(pos) instanceof StorageBridgeBlockEntity) {
                ccq$bridgeSnapshot.add(packed);
            }
        }
        if (controllerTile != null) {
            for (BlockPos bridgePos : StorageBridgeRegistry.bridgesFor(level, controllerTile.getBlockPos())) {
                ccq$bridgeSnapshot.add(bridgePos.asLong());
            }
        }
    }

    @Inject(method = "rebuild", at = @At("TAIL"), remap = false)
    private void ccq$restoreBridges(CallbackInfo ci) {
        if (level == null || level.isClientSide || controllerTile == null || itemHandlers == null || connectedDrawers == null) {
            ccq$bridgeSnapshot.clear();
            return;
        }

        BlockPos controllerPos = controllerTile.getBlockPos();
        boolean added = false;
        Set<BlockPos> seenFxntControllers = new HashSet<>();

        List<Long> ordered = new ArrayList<>(ccq$bridgeSnapshot);
        ordered.sort(Long::compareTo);

        for (Long packed : ordered) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof StorageBridgeBlockEntity bridge)) {
                StorageBridgeRegistry.unregister(level, controllerPos, pos);
                continue;
            }

            BlockPos linked = bridge.getFsControllerPos();
            if (linked != null && !linked.equals(controllerPos)) {
                continue;
            }
            if (linked == null) {
                bridge.setFsController(controllerPos);
            }

            if (!connectedDrawers.contains(packed)) {
                connectedDrawers.add(packed);
            }

            BlockPos fxntKey = bridge.getFxntControllerPos();
            if (fxntKey != null && !seenFxntControllers.add(fxntKey.immutable())) {
                continue;
            }

            itemHandlers.add(new LazyBridgeItemHandler(bridge::getStorageForFunctionalStorage));
            added = true;
        }

        if (added && controllerTile.inventoryHandler != null) {
            controllerTile.inventoryHandler.invalidateSlots();
        }
        // FS keeps its linking-tool outline separately from the inventory selectors.
        // Rebuild it after bridge positions have been restored, not before.
        if (!ordered.isEmpty()) {
            rebuildShapes();
        }
        ccq$bridgeSnapshot.clear();
    }
}
