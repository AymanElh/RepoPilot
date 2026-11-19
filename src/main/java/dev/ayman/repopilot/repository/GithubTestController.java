package dev.ayman.repopilot.repository;

import dev.ayman.repopilot.ingestion.CodeChunk;
import dev.ayman.repopilot.ingestion.JavaCodeChunker;
import dev.ayman.repopilot.ingestion.RepositoryIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/github")
public class GithubTestController {

    private final GithubUrlValidator validator;
    private final GithubRepositoryClient client;
    private final GithubArchiveClient archiveClient;
    private final RepositorySourceExtractor extractor;
    private final JavaCodeChunker chunker;
    private final RepositoryIndexingService indexingService;

    @GetMapping("/info")
    public GithubRepositoryClient.RepositoryInfo info(
            @RequestParam String url
    ) {
        log.debug("url: {}", url);
        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid Github Url");
        }

        return client.fetchRepo(url);
    }

    @GetMapping("/download")
    public Map<String, String> download(@RequestParam String url) throws IOException {
        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid github url");
        }

        var info = client.fetchRepo(url);

        Path archive = archiveClient.download(
                info.owner(),
                info.name(),
                info.commitSha()
        );

        return Map.of(
                "file", archive.toString(),
                "commitSha", info.commitSha(),
                "sizeBytes", String.valueOf(Files.size(archive))
        );
    }

    @GetMapping("/chunks")
    public Map<String, Object> chunks(@RequestParam String url) throws IOException {
        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid github url");
        }

        var info = client.fetchRepo(url);

        Path zipFile = archiveClient.download(
                info.owner(),
                info.name(),
                info.commitSha()
        );

        Path directory = null;

        try {
            directory = extractor.extract(zipFile);

            List<Path> javaFiles = extractor.findJavaFiles(directory);
            List<CodeChunk> chunks = new ArrayList<>();

            for (Path file : javaFiles) {
                chunks.addAll(chunker.chunk(directory, file));
            }

            return Map.of(
                    "repository", info.name(),
                    "commitSha", info.commitSha(),
                    "totalJavaFiles", javaFiles.size(),
                    "totalChunks", chunks.size(),
                    "sampleChunks", chunks.stream().limit(3).toList()
            );
        } finally {
            try {
                if (directory != null) {
                    extractor.deleteDirectory(directory);
                }
            } finally {
                Files.deleteIfExists(zipFile);
            }
        }

    }


    @PostMapping("/index")
    public Map<String, Object> index(
            @RequestParam String url
    ) throws IOException {

        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid GitHub URL");
        }

        var info = client.fetchRepo(url);

        Path zipFile = archiveClient.download(
                info.owner(),
                info.name(),
                info.commitSha()
        );

        Path directory = null;

        try {
            directory = extractor.extract(zipFile);

            log.info("Directory: {}", directory);

            List<Path> javaFiles = extractor.findJavaFiles(directory);

            List<CodeChunk> chunks = new ArrayList<>();

            for (Path file : javaFiles) {
                chunks.addAll(chunker.chunk(directory, file));
            }

            String repositoryId = info.owner() + "/" + info.name();

            int indexed = indexingService.index(
                    repositoryId,
                    info.commitSha(),
                    chunks
            );

            return Map.of(
                    "repositoryId", repositoryId,
                    "commitSha", info.commitSha(),
                    "javaFiles", javaFiles.size(),
                    "indexedChunks", indexed
            );

        } finally {
            Files.deleteIfExists(zipFile);

            if (directory != null) {
                extractor.deleteDirectory(directory);
            }
        }
    }

}
