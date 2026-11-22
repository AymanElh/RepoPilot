package dev.ayman.repopilot.repository;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class RepositorySourceExtractor {

    public Path extract(Path zipFile) throws IOException {
        Path destination = Files.createTempDirectory("repopilot-src-");

        int fileCount = 0;
        long totalBytes = 0;

        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination
                        .resolve(entry.getName())
                        .normalize();

                if (!target.startsWith(destination)) {
                    throw new IOException("Unsave Zip entry");
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    if (++fileCount > 10_000) {
                        throw new IOException("Too many files");
                    }

                    Files.createDirectories(target.getParent());

                    try (var output = Files.newOutputStream(target)) {
                        byte[] buffer = new byte[8192];

                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            totalBytes += n;

                            if (totalBytes > 100L * 1024 * 1024) {
                                throw new IOException("Archive too large");
                            }

                            output.write(buffer, 0, n);
                        }
                    }
                }
                zip.closeEntry();
            }
            return destination;
        } catch (IOException e) {
            deleteDirectory(destination);
            throw e;
        }
    }

    public List<Path> findJavaFiles(Path directory)
            throws IOException {

        try (var paths = Files.walk(directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.toString().contains("/target/"))
                    .filter(path -> !path.toString().contains("/build/"))
                    .sorted()
                    .toList();
        }
    }


    public void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (var paths = Files.walk(directory)) {
            for (Path path : paths
                    .sorted(Comparator.reverseOrder())
                    .toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
