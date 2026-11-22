package dev.ayman.repopilot.ingestion;


import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RepositoryIndexingService {

    private final VectorStore vectorStore;
    private final CodeChunkIdGenerator idGenerator;

    public int index(
            String repositoryId,
            String commitSha,
            List<CodeChunk> chunks
    ) {
        List<Document> documents = chunks.stream()
                .map(chunk -> Document.builder()
                        .id(idGenerator.generate(
                                repositoryId,
                                commitSha,
                                chunk
                        ))
                        .text(chunk.content())
                        .metadata(Map.of(
                                "repositoryId", repositoryId,
                                "commitSha", commitSha,
                                "filePath", chunk.filePath(),
                                "startLine", chunk.startLine(),
                                "endLine", chunk.endLine()
                        ))
                        .build()
                )
                .toList();

        int batchSize = 10;

        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());

            vectorStore.add(documents.subList(i, end));
        }

        return documents.size();
    }
}
