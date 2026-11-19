package dev.ayman.repopilot.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JavaCodeChunker {

    private static final int CHUNK_SIZE = 100;
    private static final int OVERLAP = 20;

    public List<CodeChunk> chunk(Path root, Path file) throws IOException {
        Path safeRoot = root.toRealPath();
        Path safeFile = file.toRealPath();

        if (!safeFile.startsWith(safeRoot) || !safeFile.toString().endsWith(".java")) {
            throw new IllegalArgumentException("File must be a java file inside repo");
        }

        List<String> lines = Files.readAllLines(safeFile, StandardCharsets.UTF_8);

        List<CodeChunk> chunks = new ArrayList<>();

        String relativePath = safeRoot.relativize(safeFile).toString().replace('\\', '/');

        int step = CHUNK_SIZE - OVERLAP;

        for (int start = 0; start < lines.size(); start += step) {
            int end = Math.min(start + CHUNK_SIZE, lines.size());

            String content = String.join("\n", lines.subList(start, end));

            if (!content.isBlank()) {
                chunks.add(new CodeChunk(
                        relativePath,
                        start + 1,
                        end,
                        content
                ));
            }

            if (end == lines.size()) break;



        }

//        log.info("chunks: {}", chunks);

        return chunks;
    }
}
