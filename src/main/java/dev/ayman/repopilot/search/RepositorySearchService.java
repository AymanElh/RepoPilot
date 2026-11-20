package dev.ayman.repopilot.search;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RepositorySearchService {

    private final VectorStore vectorStore;

    public List<CodeSearchResult> search(String repositoryId, String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(5)
                .similarityThreshold(0.0)
                .filterExpression(
                        "repositoryId == '" + repositoryId + "'"
                )
                .build();

        return vectorStore.similaritySearch(request)
                .stream()
                .map(doc -> {
                    Map<String, Object> metadata = doc.getMetadata();

                    String commitSha = metadata.get("commitSha").toString();
                    String filePath = metadata.get("filePath").toString();

                    int startLine = ((Number) metadata.get("startLine")).intValue();
                    int endLine = ((Number) metadata.get("endLine")).intValue();

                    String cleanPath = filePath.contains("/") ? filePath.substring(filePath.indexOf('/') + 1) : filePath;

                    String githubUrl = "https://github.com/"
                            + repositoryId
                            + "/blob/"
                            + commitSha
                            + "/"
                            + cleanPath
                            + "#L" + startLine + "-L" + endLine;

                    return new CodeSearchResult(
                            cleanPath,
                            startLine,
                            endLine,
                            doc.getText(),
                            doc.getScore(),
                            githubUrl
                    );
                })
                .toList();
    }

}
