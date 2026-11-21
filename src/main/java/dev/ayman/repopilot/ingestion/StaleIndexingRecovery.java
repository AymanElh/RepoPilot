package dev.ayman.repopilot.ingestion;

import dev.ayman.repopilot.repository.IndexedRepository;
import dev.ayman.repopilot.repository.IndexedRepositoryRepository;
import dev.ayman.repopilot.repository.RepositoryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleIndexingRecovery implements ApplicationRunner {

    private final IndexedRepositoryRepository repositories;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        repositories.findAll().stream()
                .filter(repo -> repo.getStatus() == RepositoryStatus.INDEXING)
                .forEach(repo -> {
                    log.warn("{} was indexing at startup, marking failed", repo.fullName());
                    repo.setStatus(RepositoryStatus.FAILED);
                    repo.setErrorMessage("Indexing was interrupted by an application restart");
                    repositories.save(repo);
                });
    }
}
