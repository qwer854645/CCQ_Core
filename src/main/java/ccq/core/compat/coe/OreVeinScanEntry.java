package ccq.core.compat.coe;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record OreVeinScanEntry(
        ResourceLocation recipeId,
        Component name,
        int chunkX,
        int chunkZ,
        int centerX,
        int centerZ,
        int distance,
        ScanKind kind,
        ItemStack icon,
        boolean infinite,
        long remaining
) {
    public enum ScanKind {
        CURRENT,
        NEARBY,
        DISTANT
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, OreVeinScanEntry> STREAM_CODEC =
            StreamCodec.of(OreVeinScanEntry::write, OreVeinScanEntry::read);

    private static void write(RegistryFriendlyByteBuf buf, OreVeinScanEntry entry) {
        buf.writeResourceLocation(entry.recipeId);
        ComponentSerialization.STREAM_CODEC.encode(buf, entry.name);
        buf.writeVarInt(entry.chunkX);
        buf.writeVarInt(entry.chunkZ);
        buf.writeVarInt(entry.centerX);
        buf.writeVarInt(entry.centerZ);
        buf.writeVarInt(entry.distance);
        buf.writeEnum(entry.kind);
        ItemStack.STREAM_CODEC.encode(buf, entry.icon);
        buf.writeBoolean(entry.infinite);
        buf.writeVarLong(entry.remaining);
    }

    private static OreVeinScanEntry read(RegistryFriendlyByteBuf buf) {
        ResourceLocation recipeId = buf.readResourceLocation();
        Component name = ComponentSerialization.STREAM_CODEC.decode(buf);
        int chunkX = buf.readVarInt();
        int chunkZ = buf.readVarInt();
        int centerX = buf.readVarInt();
        int centerZ = buf.readVarInt();
        int distance = buf.readVarInt();
        ScanKind kind = buf.readEnum(ScanKind.class);
        ItemStack icon = ItemStack.STREAM_CODEC.decode(buf);
        boolean infinite = buf.readBoolean();
        long remaining = buf.readVarLong();
        return new OreVeinScanEntry(recipeId, name, chunkX, chunkZ, centerX, centerZ, distance, kind, icon, infinite, remaining);
    }
}
