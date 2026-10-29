package ccq.core.compat.coe;

import ccq.core.CcqCoreMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CoeScannerSessions {
    private static final Map<UUID, ActiveScan> ACTIVE = new HashMap<>();

    private CoeScannerSessions() {
    }

    static void startScan(ServerPlayer player, InteractionHand hand, @Nullable ResourceLocation filter, OreVeinScannerMenu menu) {
        ItemStack stack = scannerStack(player, hand);
        if (stack.isEmpty()) {
            return;
        }
        if (!ACTIVE.containsKey(player.getUUID()) && player.getCooldowns().isOnCooldown(stack.getItem())) {
            return;
        }

        ActiveScan existing = ACTIVE.get(player.getUUID());
        if (existing != null) {
            existing.task.cancel();
        }

        if (existing == null) {
            player.getCooldowns().addCooldown(stack.getItem(), OreVeinScannerItem.cooldownTicks());
        }

        CoeScannerStorage.ScannerSavedState saved = CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
        CoeVeinScanTask task;
        try {
            task = new CoeVeinScanTask(player.serverLevel(), player.blockPosition(), filter);
        } catch (RuntimeException exception) {
            CcqCoreMod.LOGGER.warn("Ore vein scanner failed to start for {}", player.getGameProfile().getName(), exception);
            menu.applyState(filter, saved.entries(), saved.scanned(), false);
            CoeNetwork.sendScanResults(player, menu.containerId, saved.entries(), false, filter);
            return;
        }

        ActiveScan scan = new ActiveScan(hand, filter, task, menu);
        ACTIVE.put(player.getUUID(), scan);

        CoeScannerStorage.save(player, hand, new CoeScannerStorage.ScannerSavedState(
                filter,
                saved.entries(),
                saved.scanned(),
                true
        ));
        menu.applyState(filter, saved.entries(), saved.scanned(), true);
    }

    static void attachMenu(ServerPlayer player, InteractionHand hand, OreVeinScannerMenu menu) {
        ActiveScan active = ACTIVE.get(player.getUUID());
        if (active != null && active.hand == hand) {
            active.menu = menu;
            ItemStack stack = scannerStack(player, hand);
            CoeScannerStorage.ScannerSavedState saved = stack.isEmpty()
                    ? CoeScannerStorage.ScannerSavedState.empty()
                    : CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
            menu.applyState(active.filter, saved.entries(), saved.scanned(), true);
            return;
        }

        ItemStack stack = scannerStack(player, hand);
        CoeScannerStorage.ScannerSavedState saved = stack.isEmpty()
                ? CoeScannerStorage.ScannerSavedState.empty()
                : CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
        boolean scanning = saved.scanning() && isScanning(player.getUUID());
        menu.applyState(saved.filterRecipeId(), saved.entries(), saved.scanned(), scanning);
        if (saved.scanning() && !scanning) {
            clearScanningFlag(player, hand);
        }
    }

    static void handleLogout(ServerPlayer player) {
        ActiveScan active = ACTIVE.remove(player.getUUID());
        if (active != null) {
            active.task.cancel();
            clearScanningFlag(player, active.hand);
        } else {
            clearScanningFlag(player, InteractionHand.MAIN_HAND);
            clearScanningFlag(player, InteractionHand.OFF_HAND);
        }
    }

    static void detachMenu(ServerPlayer player, OreVeinScannerMenu menu) {
        ActiveScan active = ACTIVE.get(player.getUUID());
        if (active != null && active.menu == menu) {
            active.menu = null;
        }
    }

    static void tick(ServerPlayer player) {
        ActiveScan active = ACTIVE.get(player.getUUID());
        if (active == null) {
            return;
        }
        if (!hasScanner(player, active.hand)) {
            active.task.cancel();
            ACTIVE.remove(player.getUUID());
            clearScanningFlag(player, active.hand);
            return;
        }

        boolean finished;
        try {
            finished = active.task.tick(CoeScannerConfig.CHUNK_CHECKS_PER_TICK);
        } catch (RuntimeException exception) {
            CcqCoreMod.LOGGER.warn("Ore vein scanner crashed while scanning for {}", player.getGameProfile().getName(), exception);
            active.task.cancel();
            finished = true;
        }
        if (!finished) {
            return;
        }

        List<OreVeinScanEntry> results = active.task.isCancelled()
                ? loadSavedEntries(player, active.hand)
                : active.task.results();
        completeScan(player, active, results);
    }

    static boolean isScanning(UUID playerId) {
        return ACTIVE.containsKey(playerId);
    }

    static CoeScannerStorage.ScannerSavedState readStateForOpen(ServerPlayer player, InteractionHand hand) {
        ActiveScan active = ACTIVE.get(player.getUUID());
        if (active != null && active.hand == hand) {
            ItemStack stack = scannerStack(player, hand);
            CoeScannerStorage.ScannerSavedState saved = stack.isEmpty()
                    ? CoeScannerStorage.ScannerSavedState.empty()
                    : CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
            return new CoeScannerStorage.ScannerSavedState(
                    active.filter,
                    saved.entries(),
                    saved.scanned(),
                    true
            );
        }
        ItemStack stack = scannerStack(player, hand);
        if (stack.isEmpty()) {
            return CoeScannerStorage.ScannerSavedState.empty();
        }
        CoeScannerStorage.ScannerSavedState saved = CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
        if (saved.scanning() && !isScanning(player.getUUID())) {
            clearScanningFlag(player, hand);
            return new CoeScannerStorage.ScannerSavedState(
                    saved.filterRecipeId(),
                    saved.entries(),
                    saved.scanned(),
                    false
            );
        }
        return saved;
    }

    private static void completeScan(ServerPlayer player, ActiveScan active, List<OreVeinScanEntry> results) {
        ACTIVE.remove(player.getUUID());

        CoeScannerStorage.ScannerSavedState saved = new CoeScannerStorage.ScannerSavedState(
                active.filter,
                results,
                true,
                false
        );
        CoeScannerStorage.save(player, active.hand, saved);

        int containerId = active.menu != null ? active.menu.containerId : -1;
        if (active.menu != null) {
            active.menu.applyState(active.filter, results, true, false);
        }

        try {
            CoeNetwork.sendScanResults(player, containerId, results, true, active.filter);
        } catch (RuntimeException exception) {
            CcqCoreMod.LOGGER.warn("Ore vein scanner failed to send results to {}", player.getGameProfile().getName(), exception);
        }
    }

    private static List<OreVeinScanEntry> loadSavedEntries(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = scannerStack(player, hand);
        if (stack.isEmpty()) {
            return List.of();
        }
        return CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager()).entries();
    }

    private static void clearScanningFlag(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = scannerStack(player, hand);
        if (stack.isEmpty()) {
            return;
        }
        CoeScannerStorage.ScannerSavedState saved = CoeScannerStorage.load(stack, player.serverLevel().getRecipeManager());
        if (saved.scanning()) {
            CoeScannerStorage.save(player, hand, new CoeScannerStorage.ScannerSavedState(
                    saved.filterRecipeId(),
                    saved.entries(),
                    saved.scanned(),
                    false
            ));
        }
    }

    static ItemStack scannerStack(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(CoeItems.ORE_VEIN_SCANNER.get())) {
            return held;
        }
        if (player.getMainHandItem().is(CoeItems.ORE_VEIN_SCANNER.get())) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(CoeItems.ORE_VEIN_SCANNER.get())) {
            return player.getOffhandItem();
        }
        return ItemStack.EMPTY;
    }

    private static boolean hasScanner(ServerPlayer player, InteractionHand hand) {
        return !scannerStack(player, hand).isEmpty();
    }

    static void tickAll(net.minecraft.server.MinecraftServer server) {
        ScanSessionTicker.tick(ACTIVE, (playerId, scan) -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                scan.task.cancel();
                ACTIVE.remove(playerId);
                return;
            }
            tick(player);
        });
    }

    private static final class ActiveScan {
        private final InteractionHand hand;
        @Nullable
        private final ResourceLocation filter;
        private final CoeVeinScanTask task;
        private OreVeinScannerMenu menu;

        private ActiveScan(
                InteractionHand hand,
                @Nullable ResourceLocation filter,
                CoeVeinScanTask task,
                OreVeinScannerMenu menu
        ) {
            this.hand = hand;
            this.filter = filter;
            this.task = task;
            this.menu = menu;
        }
    }
}
