package dev.ayman.repopilot.chat;

public record SourceReference(
        String filePath,
        int startLine,
        int endLine,
        String githubUrl
) {
}
