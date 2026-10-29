package ccq.core.compat.coe;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanSessionTickerTest {
    @Test
    void completingAndCancellingScansDoesNotInterruptOtherPlayers() {
        Map<String, String> sessions = new LinkedHashMap<>();
        sessions.put("completed", "scan1");
        sessions.put("cancelled", "scan2");
        sessions.put("running", "scan3");
        List<String> ticked = new ArrayList<>();
        ScanSessionTicker.tick(sessions, (player, scan) -> {
            ticked.add(player);
            if (!player.equals("running")) sessions.remove(player);
        });
        assertEquals(List.of("completed", "cancelled", "running"), ticked);
        assertEquals(Map.of("running", "scan3"), sessions);
    }

    @Test
    void removedSessionsAreSkippedAndNewSessionsWaitUntilNextTick() {
        Map<String, String> sessions = new LinkedHashMap<>();
        sessions.put("first", "scan1");
        sessions.put("removed", "scan2");
        List<String> ticked = new ArrayList<>();
        ScanSessionTicker.tick(sessions, (player, scan) -> {
            ticked.add(player);
            sessions.remove("removed");
            sessions.put("new", "scan3");
        });
        assertEquals(List.of("first"), ticked);
    }
}
