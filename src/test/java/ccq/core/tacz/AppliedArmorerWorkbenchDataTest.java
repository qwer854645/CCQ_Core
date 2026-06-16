package ccq.core.tacz;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AppliedArmorerWorkbenchDataTest {
    private static final Gson GSON = new Gson();
    private static final String ENTRY = "data/applied_armorer/data/blocks/worckbench_applied_armorer_data.json";

    @Test
    void generatedJsonMatchesReferenceStructure() throws Exception {
        Path referenceZip = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/Applied Armorer-v1.1.4.1-for114+-fixed.zip");
        assumeTrue(Files.isRegularFile(referenceZip), "Reference zip not available on this machine");

        JsonObject reference;
        try (ZipFile zipFile = new ZipFile(referenceZip.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(ENTRY);
            reference = GSON.fromJson(new String(zipFile.getInputStream(zipEntry).readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
        }

        JsonObject generated = GSON.fromJson(new String(AppliedArmorerWorkbenchData.createPatchedContent(), StandardCharsets.UTF_8), JsonObject.class);

        assertEquals(reference.get("filter").getAsString(), generated.get("filter").getAsString());
        assertEquals(reference.getAsJsonArray("tabs").size(), generated.getAsJsonArray("tabs").size());
        for (int i = 0; i < reference.getAsJsonArray("tabs").size(); i++) {
            assertEquals(reference.getAsJsonArray("tabs").get(i), generated.getAsJsonArray("tabs").get(i));
        }
    }

    @Test
    void generatedJsonHasTabsKey() {
        JsonObject generated = GSON.fromJson(new String(AppliedArmorerWorkbenchData.createPatchedContent(), StandardCharsets.UTF_8), JsonObject.class);
        assertNotNull(generated.getAsJsonArray("tabs"));
        assertEquals(13, generated.getAsJsonArray("tabs").size());
    }

    @Test
    void generatedJsonUsesCrlfLineEndings() {
        byte[] generated = AppliedArmorerWorkbenchData.createPatchedContent();
        assertTrue(generated.length > 0);
        assertFalse(containsStandaloneLf(generated));
        assertTrue(containsCrlf(generated));
    }

    @Test
    void generatedJsonMatchesReferenceBytes() throws Exception {
        Path referenceZip = Path.of("M:/MC/createa/.minecraft/versions/Create_Craft&Quiet/tacz/Applied Armorer-v1.1.4.1-for114+-fixed.zip");
        assumeTrue(Files.isRegularFile(referenceZip), "Reference zip not available on this machine");

        byte[] reference;
        try (ZipFile zipFile = new ZipFile(referenceZip.toFile())) {
            ZipEntry zipEntry = zipFile.getEntry(ENTRY);
            reference = zipFile.getInputStream(zipEntry).readAllBytes();
        }

        assertArrayEquals(reference, AppliedArmorerWorkbenchData.createPatchedContent());
    }

    @Test
    void rejectsIncorrectTabsContent() {
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

        assertFalse(AppliedArmorerWorkbenchData.matchesExpectedContent(wrongTabs));
        assertTrue(AppliedArmorerWorkbenchData.matchesExpectedContent(AppliedArmorerWorkbenchData.createPatchedContent()));
    }

    private static boolean containsStandaloneLf(byte[] content) {
        for (int index = 0; index < content.length; index++) {
            if (content[index] == '\n' && (index == 0 || content[index - 1] != '\r')) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCrlf(byte[] content) {
        for (int index = 1; index < content.length; index++) {
            if (content[index - 1] == '\r' && content[index] == '\n') {
                return true;
            }
        }
        return false;
    }
}
