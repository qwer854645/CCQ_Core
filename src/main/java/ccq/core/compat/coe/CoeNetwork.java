package ccq.core.compat.coe;

import ccq.core.compat.coe.network.OreVeinScanRequestPayload;
import ccq.core.compat.coe.network.OreVeinScanResultsPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public final class CoeNetwork {
    private CoeNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(
                OreVeinScanRequestPayload.TYPE,
                OreVeinScanRequestPayload.STREAM_CODEC,
                OreVeinScanRequestPayload::handle
        );
        registrar.playToClient(
                OreVeinScanResultsPayload.TYPE,
                OreVeinScanResultsPayload.STREAM_CODEC,
                OreVeinScanResultsPayload::handle
        );
    }

    public static void sendScanResults(
            ServerPlayer player,
            int containerId,
            List<OreVeinScanEntry> entries,
            boolean notifyPlayer,
            @javax.annotation.Nullable ResourceLocation filterRecipeId
    ) {
        PacketDistributor.sendToPlayer(player, new OreVeinScanResultsPayload(
                containerId,
                entries,
                notifyPlayer,
                java.util.Optional.ofNullable(filterRecipeId)
        ));
    }

    public static void handleScanRequest(OreVeinScanRequestPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!(serverPlayer.containerMenu instanceof OreVeinScannerMenu menu) || menu.containerId != payload.containerId()) {
            return;
        }
        if (menu.isScanPending() || CoeScannerSessions.isScanning(serverPlayer.getUUID())) {
            return;
        }
        menu.performScan(serverPlayer, payload.filterRecipeId().orElse(null));
    }
}
