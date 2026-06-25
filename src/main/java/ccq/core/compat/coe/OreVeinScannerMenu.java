package ccq.core.compat.coe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OreVeinScannerMenu extends AbstractContainerMenu {
    private final InteractionHand hand;
    private final Player player;
    private List<OreVeinScanEntry> entries = new ArrayList<>();
    private boolean scanned;
    private boolean scanPending;
    @Nullable
    private ResourceLocation filterRecipeId;

    public OreVeinScannerMenu(int containerId, Inventory inventory) {
        this(CoeMenus.ORE_VEIN_SCANNER.get(), containerId, inventory, InteractionHand.MAIN_HAND,
                CoeScannerStorage.ScannerSavedState.empty());
    }

    public OreVeinScannerMenu(int containerId, Inventory inventory, InteractionHand hand, CoeScannerStorage.ScannerSavedState initial) {
        this(CoeMenus.ORE_VEIN_SCANNER.get(), containerId, inventory, hand, initial);
    }

    public OreVeinScannerMenu(
            MenuType<?> type,
            int containerId,
            Inventory inventory,
            InteractionHand hand,
            CoeScannerStorage.ScannerSavedState initial
    ) {
        super(type, containerId);
        this.hand = hand;
        this.player = inventory.player;
        applyState(initial.filterRecipeId(), initial.entries(), initial.scanned(), initial.scanning());
        if (player instanceof ServerPlayer serverPlayer) {
            CoeScannerSessions.attachMenu(serverPlayer, hand, this);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            CoeScannerSessions.detachMenu(serverPlayer, this);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return hasScanner(player);
    }

    private boolean hasScanner(Player player) {
        if (player.getItemInHand(hand).is(CoeItems.ORE_VEIN_SCANNER.get())) {
            return true;
        }
        return player.getMainHandItem().is(CoeItems.ORE_VEIN_SCANNER.get())
                || player.getOffhandItem().is(CoeItems.ORE_VEIN_SCANNER.get());
    }

    public InteractionHand getHand() {
        return hand;
    }

    @Nullable
    public ResourceLocation getFilterRecipeId() {
        return filterRecipeId;
    }

    public boolean hasScanned() {
        return scanned;
    }

    public boolean isScanPending() {
        return scanPending;
    }

    public void setScanPending(boolean scanPending) {
        this.scanPending = scanPending;
    }

    public List<OreVeinScanEntry> getEntries() {
        return entries;
    }

    public void applyState(
            @Nullable ResourceLocation filterRecipeId,
            List<OreVeinScanEntry> entries,
            boolean scanned,
            boolean scanPending
    ) {
        this.filterRecipeId = filterRecipeId;
        this.entries = new ArrayList<>(entries);
        this.scanned = scanned;
        this.scanPending = scanPending;
    }

    public void performScan(ServerPlayer serverPlayer, @Nullable ResourceLocation filterRecipeId) {
        CoeScannerSessions.startScan(serverPlayer, hand, filterRecipeId, this);
    }

    public List<OreVeinScanEntry> entriesView() {
        return Collections.unmodifiableList(entries);
    }
}
