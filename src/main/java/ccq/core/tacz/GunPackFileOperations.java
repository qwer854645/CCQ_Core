package ccq.core.tacz;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class GunPackFileOperations {
    private GunPackFileOperations() {
    }

    static void copyRelativeFile(Path sourceRoot, Path targetRoot, String relativePath) throws IOException {
        Path source = sourceRoot.resolve(relativePath);
        if (!Files.isRegularFile(source)) {
            throw new IOException("Missing gun pack file: " + relativePath);
        }

        Path target = targetRoot.resolve(relativePath);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    static void extractRelativeFile(Path zipPath, Path targetRoot, String relativePath) throws IOException {
        byte[] content = GunPackZipPatcher.readEntry(zipPath, relativePath);
        if (content == null) {
            throw new IOException("Missing zip entry: " + relativePath);
        }

        writeRelativeBytes(targetRoot, relativePath, content);
    }

    static void writeRelativeBytes(Path targetRoot, String relativePath, byte[] content) throws IOException {
        Path target = targetRoot.resolve(relativePath).normalize();
        if (!target.startsWith(targetRoot.normalize())) {
            throw new IOException("Blocked path outside gun pack directory: " + relativePath);
        }

        Files.createDirectories(target.getParent());
        Files.write(target, content);
    }

    static void copyKeepListFromDirectory(Path sourceRoot, Path targetRoot, List<String> keepFiles) throws IOException {
        for (String relativePath : keepFiles) {
            copyRelativeFile(sourceRoot, targetRoot, relativePath);
        }
    }

    static void copyKeepListFromPrefixedZip(Path zipPath, String entryPrefix, Path targetRoot, List<String> keepFiles) throws IOException {
        Set<String> pending = keepFiles.stream()
                .map(GunPackFileOperations::normalizeEntryPath)
                .collect(Collectors.toSet());

        String normalizedPrefix = normalizeEntryPath(entryPrefix);
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix + "/";
        }

        try (ZipInputStream zipInput = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInput.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String normalizedName = normalizeEntryPath(entry.getName());
                if (!normalizedPrefix.isEmpty() && !normalizedName.startsWith(normalizedPrefix)) {
                    continue;
                }

                String relativePath = normalizedPrefix.isEmpty()
                        ? normalizedName
                        : normalizedName.substring(normalizedPrefix.length());
                if (!pending.remove(relativePath)) {
                    continue;
                }

                writeRelativeBytes(targetRoot, relativePath, zipInput.readAllBytes());
            }
        }

        if (!pending.isEmpty()) {
            throw new IOException("Zip is missing required entries: " + String.join(", ", pending));
        }
    }

    static void copyKeepListFromZip(Path zipPath, Path targetRoot, List<String> keepFiles) throws IOException {
        copyKeepListFromPrefixedZip(zipPath, "", targetRoot, keepFiles);
    }

    static void copyKeepListFromPrefixedDirectory(Path sourceRoot, String entryPrefix, Path targetRoot, List<String> keepFiles) throws IOException {
        String normalizedPrefix = normalizeEntryPath(entryPrefix);
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix + "/";
        }

        for (String relativePath : keepFiles) {
            String sourceRelative = normalizedPrefix.isEmpty() ? relativePath : normalizedPrefix + relativePath;
            Path source = sourceRoot.resolve(sourceRelative);
            if (!Files.isRegularFile(source)) {
                throw new IOException("Missing gun pack file: " + sourceRelative);
            }

            writeRelativeBytes(targetRoot, relativePath, Files.readAllBytes(source));
        }
    }

    static void resetDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            deletePack(directory);
        }
        Files.createDirectories(directory);
    }

    static void publishDirectory(Path stagingDir, Path targetDir) throws IOException {
        if (Files.exists(targetDir)) {
            deletePack(targetDir);
        }
        Files.move(stagingDir, targetDir);
    }

    static void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            // Best-effort cache cleanup.
        }
    }

    static void deletePack(Path packPath) throws IOException {
        if (Files.isDirectory(packPath)) {
            Files.walkFileTree(packPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            return;
        }

        Files.deleteIfExists(packPath);
    }

    static byte[] readPackMeta(Path packPath) throws IOException {
        if (Files.isRegularFile(packPath)) {
            return GunPackZipPatcher.readEntry(packPath, CcqDefaultGunPackContent.PACK_META_ENTRY);
        }

        Path meta = packPath.resolve(CcqDefaultGunPackContent.PACK_META_ENTRY);
        if (Files.isRegularFile(meta)) {
            return Files.readAllBytes(meta);
        }

        return null;
    }

    static boolean containsTaczNamespace(byte[] metaBytes) {
        String meta = new String(metaBytes, java.nio.charset.StandardCharsets.UTF_8);
        return meta.contains("\"namespace\": \"tacz\"")
                || meta.contains("\"namespace\":\"tacz\"");
    }

    private static String normalizeEntryPath(String entryPath) {
        return entryPath.replace('\\', '/');
    }
}
