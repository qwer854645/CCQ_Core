package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GunPackZipPatcherTest {
    private static final Gson GSON = new Gson();
    private static final String ENTRY = "data/applied_armorer/data/blocks/worckbench_applied_armorer_data.json";

    @Test
    void replaceStoredEntryKeepsZipReadable(@TempDir Path tempDir) throws Exception {
        Path sourceZip = Path.of("M:/others/old/gunpack/Applied Armorer-v1.1.4.1-for114+.zip.bak");
        assumeTrue(Files.isRegularFile(sourceZip), "Source zip not available on this machine");

        Path workingZip = tempDir.resolve("applied_armorer.zip");
        Files.copy(sourceZip, workingZip, StandardCopyOption.REPLACE_EXISTING);

        byte[] patched = AppliedArmorerWorkbenchData.createPatchedContent();
        GunPackZipPatcher.replaceEntry(workingZip, ENTRY, patched);

        try (ZipFile zipFile = new ZipFile(workingZip.toFile())) {
            ZipEntry entry = zipFile.getEntry(ENTRY);
            assertNotNull(entry, "Patched entry missing from zip");

            JsonObject json = GSON.fromJson(new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            assertEquals(13, json.getAsJsonArray("tabs").size());
            assertEquals(zipFile.size() > 0, true);
        }
    }

    @Test
    void replaceEntryPreservesZipEntryCount(@TempDir Path tempDir) throws Exception {
        Path sourceZip = Path.of("M:/others/old/gunpack/Applied Armorer-v1.1.4.1-for114+.zip.bak");
        assumeTrue(Files.isRegularFile(sourceZip), "Source zip not available on this machine");

        Path workingZip = tempDir.resolve("applied_armorer.zip");
        Files.copy(sourceZip, workingZip, StandardCopyOption.REPLACE_EXISTING);

        int entryCount;
        try (ZipFile zipFile = new ZipFile(workingZip.toFile())) {
            entryCount = zipFile.size();
        }

        GunPackZipPatcher.replaceEntry(workingZip, ENTRY, AppliedArmorerWorkbenchData.createPatchedContent());

        try (ZipFile zipFile = new ZipFile(workingZip.toFile())) {
            assertEquals(entryCount, zipFile.size());

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    assertNotNull(zipFile.getInputStream(entry).readAllBytes());
                }
            }
        }
    }
}
