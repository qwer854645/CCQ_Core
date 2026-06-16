package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AppliedArmorerPatchTest {
    private static final Gson GSON = new Gson();
    private static final String ENTRY = AppliedArmorerPatch.WORKBENCH_DATA_ENTRY;

    @Test
    void patchesEveryUnpatchedZip(@TempDir Path tempDir) throws Exception {
        Path sourceZip = Path.of("M:/others/old/gunpack/Applied Armorer-v1.1.4.1-for114+.zip.bak");
        Path fixedZip = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/Applied Armorer-v1.1.4.1-for114+-fixed.zip");
        assumeTrue(Files.isRegularFile(sourceZip), "Source zip not available on this machine");
        assumeTrue(Files.isRegularFile(fixedZip), "Fixed zip not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Files.createDirectories(taczDir);

        Path originalCopy = taczDir.resolve("Applied Armorer-v1.1.4.1-for114+.zip");
        Path fixedCopy = taczDir.resolve("Applied Armorer-v1.1.4.1-for114+-fixed.zip");
        Files.copy(sourceZip, originalCopy, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(fixedZip, fixedCopy, StandardCopyOption.REPLACE_EXISTING);

        AppliedArmorerPatch.applyIfPresent(taczDir);

        assertTabsCount(originalCopy, 13);
        assertTabsCount(fixedCopy, 13);
    }

    @Test
    void patchesExtractedDirectoryPack(@TempDir Path tempDir) throws Exception {
        Path sourceZip = Path.of("M:/others/old/gunpack/Applied Armorer-v1.1.4.1-for114+.zip.bak");
        assumeTrue(Files.isRegularFile(sourceZip), "Source zip not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Path packDir = taczDir.resolve("Applied Armorer");
        Files.createDirectories(packDir);

        try (ZipFile zipFile = new ZipFile(sourceZip.toFile())) {
            copyZipEntry(zipFile, AppliedArmorerPatch.PACK_META_ENTRY, packDir.resolve(AppliedArmorerPatch.PACK_META_ENTRY));
            copyZipEntry(zipFile, ENTRY, packDir.resolve(ENTRY));
        }

        AppliedArmorerPatch.applyIfPresent(taczDir);

        JsonObject json = GSON.fromJson(
                Files.readString(packDir.resolve(ENTRY), StandardCharsets.UTF_8),
                JsonObject.class
        );
        assertEquals(13, json.getAsJsonArray("tabs").size());
    }

    @Test
    void repatchesIncorrectTabs(@TempDir Path tempDir) throws Exception {
        Path sourceZip = Path.of("M:/others/old/gunpack/Applied Armorer-v1.1.4.1-for114+.zip.bak");
        assumeTrue(Files.isRegularFile(sourceZip), "Source zip not available on this machine");

        Path taczDir = tempDir.resolve("tacz");
        Files.createDirectories(taczDir);

        Path packZip = taczDir.resolve("Applied Armorer-v1.1.4.1-for114+.zip");
        Files.copy(sourceZip, packZip, StandardCopyOption.REPLACE_EXISTING);

        byte[] wrongTabs = """
                {
                  "filter": "applied_armorer:default",
                  "tabs": [
                    {
                      "id": "tacz:pistol",
                      "name": "tacz.type.pistol.name",
                      "icon": {
                        "item": "tacz:modern_kinetic_gun",
                        "nbt": {
                          "GunId": "applied_armorer:wrong_gun"
                        }
                      }
                    }
                  ]
                }
                """.replace("\n", "\r\n").getBytes(StandardCharsets.UTF_8);
        GunPackZipPatcher.replaceEntry(packZip, ENTRY, wrongTabs);

        AppliedArmorerPatch.applyIfPresent(taczDir);

        assertTabsMatchExpected(packZip);
    }

    private static void assertTabsMatchExpected(Path zipPath) throws Exception {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(ENTRY);
            assertNotNull(entry, "Workbench data missing from " + zipPath.getFileName());

            byte[] content = zipFile.getInputStream(entry).readAllBytes();
            assertTrue(AppliedArmorerWorkbenchData.matchesExpectedContent(content));
        }
    }

    private static void assertTabsCount(Path zipPath, int expectedTabs) throws Exception {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(ENTRY);
            assertNotNull(entry, "Workbench data missing from " + zipPath.getFileName());

            JsonObject json = GSON.fromJson(
                    new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8),
                    JsonObject.class
            );
            assertEquals(expectedTabs, json.getAsJsonArray("tabs").size());
        }
    }

    private static void copyZipEntry(ZipFile zipFile, String entryName, Path target) throws Exception {
        ZipEntry entry = zipFile.getEntry(entryName);
        assertNotNull(entry, "Missing zip entry: " + entryName);
        Files.createDirectories(target.getParent());
        Files.copy(zipFile.getInputStream(entry), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
