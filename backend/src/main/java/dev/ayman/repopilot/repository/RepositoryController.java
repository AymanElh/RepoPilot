package dev.ayman.repopilot.repository;

import dev.ayman.repopilot.RepositoryNotFoundException;
import dev.ayman.repopilot.ingestion.IndexingJobRunner;
import dev.ayman.repopilot.ingestion.RepositoryIngestionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryIngestionService ingestionService;
    private final IndexingJobRunner jobRunner;
    private final IndexedRepositoryRepository repositories;

    public record IndexRequest(@NotBlank String url) {
    }

    @PostMapping
    public ResponseEntity<RepositoryResponse> index(@Valid @RequestBody IndexRequest request) {
        var prepared = ingestionService.prepare(request.url);

        if (!prepared.needsIndexing()) {
            return ResponseEntity.ok(toResponse(prepared.repository()));
        }

        try {
            jobRunner.run(prepared.repository(), prepared.info());
        } catch (TaskRejectedException e) {
            ingestionService.markFailed(prepared.repository(), "Indexing queue is full");
            throw e;
        }

        return ResponseEntity.accepted().body(toResponse(prepared.repository()));
    }

    @GetMapping("/{owner}/{name}")
    public RepositoryResponse get(@PathVariable String owner, @PathVariable String name) {
        return repositories.findByOwnerAndName(owner, name)
                .map(RepositoryController::toResponse)
                .orElseThrow(() -> new RepositoryNotFoundException(owner + "/" + name));
    }

    @GetMapping
    public List<RepositoryResponse> list() {
        return repositories.findAll().stream().map(RepositoryController::toResponse).toList();
    }

    private static RepositoryResponse toResponse(IndexedRepository repo) {
        return new RepositoryResponse(
                repo.getId(), repo.getOwner(), repo.fullName(), repo.getDefaultBranch(),
                repo.getIndexedCommitSha(), repo.getStatus(), repo.getErrorMessage(),
                repo.getJavaFiles(), repo.getChunkCount(), repo.getCreatedAt(), repo.getUpdatedAt());
    }
}
