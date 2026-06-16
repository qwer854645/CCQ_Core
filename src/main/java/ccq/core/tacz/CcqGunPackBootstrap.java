package ccq.core.tacz;

import ccq.core.CcqCoreMod;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CcqGunPackBootstrap {
    public static final String PACK_ID = "ccq_default_gun";
    private static final String EMBEDDED_ZIP = "/embedded/ccq_default_gun.zip";

    private CcqGunPackBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.HIGHEST, CcqGunPackBootstrap::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CcqGunPackBootstrap::ensureGunPack);
    }

    static void ensureGunPack() {
        if (!ModList.get().isLoaded("tacz")) {
            return;
        }

        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");
        if (isPackPresent(taczDir)) {
            return;
        }

        try {
            Files.createDirectories(taczDir);
            Path packDir = taczDir.resolve(PACK_ID);
            extractEmbeddedPack(packDir);
            CcqCoreMod.LOGGER.info("Installed missing TaCZ gun pack at {}", packDir.toAbsolutePath());
        } catch (IOException exception) {
            CcqCoreMod.LOGGER.error("Failed to install TaCZ gun pack {}", PACK_ID, exception);
        }
    }

    static boolean isPackPresent(Path taczDir) {
        if (Files.isRegularFile(taczDir.resolve(PACK_ID + ".zip"))) {
            return true;
        }

        Path packDir = taczDir.resolve(PACK_ID);
        return Files.isRegularFile(packDir.resolve("gunpack.meta.json"));
    }

    private static void extractEmbeddedPack(Path packDir) throws IOException {
        try (InputStream input = CcqGunPackBootstrap.class.getResourceAsStream(EMBEDDED_ZIP)) {
            if (input == null) {
                throw new IOException("Embedded gun pack resource missing: " + EMBEDDED_ZIP);
            }

            Files.createDirectories(packDir);
            try (ZipInputStream zipInput = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        Files.createDirectories(packDir.resolve(entry.getName()));
                        continue;
                    }

                    Path target = packDir.resolve(entry.getName()).normalize();
                    if (!target.startsWith(packDir.normalize())) {
                        throw new IOException("Blocked zip entry outside gun pack directory: " + entry.getName());
                    }

                    Files.createDirectories(target.getParent());
                    Files.copy(zipInput, target);
                }
            }
        }
    }
}
