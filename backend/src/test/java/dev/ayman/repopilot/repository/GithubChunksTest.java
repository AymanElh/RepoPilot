package dev.ayman.repopilot.repository;

import dev.ayman.repopilot.ingestion.JavaCodeChunker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GithubChunksTest {
    @TempDir
    Path temp;

    private Path archive() throws IOException {
        Path archive = temp.resolve("repo.zip");
        try (var zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            zip.putNextEntry(new ZipEntry("repo/"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("repo/Example.java"));
            zip.write("class Example {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return archive;
    }

    private GithubTestController controller(Path archive, RepositorySourceExtractor extractor) {
        return new GithubTestController(new GithubUrlValidator(), new GithubRepositoryClient() {
            @Override
            public RepositoryInfo fetchRepo(String url) {
                return new RepositoryInfo("owner", "repo", "main", "commit");
            }
        }, new GithubArchiveClient() {
            @Override
            public Path download(String owner, String repo, String sha) {
                return archive;
            }
        }, extractor, new JavaCodeChunker(), null);
    }

    @Test
    void extractsFilesAsFilesAndDirectoriesAsDirectories() throws Exception {
        var extractor = new RepositorySourceExtractor();
        Path directory = extractor.extract(archive());
        try {
            assertTrue(Files.isDirectory(directory.resolve("repo")));
            assertEquals("class Example {}", Files.readString(directory.resolve("repo/Example.java")));
        } finally {
            extractor.deleteDirectory(directory);
        }
    }

    @Test
    void returnsChunksAndCleansTemporaryFiles() throws Exception {
        Path archive = archive();
        Path[] extracted = new Path[1];
        var extractor = new RepositorySourceExtractor() {
            @Override
            public Path extract(Path zipFile) throws IOException {
                extracted[0] = super.extract(zipFile);
                return extracted[0];
            }
        };
        var result = controller(archive, extractor).chunks("https://github.com/owner/repo");
        assertEquals(1, result.get("totalJavaFiles"));
        assertEquals(1, result.get("totalChunks"));
        assertFalse(Files.exists(archive));
        assertFalse(Files.exists(extracted[0]));
    }

    @Test
    void preservesExtractionErrorAndDeletesArchive() throws Exception {
        Path archive = archive();
        var failure = new IOException("Extraction failed");
        var extractor = new RepositorySourceExtractor() {
            @Override
            public Path extract(Path zipFile) throws IOException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(IOException.class,
                () -> controller(archive, extractor).chunks("https://github.com/owner/repo")));
        assertFalse(Files.exists(archive));
    }
}
