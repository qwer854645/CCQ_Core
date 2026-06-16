package ccq.core.tacz;

import ccq.core.CcqCoreMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

final class AppliedArmorerPatch {
    static final String ZIP_GLOB = "Applied Armorer*.zip";
    static final String WORKBENCH_DATA_ENTRY = "data/applied_armorer/data/blocks/worckbench_applied_armorer_data.json";

    private AppliedArmorerPatch() {
    }

    static void applyIfPresent(Path taczDir) {
        try {
            Optional<Path> pack = findPack(taczDir);
            if (pack.isEmpty()) {
                CcqCoreMod.LOGGER.debug("Applied Armorer pack not found in {}, skipping CCQ patch", taczDir);
                return;
            }

            Path packPath = pack.get();
            if (isAlreadyPatched(packPath)) {
                CcqCoreMod.LOGGER.debug("Applied Armorer pack already patched: {}", packPath.getFileName());
                return;
            }

            byte[] patchContent = AppliedArmorerWorkbenchData.createPatchedContent();
            applyPatch(packPath, patchContent);
            CcqCoreMod.LOGGER.info("Patched Applied Armorer workbench data in {}", packPath.toAbsolutePath());
        } catch (IOException exception) {
            CcqCoreMod.LOGGER.error("Failed to apply Applied Armorer CCQ patch", exception);
        }
    }

    private static Optional<Path> findPack(Path taczDir) throws IOException {
        if (!Files.isDirectory(taczDir)) {
            return Optional.empty();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir, ZIP_GLOB)) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir)) {
            for (Path candidate : stream) {
                if (!Files.isDirectory(candidate)) {
                    continue;
                }
                Path meta = candidate.resolve("gunpack.meta.json");
                if (Files.isRegularFile(meta) && Files.readString(meta, StandardCharsets.UTF_8).contains("applied_armorer")) {
                    return Optional.of(candidate);
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isAlreadyPatched(Path packPath) throws IOException {
        byte[] current = readWorkbenchData(packPath);
        if (current == null) {
            return false;
        }
        return new String(current, StandardCharsets.UTF_8).contains("\"tabs\"");
    }

    private static byte[] readWorkbenchData(Path packPath) throws IOException {
        if (Files.isRegularFile(packPath)) {
            return GunPackZipPatcher.readEntry(packPath, WORKBENCH_DATA_ENTRY);
        }

        Path dataFile = packPath.resolve(WORKBENCH_DATA_ENTRY);
        if (Files.isRegularFile(dataFile)) {
            return Files.readAllBytes(dataFile);
        }
        return null;
    }

    private static void applyPatch(Path packPath, byte[] patchContent) throws IOException {
        if (Files.isRegularFile(packPath)) {
            GunPackZipPatcher.replaceEntry(packPath, WORKBENCH_DATA_ENTRY, patchContent);
            return;
        }

        Path dataFile = packPath.resolve(WORKBENCH_DATA_ENTRY);
        Files.createDirectories(dataFile.getParent());
        Files.write(dataFile, patchContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }
}
