package dev.ayman.repopilot.ingestion;

import dev.ayman.repopilot.repository.GithubRepositoryClient;
import dev.ayman.repopilot.repository.IndexedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IndexingJobRunner {

    private final RepositoryIngestionService ingestionService;

    @Async(AsyncIndexingConfig.INDEXING_EXECUTOR)
    public void run(IndexedRepository repo, GithubRepositoryClient.RepositoryInfo info) {
        log.info("Background indexing started for {} at {}", repo.fullName(), info.commitSha());

        IndexedRepository result = ingestionService.execute(repo, info);

        log.info("Background indexing of {} finished {}", result.fullName(), result.getStatus());
    }
}
