package dev.ayman.repopilot;

public class RepositoryNotFoundException extends RuntimeException {
    public RepositoryNotFoundException(String fullName) {
        super("Repository not indexed: " + fullName);
    }
}
