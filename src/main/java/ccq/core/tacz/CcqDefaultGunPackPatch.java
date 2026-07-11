package ccq.core.tacz;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class CcqDefaultGunPackPatch {
    private static final Logger LOGGER = LoggerFactory.getLogger("ccq_core");

    private CcqDefaultGunPackPatch() {
    }

    static void applyIfPresent(Path taczDir) {
        try {
            Files.createDirectories(taczDir);
            Path outputDir = taczDir.resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME);
            List<Path> sources = findSourcePacks(taczDir);

            if (isStrippedPackPresent(outputDir)) {
                cleanupSources(sources);
                LOGGER.debug("CCQ default gun pack already present: {}", outputDir.getFileName());
                return;
            }

            if (!sources.isEmpty()) {
                Path source = sources.get(0);
                stripToOutput(taczDir, source, outputDir);
                cleanupSources(sources);
                LOGGER.info("Stripped TACZ default gun pack into {}", outputDir.toAbsolutePath());
                return;
            }

            Path modRoot = findTaczModRoot();
            if (modRoot == null) {
                LOGGER.debug("TACZ default gun pack not found in {} and tacz mod root is unavailable", taczDir);
                return;
            }

            stripFromModRoot(taczDir, modRoot, outputDir);
            LOGGER.info("Extracted and stripped TACZ default gun pack from mod root into {}", outputDir.toAbsolutePath());
        } catch (IOException exception) {
            LOGGER.error("Failed to strip TACZ default gun pack", exception);
        }
    }

    static void stripFromModJar(Path modJar, Path outputDir) throws IOException {
        stripFromModRoot(outputDir.getParent(), modJar, outputDir);
    }

    private static void stripFromModRoot(Path taczDir, Path modRoot, Path outputDir) throws IOException {
        publishStrippedPack(taczDir, outputDir, stagingDir -> {
            if (Files.isRegularFile(modRoot)) {
                GunPackFileOperations.copyKeepListFromPrefixedZip(
                        modRoot,
                        CcqDefaultGunPackContent.MOD_JAR_PACK_PREFIX,
                        stagingDir,
                        CcqDefaultGunPackContent.KEEP_FILES
                );
                GunPackFileOperations.copyKeepPrefixesFromPrefixedZip(
                        modRoot,
                        CcqDefaultGunPackContent.MOD_JAR_PACK_PREFIX,
                        stagingDir,
                        CcqDefaultGunPackContent.KEEP_PREFIXES
                );
            } else if (Files.isDirectory(modRoot)) {
                GunPackFileOperations.copyKeepListFromPrefixedDirectory(
                        modRoot,
                        CcqDefaultGunPackContent.MOD_JAR_PACK_PREFIX,
                        stagingDir,
                        CcqDefaultGunPackContent.KEEP_FILES
                );
                GunPackFileOperations.copyKeepPrefixesFromPrefixedDirectory(
                        modRoot,
                        CcqDefaultGunPackContent.MOD_JAR_PACK_PREFIX,
                        stagingDir,
                        CcqDefaultGunPackContent.KEEP_PREFIXES
                );
            } else {
                throw new IOException("Unsupported tacz mod root: " + modRoot);
            }
        });
    }

    private static void stripToOutput(Path taczDir, Path source, Path outputDir) throws IOException {
        publishStrippedPack(taczDir, outputDir, stagingDir -> {
            if (Files.isRegularFile(source)) {
                GunPackFileOperations.copyKeepListFromZip(source, stagingDir, CcqDefaultGunPackContent.KEEP_FILES);
                GunPackFileOperations.copyKeepPrefixesFromZip(source, stagingDir, CcqDefaultGunPackContent.KEEP_PREFIXES);
            } else {
                GunPackFileOperations.copyKeepListFromDirectory(source, stagingDir, CcqDefaultGunPackContent.KEEP_FILES);
                GunPackFileOperations.copyKeepPrefixesFromDirectory(source, stagingDir, CcqDefaultGunPackContent.KEEP_PREFIXES);
            }
        });
    }

    @FunctionalInterface
    private interface StagingWriter {
        void write(Path stagingDir) throws IOException;
    }

    private static void publishStrippedPack(Path taczDir, Path outputDir, StagingWriter writer) throws IOException {
        clearGunsmithCache(taczDir);
        Path stagingDir = taczDir.resolve(CcqDefaultGunPackContent.STAGING_DIR_NAME);
        try {
            GunPackFileOperations.resetDirectory(stagingDir);
            writer.write(stagingDir);
            writeGeneratedContent(stagingDir);
            GunPackFileOperations.publishDirectory(stagingDir, outputDir);
        } finally {
            if (Files.exists(stagingDir)) {
                GunPackFileOperations.deletePack(stagingDir);
            }
            clearGunsmithCache(taczDir);
        }
    }

    private static void writeGeneratedContent(Path outputDir) throws IOException {
        GunPackFileOperations.writeRelativeBytes(outputDir, "assets/tacz/gunpack_info.json", CcqDefaultGunPackContent.createGunpackInfoJson());
        GunPackFileOperations.writeRelativeBytes(outputDir, "assets/tacz/lang/en_us.json", CcqDefaultGunPackContent.createEnglishLangJson());
        GunPackFileOperations.writeRelativeBytes(outputDir, "assets/tacz/lang/zh_cn.json", CcqDefaultGunPackContent.createChineseLangJson());
        GunPackFileOperations.writeRelativeBytes(outputDir, "NOTICE.txt", CcqDefaultGunPackContent.createNoticeText());
    }

    private static Path findTaczModRoot() {
        var modFile = ModList.get().getModFileById("tacz");
        if (modFile == null) {
            return null;
        }

        Path modRoot = modFile.getFile().getFilePath();
        if (Files.isRegularFile(modRoot) || Files.isDirectory(modRoot)) {
            return modRoot;
        }

        return null;
    }

    private static void clearGunsmithCache(Path taczDir) {
        Path cacheDir = taczDir.getParent().resolve(".gunsmithlib/upgrade_cache/v3");
        GunPackFileOperations.deleteIfExists(cacheDir.resolve("ccq_default_gun.zip"));
        GunPackFileOperations.deleteIfExists(cacheDir.resolve("tacz_default_gun.zip"));
    }

    private static boolean isStrippedPackPresent(Path outputDir) throws IOException {
        if (!Files.isDirectory(outputDir)) {
            return false;
        }

        byte[] meta = GunPackFileOperations.readPackMeta(outputDir);
        if (meta == null || !GunPackFileOperations.containsTaczNamespace(meta)) {
            return false;
        }

        for (String relativePath : CcqDefaultGunPackContent.KEEP_FILES) {
            if (!Files.isRegularFile(outputDir.resolve(relativePath))) {
                return false;
            }
        }

        if (!Files.isRegularFile(outputDir.resolve("assets/tacz/gunpack_info.json"))) {
            return false;
        }

        if (!Files.isRegularFile(outputDir.resolve("assets/tacz/lang/en_us.json"))) {
            return false;
        }

        if (!Files.isRegularFile(outputDir.resolve("assets/tacz/lang/zh_cn.json"))) {
            return false;
        }

        if (!Files.isRegularFile(outputDir.resolve(CcqDefaultGunPackContent.SHARED_SOUND_MARKER))) {
            return false;
        }

        if (!Files.isRegularFile(outputDir.resolve(CcqDefaultGunPackContent.AK47_SHARED_SOUND_MARKER))) {
            return false;
        }

        return !Files.exists(outputDir.resolve("data/tacz/index/guns"));
    }

    private static List<Path> findSourcePacks(Path taczDir) throws IOException {
        if (!Files.isDirectory(taczDir)) {
            return List.of();
        }

        List<Path> sources = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir, CcqDefaultGunPackContent.SOURCE_ZIP_GLOB)) {
            for (Path candidate : stream) {
                if (Files.isRegularFile(candidate) && isTaczDefaultSource(candidate)) {
                    sources.add(candidate);
                }
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(taczDir)) {
            for (Path candidate : stream) {
                if (!Files.isDirectory(candidate) || !isTaczDefaultSource(candidate)) {
                    continue;
                }

                try {
                    byte[] meta = GunPackFileOperations.readPackMeta(candidate);
                    if (meta != null && GunPackFileOperations.containsTaczNamespace(meta)) {
                        sources.add(candidate);
                    }
                } catch (IOException exception) {
                    LOGGER.warn("Failed to inspect TACZ default gun pack {}, skipping", candidate.getFileName(), exception);
                }
            }
        }

        sources.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return sources;
    }

    private static boolean isTaczDefaultSource(Path packPath) {
        String name = packPath.getFileName().toString();
        if (name.regionMatches(true, 0, CcqDefaultGunPackContent.OUTPUT_DIR_NAME, 0, CcqDefaultGunPackContent.OUTPUT_DIR_NAME.length())) {
            return false;
        }

        return name.regionMatches(true, 0, "tacz_default_gun", 0, "tacz_default_gun".length());
    }

    private static void cleanupSources(List<Path> sources) {
        for (Path source : sources) {
            try {
                GunPackFileOperations.deletePack(source);
                LOGGER.info("Removed original TACZ default gun pack: {}", source.getFileName());
            } catch (IOException exception) {
                LOGGER.error("Failed to remove original TACZ default gun pack {}", source.getFileName(), exception);
            }
        }
    }
}
