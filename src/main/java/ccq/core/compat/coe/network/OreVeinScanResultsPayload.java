package ccq.core.compat.coe.network;

import ccq.core.CcqCoreMod;
import ccq.core.compat.coe.CoeClientNetwork;
import ccq.core.compat.coe.OreVeinScanEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record OreVeinScanResultsPayload(
        int containerId,
        List<OreVeinScanEntry> entries,
        boolean notifyPlayer,
        java.util.Optional<ResourceLocation> filterRecipeId
) implements CustomPacketPayload {
    public static final Type<OreVeinScanResultsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "ore_vein_scan_results"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<OreVeinScanEntry>> ENTRY_LIST_CODEC =
            OreVeinScanEntry.STREAM_CODEC.apply(ByteBufCodecs.collection(ArrayList::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreVeinScanResultsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OreVeinScanResultsPayload::containerId,
                    ENTRY_LIST_CODEC, OreVeinScanResultsPayload::entries,
                    ByteBufCodecs.BOOL, OreVeinScanResultsPayload::notifyPlayer,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), OreVeinScanResultsPayload::filterRecipeId,
                    OreVeinScanResultsPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OreVeinScanResultsPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> CoeClientNetwork.handleScanResults(payload));
    }
}
