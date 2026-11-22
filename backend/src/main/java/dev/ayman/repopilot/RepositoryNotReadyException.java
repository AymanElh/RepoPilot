package dev.ayman.repopilot;

import dev.ayman.repopilot.repository.RepositoryStatus;

public class RepositoryNotReadyException extends RuntimeException {
    private final RepositoryStatus status;

    public RepositoryNotReadyException(String fullName, RepositoryStatus status) {
        super("Repository " + fullName + " is not ready for chat (status: " + status + ")");
        this.status = status;
    }

    public RepositoryStatus getStatus() {
        return status;
    }
}
