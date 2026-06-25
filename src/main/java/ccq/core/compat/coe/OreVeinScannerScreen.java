package ccq.core.compat.coe;

import ccq.core.compat.coe.network.OreVeinScanRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

public class OreVeinScannerScreen extends AbstractContainerScreen<OreVeinScannerMenu> {
    private static final int ROW_HEIGHT = 34;
    private static final int LIST_TOP = 48;
    private static final int FOOTER_HEIGHT = 26;
    private static final int LIST_PADDING = 2;

    private int scrollOffset;
    private int filterIndex;
    private List<VeinFilterOption> filterOptions = List.of(VeinFilterOption.all());
    private Button scanButton;
    private Button prevFilterButton;
    private Button nextFilterButton;

    public OreVeinScannerScreen(OreVeinScannerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 260;
        this.imageHeight = 214;
        this.titleLabelX = 8;
        this.titleLabelY = 4;
        this.inventoryLabelY = this.imageHeight + 100;
    }

    @Override
    protected void init() {
        super.init();
        filterOptions = VeinFilterOption.buildClientOptions();
        filterIndex = findFilterIndex(menu.getFilterRecipeId());

        prevFilterButton = Button.builder(Component.literal("<"), button -> cycleFilter(-1))
                .bounds(leftPos + 8, topPos + 22, 18, 18)
                .build();
        nextFilterButton = Button.builder(Component.literal(">"), button -> cycleFilter(1))
                .bounds(leftPos + imageWidth - 26, topPos + 22, 18, 18)
                .build();
        addRenderableWidget(prevFilterButton);
        addRenderableWidget(nextFilterButton);

        scanButton = Button.builder(Component.translatable("gui.ccq_core.ore_vein_scanner.scan"), button -> requestScan())
                .bounds(leftPos + 8, topPos + imageHeight - FOOTER_HEIGHT + 3, 88, 20)
                .build();
        addRenderableWidget(scanButton);
    }

    private int findFilterIndex(ResourceLocation filterRecipeId) {
        if (filterRecipeId == null) {
            return 0;
        }
        for (int i = 0; i < filterOptions.size(); i++) {
            if (filterRecipeId.equals(filterOptions.get(i).recipeId())) {
                return i;
            }
        }
        return 0;
    }

    private void cycleFilter(int delta) {
        if (filterOptions.isEmpty() || menu.isScanPending()) {
            return;
        }
        filterIndex = Math.floorMod(filterIndex + delta, filterOptions.size());
        scrollOffset = 0;
    }

    private void requestScan() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (menu.isScanPending()) {
            return;
        }
        if (minecraft.player.getCooldowns().isOnCooldown(CoeItems.ORE_VEIN_SCANNER.get())) {
            return;
        }

        menu.setScanPending(true);
        sendScanPacket();
    }

    private void sendScanPacket() {
        VeinFilterOption filter = filterOptions.get(filterIndex);
        Optional<ResourceLocation> recipeId = filter.recipeId() == null
                ? Optional.empty()
                : Optional.of(filter.recipeId());
        PacketDistributor.sendToServer(new OreVeinScanRequestPayload(menu.containerId, recipeId));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xC0101010);
        graphics.fill(x + 4, y + 18, x + imageWidth - 4, y + 44, 0x90202020);
        graphics.fill(x + 4, y + LIST_TOP, x + imageWidth - 4, y + imageHeight - FOOTER_HEIGHT, 0xA0000000);
        graphics.fill(x + 4, y + imageHeight - FOOTER_HEIGHT, x + imageWidth - 4, y + imageHeight - 2, 0x90202020);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderFilterBar(graphics);
        renderEntries(graphics);

        prevFilterButton.active = !menu.isScanPending();
        nextFilterButton.active = !menu.isScanPending();

        if (minecraft != null && minecraft.player != null) {
            boolean onCooldown = minecraft.player.getCooldowns().isOnCooldown(CoeItems.ORE_VEIN_SCANNER.get());
            scanButton.active = !onCooldown && !menu.isScanPending();
        }
        scanButton.setMessage(menu.hasScanned()
                ? Component.translatable("gui.ccq_core.ore_vein_scanner.rescan")
                : Component.translatable("gui.ccq_core.ore_vein_scanner.scan"));
    }

    private void renderFilterBar(GuiGraphics graphics) {
        if (filterOptions.isEmpty()) {
            return;
        }

        VeinFilterOption filter = filterOptions.get(filterIndex);
        int rowY = topPos + 23;
        graphics.renderItem(filter.icon(), leftPos + 30, rowY);
        graphics.renderItemDecorations(font, filter.icon(), leftPos + 30, rowY);

        String filterText = font.plainSubstrByWidth(filter.name().getString(), imageWidth - 92);
        graphics.drawString(
                font,
                Component.translatable("gui.ccq_core.ore_vein_scanner.filter_short", filterText),
                leftPos + 50,
                rowY + 5,
                0xFFFFFF,
                false
        );
    }

    private void renderEntries(GuiGraphics graphics) {
        int listTop = topPos + LIST_TOP + LIST_PADDING;
        int listBottom = topPos + imageHeight - FOOTER_HEIGHT - LIST_PADDING;
        int listHeight = listBottom - listTop;
        int maxScroll = Math.max(0, menu.getEntries().size() * ROW_HEIGHT - listHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        graphics.enableScissor(leftPos + 6, listTop, leftPos + imageWidth - 6, listBottom);

        if (menu.isScanPending()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.ccq_core.ore_vein_scanner.scanning"),
                    leftPos + 12,
                    listTop + 8,
                    0xFFFFAA,
                    false
            );
            if (menu.hasScanned() && !menu.getEntries().isEmpty()) {
                graphics.drawString(
                        font,
                        Component.translatable("gui.ccq_core.ore_vein_scanner.scanning_hint"),
                        leftPos + 12,
                        listTop + 20,
                        0x808080,
                        false
                );
            }
        } else if (!menu.hasScanned()) {
            graphics.drawString(
                    font,
                    Component.translatable("gui.ccq_core.ore_vein_scanner.idle"),
                    leftPos + 12,
                    listTop + 8,
                    0xA0A0A0,
                    false
            );
        } else if (menu.getEntries().isEmpty()) {
            Component emptyKey = filterIndex == 0
                    ? Component.translatable("gui.ccq_core.ore_vein_scanner.empty")
                    : Component.translatable("gui.ccq_core.ore_vein_scanner.empty_filtered");
            graphics.drawString(font, emptyKey, leftPos + 12, listTop + 8, 0xA0A0A0, false);
        } else {
            int y = listTop - scrollOffset;
            for (OreVeinScanEntry entry : menu.getEntries()) {
                if (y + ROW_HEIGHT > listTop && y < listBottom) {
                    renderEntry(graphics, entry, leftPos + 8, y);
                }
                y += ROW_HEIGHT;
            }
        }

        graphics.disableScissor();
    }

    private int listContentTop() {
        return topPos + LIST_TOP + LIST_PADDING;
    }

    private int listContentBottom() {
        return topPos + imageHeight - FOOTER_HEIGHT - LIST_PADDING;
    }

    private void renderEntry(GuiGraphics graphics, OreVeinScanEntry entry, int x, int y) {
        graphics.renderItem(entry.icon(), x, y + 8);
        graphics.renderItemDecorations(font, entry.icon(), x, y + 8);

        int textX = x + 20;
        int textWidth = imageWidth - 36;
        String name = font.plainSubstrByWidth(entry.name().getString(), textWidth);
        graphics.drawString(font, name, textX, y + 4, 0xFFFFFF, false);

        Component location = Component.translatable(
                "gui.ccq_core.ore_vein_scanner.entry_location",
                entry.centerX(),
                entry.centerZ(),
                entry.distance()
        );
        String locationText = font.plainSubstrByWidth(location.getString(), textWidth);
        graphics.drawString(font, locationText, textX, y + 14, 0xC0C0C0, false);

        Component status = Component.translatable(
                "gui.ccq_core.ore_vein_scanner.entry_status",
                kindLabel(entry.kind()),
                remainingLabel(entry)
        );
        String statusText = font.plainSubstrByWidth(status.getString(), textWidth);
        graphics.drawString(font, statusText, textX, y + 24, 0x909090, false);
    }

    private static Component kindLabel(OreVeinScanEntry.ScanKind kind) {
        return Component.translatable("gui.ccq_core.ore_vein_scanner.kind." + kind.name().toLowerCase());
    }

    private static Component remainingLabel(OreVeinScanEntry entry) {
        if (entry.infinite()) {
            return Component.translatable("gui.ccq_core.ore_vein_scanner.infinite");
        }
        return Component.translatable("gui.ccq_core.ore_vein_scanner.finite", entry.remaining());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX < leftPos + 4 || mouseX > leftPos + imageWidth - 4
                || mouseY < listContentTop() || mouseY > listContentBottom()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        int listHeight = listContentBottom() - listContentTop();
        int maxScroll = Math.max(0, menu.getEntries().size() * ROW_HEIGHT - listHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * ROW_HEIGHT / 2)));
        return true;
    }
}
