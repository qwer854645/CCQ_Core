package ccq.core.tacz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class GunPackZipPatcher {
    private GunPackZipPatcher() {
    }

    static byte[] readEntry(Path zipPath, String entryPath) throws IOException {
        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (matchesEntry(entry.getName(), entryPath)) {
                    return zipInput.readAllBytes();
                }
            }
        }
        return null;
    }

    static void replaceEntry(Path zipPath, String entryPath, byte[] content) throws IOException {
        Path tempZip = Files.createTempFile(zipPath.getParent(), "ccq-gunpack-", ".zip");
        Map<String, byte[]> replacements = Map.of(normalizeEntryPath(entryPath), content);

        try {
            try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zipPath));
                 ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    String normalizedName = normalizeEntryPath(entry.getName());
                    byte[] replacement = replacements.remove(normalizedName);
                    if (replacement != null) {
                        writeEntry(zipOutput, normalizedName, replacement, entry.getMethod());
                    } else {
                        copyEntry(zipInput, zipOutput, entry);
                    }
                }

                for (Map.Entry<String, byte[]> pending : replacements.entrySet()) {
                    writeEntry(zipOutput, pending.getKey(), pending.getValue(), ZipEntry.DEFLATED);
                }
            }

            Files.move(tempZip, zipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    private static void copyEntry(InputStream input, ZipOutputStream output, ZipEntry entry) throws IOException {
        ZipEntry copied = new ZipEntry(normalizeEntryPath(entry.getName()));
        copied.setMethod(entry.getMethod());
        copied.setTime(entry.getTime());
        output.putNextEntry(copied);
        input.transferTo(output);
        output.closeEntry();
    }

    private static void writeEntry(ZipOutputStream output, String entryPath, byte[] content, int method) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);
        entry.setMethod(method);
        output.putNextEntry(entry);
        output.write(content);
        output.closeEntry();
    }

    private static boolean matchesEntry(String actual, String expected) {
        return normalizeEntryPath(actual).equals(normalizeEntryPath(expected));
    }

    private static String normalizeEntryPath(String entryPath) {
        return entryPath.replace('\\', '/');
    }
}
