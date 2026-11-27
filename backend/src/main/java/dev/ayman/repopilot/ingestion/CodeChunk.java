package dev.ayman.repopilot.ingestion;

public record CodeChunk (
        String filePath,
        int startLine,
        int endLine,
        String content
) {
}
