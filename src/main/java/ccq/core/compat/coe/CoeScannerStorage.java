package ccq.core.compat.coe;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class CoeScannerStorage {
    private static final String ROOT = "ccq_core_scanner";
    private static final String FILTER = "filter";
    private static final String SCANNING = "scanning";
    private static final String SCANNED = "scanned";
    private static final String ENTRIES = "entries";
    private static final String RECIPE = "recipe";
    private static final String CHUNK_X = "chunk_x";
    private static final String CHUNK_Z = "chunk_z";
    private static final String CENTER_X = "center_x";
    private static final String CENTER_Z = "center_z";
    private static final String DISTANCE = "distance";
    private static final String KIND = "kind";
    private static final String INFINITE = "infinite";
    private static final String REMAINING = "remaining";

    private CoeScannerStorage() {
    }

    static ScannerSavedState load(ItemStack stack, RecipeManager recipes) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return ScannerSavedState.empty();
        }
        CompoundTag root = custom.copyTag();
        if (!root.contains(ROOT, Tag.TAG_COMPOUND)) {
            return ScannerSavedState.empty();
        }
        CompoundTag tag = root.getCompound(ROOT);
        ResourceLocation filter = tag.contains(FILTER, Tag.TAG_STRING)
                ? ResourceLocation.parse(tag.getString(FILTER))
                : null;
        boolean scanning = tag.getBoolean(SCANNING);
        boolean scanned = tag.getBoolean(SCANNED);
        List<OreVeinScanEntry> entries = new ArrayList<>();
        if (tag.contains(ENTRIES, Tag.TAG_LIST)) {
            ListTag list = tag.getList(ENTRIES, Tag.TAG_COMPOUND);
            for (Tag element : list) {
                readEntry((CompoundTag) element, recipes).ifPresent(entries::add);
            }
        }
        return new ScannerSavedState(filter, entries, scanned, scanning);
    }

    static void save(ServerPlayer player, InteractionHand hand, ScannerSavedState state) {
        ItemStack stack = CoeScannerSessions.scannerStack(player, hand);
        if (stack.isEmpty()) {
            return;
        }

        CompoundTag root = Optional.ofNullable(stack.get(DataComponents.CUSTOM_DATA))
                .map(CustomData::copyTag)
                .orElseGet(CompoundTag::new);
        CompoundTag tag = new CompoundTag();
        if (state.filterRecipeId() != null) {
            tag.putString(FILTER, state.filterRecipeId().toString());
        }
        tag.putBoolean(SCANNING, state.scanning());
        tag.putBoolean(SCANNED, state.scanned());
        ListTag list = new ListTag();
        for (OreVeinScanEntry entry : state.entries()) {
            list.add(writeEntry(entry));
        }
        tag.put(ENTRIES, list);
        root.put(ROOT, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    static void writeOpenBuffer(RegistryFriendlyByteBuf buf, ScannerSavedState state) {
        buf.writeBoolean(state.scanning());
        buf.writeBoolean(state.scanned());
        ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC)
                .encode(buf, java.util.Optional.ofNullable(state.filterRecipeId()));
        ENTRY_LIST_CODEC.encode(buf, state.entries());
    }

    static ScannerSavedState readOpenBuffer(RegistryFriendlyByteBuf buf) {
        boolean scanning = buf.readBoolean();
        boolean scanned = buf.readBoolean();
        ResourceLocation filter = ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC).decode(buf).orElse(null);
        List<OreVeinScanEntry> entries = ENTRY_LIST_CODEC.decode(buf);
        return new ScannerSavedState(filter, entries, scanned, scanning);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, List<OreVeinScanEntry>> ENTRY_LIST_CODEC =
            OreVeinScanEntry.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new));

    private static CompoundTag writeEntry(OreVeinScanEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putString(RECIPE, entry.recipeId().toString());
        tag.putInt(CHUNK_X, entry.chunkX());
        tag.putInt(CHUNK_Z, entry.chunkZ());
        tag.putInt(CENTER_X, entry.centerX());
        tag.putInt(CENTER_Z, entry.centerZ());
        tag.putInt(DISTANCE, entry.distance());
        tag.putString(KIND, entry.kind().name());
        tag.putBoolean(INFINITE, entry.infinite());
        tag.putLong(REMAINING, entry.remaining());
        return tag;
    }

    private static Optional<OreVeinScanEntry> readEntry(CompoundTag tag, RecipeManager recipes) {
        ResourceLocation recipeId = ResourceLocation.parse(tag.getString(RECIPE));
        var holder = recipes.byKey(recipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof com.tom.createores.recipe.VeinRecipe vein)) {
            return Optional.empty();
        }
        OreVeinScanEntry.ScanKind kind = OreVeinScanEntry.ScanKind.valueOf(tag.getString(KIND));
        return Optional.of(new OreVeinScanEntry(
                recipeId,
                vein.getName(),
                tag.getInt(CHUNK_X),
                tag.getInt(CHUNK_Z),
                tag.getInt(CENTER_X),
                tag.getInt(CENTER_Z),
                tag.getInt(DISTANCE),
                kind,
                vein.getIcon().copy(),
                tag.getBoolean(INFINITE),
                tag.getLong(REMAINING)
        ));
    }

    record ScannerSavedState(
            @Nullable ResourceLocation filterRecipeId,
            List<OreVeinScanEntry> entries,
            boolean scanned,
            boolean scanning
    ) {
        static ScannerSavedState empty() {
            return new ScannerSavedState(null, List.of(), false, false);
        }
    }
}
