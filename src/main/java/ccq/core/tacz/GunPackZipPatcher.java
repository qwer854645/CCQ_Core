package ccq.core.tacz;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
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
        String normalizedTarget = normalizeEntryPath(entryPath);
        Map<String, byte[]> replacements = new HashMap<>();
        replacements.put(normalizedTarget, content);
        boolean replaced = false;

        try {
            try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zipPath));
                 ZipOutputStream zipOutput = new ZipOutputStream(Files.newOutputStream(tempZip))) {
                ZipEntry entry;
                while ((entry = zipInput.getNextEntry()) != null) {
                    String normalizedName = normalizeEntryPath(entry.getName());
                    byte[] replacement = replacements.remove(normalizedName);
                    if (replacement != null) {
                        writeEntry(zipOutput, normalizedName, replacement);
                        replaced = true;
                        continue;
                    }

                    if (entry.isDirectory()) {
                        writeDirectoryEntry(zipOutput, normalizedName, entry.getTime());
                        continue;
                    }

                    copyEntry(zipInput, zipOutput, entry);
                }

                for (Map.Entry<String, byte[]> pending : replacements.entrySet()) {
                    writeEntry(zipOutput, pending.getKey(), pending.getValue());
                    replaced = true;
                }
            }

            if (!replaced) {
                throw new IOException("Zip entry not found for replacement: " + entryPath);
            }

            moveReplacing(tempZip, zipPath);
            tempZip = null;
        } finally {
            if (tempZip != null) {
                Files.deleteIfExists(tempZip);
            }
        }
    }

    private static void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            if (Files.exists(source)) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }

            throw exception;
        }
    }

    private static void copyEntry(InputStream input, ZipOutputStream output, ZipEntry entry) throws IOException {
        String normalizedName = normalizeEntryPath(entry.getName());
        byte[] data = input.readAllBytes();

        if (entry.getMethod() == ZipEntry.STORED) {
            writeStoredEntry(output, normalizedName, data, entry.getTime());
            return;
        }

        ZipEntry copied = new ZipEntry(normalizedName);
        copied.setMethod(ZipEntry.DEFLATED);
        copied.setTime(entry.getTime());
        output.putNextEntry(copied);
        output.write(data);
        output.closeEntry();
    }

    private static void writeEntry(ZipOutputStream output, String entryPath, byte[] content) throws IOException {
        writeDeflatedEntry(output, entryPath, content, System.currentTimeMillis());
    }

    private static void writeStoredEntry(ZipOutputStream output, String entryPath, byte[] content, long time) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);
        entry.setMethod(ZipEntry.STORED);
        entry.setTime(time);
        entry.setSize(content.length);
        entry.setCompressedSize(content.length);
        CRC32 crc = new CRC32();
        crc.update(content);
        entry.setCrc(crc.getValue());
        output.putNextEntry(entry);
        output.write(content);
        output.closeEntry();
    }

    private static void writeDeflatedEntry(ZipOutputStream output, String entryPath, byte[] content, long time) throws IOException {
        ZipEntry entry = new ZipEntry(entryPath);
        entry.setMethod(ZipEntry.DEFLATED);
        entry.setTime(time);
        output.putNextEntry(entry);
        output.write(content);
        output.closeEntry();
    }

    private static void writeDirectoryEntry(ZipOutputStream output, String entryPath, long time) throws IOException {
        String directoryName = entryPath.endsWith("/") ? entryPath : entryPath + "/";
        ZipEntry entry = new ZipEntry(directoryName);
        entry.setTime(time);
        output.putNextEntry(entry);
        output.closeEntry();
    }

    private static boolean matchesEntry(String actual, String expected) {
        return normalizeEntryPath(actual).equals(normalizeEntryPath(expected));
    }

    private static String normalizeEntryPath(String entryPath) {
        return entryPath.replace('\\', '/');
    }
}
