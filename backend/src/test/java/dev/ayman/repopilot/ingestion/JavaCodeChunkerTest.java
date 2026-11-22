package dev.ayman.repopilot.ingestion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class JavaCodeChunkerTest {

    @TempDir
    Path root;

    private final JavaCodeChunker chunker = new JavaCodeChunker();

    @Test
    void shouldSplitFileAndPreserveLineNumbers() throws Exception {

        Path file = root.resolve("Example.java");

        List<String> lines = IntStream.rangeClosed(1, 250)
                .mapToObj(i -> "// Line " + i)
                .toList();

        Files.write(file, lines);

        List<CodeChunk> chunks = chunker.chunk(root, file);

        assertEquals(3, chunks.size());

        assertEquals(1, chunks.get(0).startLine());
        assertEquals(100, chunks.get(0).endLine());

        assertEquals(81, chunks.get(1).startLine());
        assertEquals(180, chunks.get(1).endLine());

        assertEquals(161, chunks.get(2).startLine());
        assertEquals(250, chunks.get(2).endLine());

        assertEquals("Example.java", chunks.get(0).filePath());
        assertTrue(chunks.get(2).content().contains("// Line 250"));
    }
}
