package ccq.core.compat.coe;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

final class ScanSessionTicker {
    private ScanSessionTicker() {}

    static <K, V> void tick(Map<K, V> sessions, BiConsumer<K, V> action) {
        // Completion/cancellation may remove sessions. New sessions wait until the next tick.
        for (K key : List.copyOf(sessions.keySet())) {
            V session = sessions.get(key);
            if (session != null) {
                action.accept(key, session);
            }
        }
    }
}
