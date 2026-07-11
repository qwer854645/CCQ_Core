package ccq.core.tacz;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
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

    static void copyKeepPrefixesFromDirectory(Path sourceRoot, Path targetRoot, List<String> keepPrefixes) throws IOException {
        Set<String> pendingPrefixes = normalizeKeepPrefixes(keepPrefixes);
        Files.walkFileTree(sourceRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = normalizeEntryPath(sourceRoot.relativize(file).toString());
                if (!matchesKeepPrefix(relativePath, pendingPrefixes)) {
                    return FileVisitResult.CONTINUE;
                }

                writeRelativeBytes(targetRoot, relativePath, Files.readAllBytes(file));
                return FileVisitResult.CONTINUE;
            }
        });

        verifyCopiedPrefixes(targetRoot, pendingPrefixes);
    }

    static void copyKeepPrefixesFromPrefixedDirectory(
            Path sourceRoot,
            String entryPrefix,
            Path targetRoot,
            List<String> keepPrefixes
    ) throws IOException {
        String normalizedPrefix = normalizeEntryPath(entryPrefix);
        if (!normalizedPrefix.isEmpty() && !normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix + "/";
        }

        Set<String> pendingPrefixes = normalizeKeepPrefixes(keepPrefixes);
        Path searchRoot = normalizedPrefix.isEmpty() ? sourceRoot : sourceRoot.resolve(normalizedPrefix);
        if (!Files.isDirectory(searchRoot)) {
            throw new IOException("Missing gun pack directory: " + searchRoot);
        }

        Files.walkFileTree(searchRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = normalizeEntryPath(searchRoot.relativize(file).toString());
                if (!matchesKeepPrefix(relativePath, pendingPrefixes)) {
                    return FileVisitResult.CONTINUE;
                }

                writeRelativeBytes(targetRoot, relativePath, Files.readAllBytes(file));
                return FileVisitResult.CONTINUE;
            }
        });

        verifyCopiedPrefixes(targetRoot, pendingPrefixes);
    }

    static void copyKeepPrefixesFromZip(Path zipPath, Path targetRoot, List<String> keepPrefixes) throws IOException {
        copyKeepPrefixesFromPrefixedZip(zipPath, "", targetRoot, keepPrefixes);
    }

    static void copyKeepPrefixesFromPrefixedZip(
            Path zipPath,
            String entryPrefix,
            Path targetRoot,
            List<String> keepPrefixes
    ) throws IOException {
        Set<String> pendingPrefixes = normalizeKeepPrefixes(keepPrefixes);
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
                if (!matchesKeepPrefix(relativePath, pendingPrefixes)) {
                    continue;
                }

                writeRelativeBytes(targetRoot, relativePath, zipInput.readAllBytes());
            }
        }

        verifyCopiedPrefixes(targetRoot, pendingPrefixes);
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

    private static Set<String> normalizeKeepPrefixes(List<String> keepPrefixes) {
        Set<String> normalized = new HashSet<>();
        for (String keepPrefix : keepPrefixes) {
            String prefix = normalizeEntryPath(keepPrefix);
            if (!prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            normalized.add(prefix);
        }
        return normalized;
    }

    private static boolean matchesKeepPrefix(String relativePath, Set<String> keepPrefixes) {
        for (String keepPrefix : keepPrefixes) {
            if (relativePath.startsWith(keepPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static void verifyCopiedPrefixes(Path targetRoot, Set<String> keepPrefixes) throws IOException {
        List<String> missingPrefixes = new ArrayList<>();
        for (String prefix : keepPrefixes) {
            if (!hasFilesUnderPrefix(targetRoot, prefix)) {
                missingPrefixes.add(prefix);
            }
        }
        if (!missingPrefixes.isEmpty()) {
            throw new IOException("Gun pack is missing required prefix entries: " + String.join(", ", missingPrefixes));
        }
    }

    private static boolean hasFilesUnderPrefix(Path targetRoot, String prefix) throws IOException {
        Path prefixDir = targetRoot.resolve(prefix);
        if (!Files.isDirectory(prefixDir)) {
            return false;
        }

        try (var paths = Files.walk(prefixDir)) {
            return paths.anyMatch(Files::isRegularFile);
        }
    }
}
