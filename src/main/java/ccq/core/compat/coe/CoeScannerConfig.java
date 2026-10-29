package ccq.core.compat.coe;

public final class CoeScannerConfig {
    /** Chunk radius around the player (64 chunks ≈ 1024 blocks). */
    public static final int CHUNK_RADIUS = 64;

    /** Chunks within this radius are labeled as "nearby"; beyond that as "distant". */
    static final int NEARBY_CHUNK_RADIUS = 8;

    /** Rescan cooldown in ticks (20 ticks = 1 second). */
    static final int COOLDOWN_TICKS = 20;

    /** Placement checks processed per server tick while scanning. */
    static final int CHUNK_CHECKS_PER_TICK = 128;

    /** Hard cap so result packets stay within vanilla custom-payload limits. */
    static final int MAX_RESULTS = 256;

    private CoeScannerConfig() {
    }
}
