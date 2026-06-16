package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AppliedArmorerPatch {
    private static final Logger LOGGER = LoggerFactory.getLogger("ccq_core");
    private static final Gson GSON = new Gson();
    static final String ZIP_GLOB = "Applied Armorer*.zip";
    static final String WORKBENCH_DATA_ENTRY = "data/applied_armorer/data/blocks/worckbench_applied_armorer_data.json";
    static final String PACK_META_ENTRY = "gunpack.meta.json";

    private AppliedArmorerPatch() {
    }

    static void applyIfPresent(Path taczDir) {
        try {
            List<Path> packs = findPacks(taczDir);
            if (packs.isEmpty()) {
                LOGGER.debug("Applied Armorer pack not found in {}, skipping CCQ patch", taczDir);
                return;
            }

            byte[] patchContent = AppliedArmorerWorkbenchData.createPatchedContent();
            for (Path packPath : packs) {
                try {
                    patchPackIfNeeded(packPath, patchContent);
                } catch (IOException exception) {
                    LOGGER.error("Failed to patch Applied Armorer pack {}", packPath.getFileName(), exception);
                }
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to scan for Applied Armorer packs", exception);
        }
    }

    private static void patchPackIfNeeded(Path packPath, byte[] patchContent) throws IOException {
        if (!isAppliedArmorerPack(packPath)) {
            LOGGER.warn("Skipping CCQ patch for non-Applied Armorer pack: {}", packPath.getFileName());
            return;
        }

        byte[] current = readWorkbenchData(packPath);
        if (current != null && AppliedArmorerWorkbenchData.matchesExpectedContent(current)) {
            LOGGER.debug("Applied Armorer pack already has expected workbench data: {}", packPath.getFileName());
            return;
        }

        if (current != null) {
            try {
                GSON.fromJson(decodeJsonText(current), JsonObject.class);
            } catch (JsonParseException exception) {
                LOGGER.warn("Applied Armorer workbench data is not valid JSON, will patch: {}", packPath.getFileName());
            }

            LOGGER.info("Updating outdated Applied Armorer workbench data in {}", packPath.getFileName());
        }

        applyPatch(packPath, patchContent);
        LOGGER.info("Patched Applied Armorer workbench data in {}", packPath.toAbsolutePath());
    }

    private static List<Path> findPacks(Path taczDir) throws IOException {
        if (!Files.isDirectory(taczDir)) {
            return List.of();
        }

        List<Path> packs = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir, ZIP_GLOB)) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate)) {
                    packs.add(candidate);
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir)) {
            for (Path candidate : stream) {
                if (!Files.isDirectory(candidate)) {
                    continue;
                }

                try {
                    if (isAppliedArmorerPack(candidate)) {
                        packs.add(candidate);
                    }
                } catch (IOException exception) {
                    LOGGER.warn("Failed to inspect gun pack directory {}, skipping", candidate.getFileName(), exception);
                }
            }
        }

        packs.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return packs;
    }

    private static boolean isAppliedArmorerPack(Path packPath) throws IOException {
        if (Files.isRegularFile(packPath)) {
            byte[] meta = GunPackZipPatcher.readEntry(packPath, PACK_META_ENTRY);
            return meta != null && containsAppliedArmorerNamespace(meta);
        }

        Path meta = packPath.resolve(PACK_META_ENTRY);
        return Files.isRegularFile(meta) && containsAppliedArmorerNamespace(Files.readAllBytes(meta));
    }

    private static boolean containsAppliedArmorerNamespace(byte[] metaBytes) {
        String meta = new String(metaBytes, StandardCharsets.UTF_8);
        return meta.contains("\"namespace\": \"applied_armorer\"")
                || meta.contains("\"namespace\":\"applied_armorer\"");
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

    private static String decodeJsonText(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        return new String(bytes, StandardCharsets.UTF_8);
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
