package dev.ayman.repopilot.ingestion;



import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class CodeChunkIdGenerator {

    public String generate(
            String repositoryId,
            String commitSha,
            CodeChunk chunk
    ) {
        String key = String.join(
                "|",
                repositoryId,
                commitSha,
                chunk.filePath(),
                String.valueOf(chunk.startLine()),
                String.valueOf(chunk.endLine())
        );

        return UUID.nameUUIDFromBytes(
                key.getBytes(StandardCharsets.UTF_8)
        ).toString();
    }
}
