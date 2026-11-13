package ccq.core.testground;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

final class PortalLinks extends SavedData {
    record Endpoint(ResourceKey<Level> dimension, BlockPos pos) {}
    private final Map<Endpoint, Endpoint> links = new HashMap<>();
    static PortalLinks get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PortalLinks::new, PortalLinks::load), "ccq_testground_portals");
    }
    Endpoint target(Endpoint source) { return links.get(source); }
    void pair(Endpoint a, Endpoint b) {
        unpair(a); unpair(b);
        links.put(a, b); links.put(b, a); setDirty();
    }
    private void unpair(Endpoint a) {
        Endpoint old = links.remove(a);
        if (old != null) links.remove(old, a);
    }
    static PortalLinks load(CompoundTag tag, HolderLookup.Provider registries) {
        PortalLinks data = new PortalLinks();
        for (Tag item : tag.getList("links", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag)item;
            try { data.links.put(read(row.getCompound("from")), read(row.getCompound("to"))); }
            catch (IllegalArgumentException ignored) { /* Skip invalid old entries. */ }
        }
        return data;
    }
    private static Endpoint read(CompoundTag tag) {
        return new Endpoint(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("dimension"))), BlockPos.of(tag.getLong("pos")));
    }
    private static CompoundTag write(Endpoint end) {
        CompoundTag tag = new CompoundTag(); tag.putString("dimension", end.dimension.location().toString()); tag.putLong("pos", end.pos.asLong()); return tag;
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        links.forEach((a,b) -> { CompoundTag row = new CompoundTag(); row.put("from", write(a)); row.put("to", write(b)); list.add(row); });
        tag.put("links", list); return tag;
    }
}
