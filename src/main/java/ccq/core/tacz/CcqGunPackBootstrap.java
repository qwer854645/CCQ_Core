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
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CcqGunPackBootstrap {
    private static final List<EmbeddedGunPack> EMBEDDED_PACKS = List.of(
            EmbeddedGunPack.extractDirectory(
                    "ccq_default_gun",
                    "/embedded/ccq_default_gun.zip"
            )
    );

    private CcqGunPackBootstrap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EventPriority.HIGHEST, CcqGunPackBootstrap::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CcqGunPackBootstrap::ensureGunPacks);
    }

    static void ensureGunPacks() {
        if (!ModList.get().isLoaded("tacz")) {
            return;
        }

        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");

        for (EmbeddedGunPack pack : EMBEDDED_PACKS) {
            try {
                if (pack.isPresent(taczDir)) {
                    continue;
                }

                Files.createDirectories(taczDir);
                pack.install(taczDir);
                CcqCoreMod.LOGGER.info("Installed missing TaCZ gun pack: {}", pack.describeInstallTarget(taczDir));
            } catch (IOException exception) {
                CcqCoreMod.LOGGER.error("Failed to install TaCZ gun pack {}", pack.id(), exception);
            }
        }

        AppliedArmorerPatch.applyIfPresent(taczDir);
    }

    private record EmbeddedGunPack(
            String id,
            String embeddedResource,
            InstallMode installMode,
            String installName
    ) {
        private enum InstallMode {
            DIRECTORY
        }

        static EmbeddedGunPack extractDirectory(String directoryName, String embeddedResource) {
            return new EmbeddedGunPack(directoryName, embeddedResource, InstallMode.DIRECTORY, directoryName);
        }

        boolean isPresent(Path taczDir) throws IOException {
            Path packDir = taczDir.resolve(installName);
            return Files.isRegularFile(taczDir.resolve(installName + ".zip"))
                    || Files.isRegularFile(packDir.resolve("gunpack.meta.json"));
        }

        void install(Path taczDir) throws IOException {
            extractEmbeddedDirectory(taczDir.resolve(installName));
        }

        String describeInstallTarget(Path taczDir) {
            return taczDir.resolve(installName).toAbsolutePath().toString();
        }

        private void extractEmbeddedDirectory(Path packDir) throws IOException {
            try (InputStream input = openEmbeddedResource();
                 ZipInputStream zipInput = new ZipInputStream(input)) {
                Files.createDirectories(packDir);
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
                    Files.copy(zipInput, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        private InputStream openEmbeddedResource() throws IOException {
            InputStream input = CcqGunPackBootstrap.class.getResourceAsStream(embeddedResource);
            if (input == null) {
                throw new IOException("Embedded gun pack resource missing: " + embeddedResource);
            }
            return input;
        }
    }
}
