package ccq.core.compat.coe.network;

import ccq.core.CcqCoreMod;
import ccq.core.compat.coe.CoeNetwork;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record OreVeinScanRequestPayload(int containerId, Optional<ResourceLocation> filterRecipeId)
        implements CustomPacketPayload {
    public static final Type<OreVeinScanRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CcqCoreMod.MOD_ID, "ore_vein_scan_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OreVeinScanRequestPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OreVeinScanRequestPayload::containerId,
                    ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), OreVeinScanRequestPayload::filterRecipeId,
                    OreVeinScanRequestPayload::new
            );

    public OreVeinScanRequestPayload(int containerId) {
        this(containerId, Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OreVeinScanRequestPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> CoeNetwork.handleScanRequest(payload, context));
    }
}
