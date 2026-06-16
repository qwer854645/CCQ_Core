package ccq.core.tacz;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CcqDefaultGunPackPatchTest {
    @Test
    void stripsInstalledDefaultGunFolder(@TempDir Path tempDir) throws Exception {
        Path referenceDir = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/ccq_default_gun");
        assumeTrue(Files.isDirectory(referenceDir), "Reference stripped pack not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Path sourceDir = taczDir.resolve("tacz_default_gun");
        Files.createDirectories(sourceDir);
        copyDirectory(referenceDir, sourceDir);
        Files.createDirectories(sourceDir.resolve("data/tacz/index/guns"));
        Files.writeString(sourceDir.resolve("data/tacz/index/guns/should_be_removed.json"), "{}");

        CcqDefaultGunPackPatch.applyIfPresent(taczDir);

        Path outputDir = taczDir.resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME);
        assertTrue(Files.isDirectory(outputDir));
        assertFalse(Files.exists(taczDir.resolve("tacz_default_gun")));
        assertFalse(Files.exists(outputDir.resolve("data/tacz/index/guns/should_be_removed.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("assets/tacz/scripts/default_state_machine.lua")));
        assertTrue(Files.isRegularFile(outputDir.resolve("assets/tacz/gunpack_info.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("NOTICE.txt")));
    }

    @Test
    void stripsDefaultGunPackFromTaczModJar(@TempDir Path tempDir) throws Exception {
        Path taczJar = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/mods/tacz-neoforge-1.21.1-1.1.8-hotfix-r1.jar");
        assumeTrue(Files.isRegularFile(taczJar), "TaCZ mod jar not available on this machine");

        Path outputDir = tempDir.resolve("tacz").resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME);
        CcqDefaultGunPackPatch.stripFromModJar(taczJar, outputDir);

        assertTrue(Files.isRegularFile(outputDir.resolve("assets/tacz/scripts/default_state_machine.lua")));
        assertTrue(Files.isRegularFile(outputDir.resolve("assets/tacz/gunpack_info.json")));
        assertTrue(Files.isRegularFile(outputDir.resolve("NOTICE.txt")));
        assertFalse(Files.exists(outputDir.resolve("data/tacz/index/guns")));
    }

    @Test
    void skipsWhenStrippedPackAlreadyPresent(@TempDir Path tempDir) throws Exception {
        Path referenceDir = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/ccq_default_gun");
        assumeTrue(Files.isDirectory(referenceDir), "Reference stripped pack not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Path outputDir = taczDir.resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME);
        Path sourceDir = taczDir.resolve("tacz_default_gun");
        copyDirectory(referenceDir, outputDir);
        copyDirectory(referenceDir, sourceDir);
        Files.createDirectories(sourceDir.resolve("data/tacz/index/guns"));
        Files.writeString(sourceDir.resolve("data/tacz/index/guns/extra.json"), "{}");

        CcqDefaultGunPackPatch.applyIfPresent(taczDir);

        assertFalse(Files.exists(sourceDir));
        assertFalse(Files.exists(outputDir.resolve("data/tacz/index/guns/extra.json")));
    }

    @Test
    void repairsIncompleteOutputDirectory(@TempDir Path tempDir) throws Exception {
        Path referenceDir = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/ccq_default_gun");
        assumeTrue(Files.isDirectory(referenceDir), "Reference stripped pack not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Path outputDir = taczDir.resolve(CcqDefaultGunPackContent.OUTPUT_DIR_NAME);
        Path sourceDir = taczDir.resolve("tacz_default_gun");
        copyDirectory(referenceDir, outputDir);
        copyDirectory(referenceDir, sourceDir);
        Files.createDirectories(outputDir.resolve("data/tacz/index/guns"));
        Files.writeString(outputDir.resolve("data/tacz/index/guns/stale.json"), "{}");
        Files.writeString(outputDir.resolve("LICENSE"), "stale");

        CcqDefaultGunPackPatch.applyIfPresent(taczDir);

        assertFalse(Files.exists(outputDir.resolve("data/tacz/index/guns/stale.json")));
        assertFalse(Files.exists(outputDir.resolve("LICENSE")));
        assertTrue(Files.isRegularFile(outputDir.resolve("NOTICE.txt")));
    }

    private static void copyDirectory(Path source, Path target) throws Exception {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }
}
