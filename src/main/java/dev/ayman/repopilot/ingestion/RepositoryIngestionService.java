package dev.ayman.repopilot.ingestion;

import dev.ayman.repopilot.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryIngestionService {

    private final GithubUrlValidator urlValidator;
    private final GithubRepositoryClient repositoryClient;
    private final GithubArchiveClient githubArchiveClient;
    private final RepositorySourceExtractor extractor;
    private final JavaCodeChunker javaCodeChunker;
    private final RepositoryIndexingService indexingService;
    private final IndexedRepositoryRepository indexedRepositoryRepository;
    private final VectorStore vectorStore;


    public IndexedRepository ingest(String url) {
        if (!urlValidator.isValid(url)) {
            throw new IllegalArgumentException("Invalid GitHub URL");
        }

        var info = repositoryClient.fetchRepo(url);

        IndexedRepository repo = indexedRepositoryRepository
                .findByOwnerAndName(info.owner(), info.name())
                .orElseGet(() -> newRepository(info));

        if (repo.getStatus() == RepositoryStatus.INDEXING) {
            throw new IllegalStateException("Repository is already being indexed");
        }

        if (repo.getStatus() == RepositoryStatus.READY
                && info.commitSha().equals(repo.getIndexedCommitSha())) {
            log.info("{} already indexed at {}, skipping", repo.fullName(), info.commitSha());
            return repo;
        }

        repo.setDefaultBranch(info.defaultBranch());
        repo.setStatus(RepositoryStatus.INDEXING);
        repo.setErrorMessage(null);
        repo = indexedRepositoryRepository.save(repo);

        // 6. Do the real work. On failure keep the OLD chunks and record FAILED.
        try {
            return runIndexing(repo, info);
        } catch (Exception e) {
            log.error("Indexing {} failed", repo.fullName(), e);
            repo.setStatus(RepositoryStatus.FAILED);
            repo.setErrorMessage(truncate(e.getMessage()));
            return indexedRepositoryRepository.save(repo);
        }
    }

    private IndexedRepository runIndexing(
            IndexedRepository repo,
            GithubRepositoryClient.RepositoryInfo info
    ) throws IOException {
        Path zipFile = githubArchiveClient.download(info.owner(), info.name(), info.commitSha());
        Path directory = null;

        try {
            directory = extractor.extract(zipFile);

            List<Path> javaFiles = extractor.findJavaFiles(directory);
            List<CodeChunk> chunks = new ArrayList<>();
            for (Path file : javaFiles) {
                chunks.addAll(javaCodeChunker.chunk(directory, file));
            }

            // Add the new chunks first...
            int indexed = indexingService.index(repo.fullName(), info.commitSha(), chunks);

            // ...and only now that they exist, remove the chunks of older commits
            deleteOutdatedChunks(repo.fullName(), info.commitSha());

            repo.setIndexedCommitSha(info.commitSha());
            repo.setJavaFiles(javaFiles.size());
            repo.setChunkCount(indexed);
            repo.setStatus(RepositoryStatus.READY);
            repo.setErrorMessage(null);
            return indexedRepositoryRepository.save(repo);
        } finally {
            cleanup(zipFile, directory);
        }
    }

    private void deleteOutdatedChunks(String repositoryId, String currentSha) {
        var b = new FilterExpressionBuilder();
        vectorStore.delete(
                b.and(
                        b.eq("repositoryId", repositoryId),
                        b.ne("commitSha", currentSha)
                ).build()
        );
    }

    private IndexedRepository newRepository(GithubRepositoryClient.RepositoryInfo info) {
        IndexedRepository repo = new IndexedRepository();
        repo.setOwner(info.owner());
        repo.setName(info.name());
        repo.setDefaultBranch(info.defaultBranch());
        repo.setStatus(RepositoryStatus.PENDING);
        return repo;
    }

    private void cleanup(Path zipFile, Path directory) {
        try {
            if (directory != null) {
                extractor.deleteDirectory(directory);
            }
        } catch (IOException e) {
            log.warn("Could not delete {}", directory, e);
        } finally {
            try {
                Files.deleteIfExists(zipFile);
            } catch (IOException e) {
                log.warn("Could not delete {}", zipFile, e);
            }
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
