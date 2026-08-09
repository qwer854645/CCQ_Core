package ccq.core.compat.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks bridges linked to each Functional Storage controller so rebuild() can
 * restore them after vanilla strips non-drawer positions — without scanning a cube.
 */
public final class StorageBridgeRegistry {
    private static final ConcurrentHashMap<Key, Set<BlockPos>> BY_CONTROLLER = new ConcurrentHashMap<>();

    private StorageBridgeRegistry() {
    }

    public static void register(Level level, BlockPos controllerPos, BlockPos bridgePos) {
        if (level == null || controllerPos == null || bridgePos == null) {
            return;
        }
        Key key = key(level, controllerPos);
        BY_CONTROLLER
                .computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet())
                .add(bridgePos.immutable());
    }

    public static void unregister(Level level, BlockPos controllerPos, BlockPos bridgePos) {
        if (level == null || controllerPos == null || bridgePos == null) {
            return;
        }
        Key key = key(level, controllerPos);
        Set<BlockPos> bridges = BY_CONTROLLER.get(key);
        if (bridges == null) {
            return;
        }
        bridges.remove(bridgePos);
        if (bridges.isEmpty()) {
            BY_CONTROLLER.remove(key, bridges);
        }
    }

    public static void rebind(Level level, BlockPos oldController, BlockPos newController, BlockPos bridgePos) {
        if (oldController != null && !oldController.equals(newController)) {
            unregister(level, oldController, bridgePos);
        }
        if (newController != null) {
            register(level, newController, bridgePos);
        }
    }

    public static Set<BlockPos> bridgesFor(Level level, BlockPos controllerPos) {
        if (level == null || controllerPos == null) {
            return Collections.emptySet();
        }
        Set<BlockPos> bridges = BY_CONTROLLER.get(key(level, controllerPos));
        return bridges == null ? Collections.emptySet() : Set.copyOf(bridges);
    }

    private static Key key(Level level, BlockPos controllerPos) {
        return new Key(level.dimension(), controllerPos.immutable());
    }

    private record Key(ResourceKey<Level> dimension, BlockPos controllerPos) {
    }
}
