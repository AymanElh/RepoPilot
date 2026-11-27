package dev.ayman.repopilot.repository;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
        UUID id,
        String owner,
        String fullName,
        String defaultBranch,
        String indexedCommitSha,
        RepositoryStatus status,
        String errorMessage,
        int javaFiles,
        int chunkCount,
        Instant createdAt,
        Instant updatedAt
) {
}
