package dev.ayman.repopilot.search;

public record CodeSearchResult(
        String filePath,
        int startLine,
        int endLine,
        String content,
        Double store,
        String githubUrl
) {
}
