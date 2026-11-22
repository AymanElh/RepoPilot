package dev.ayman.repopilot.ai;

import org.springframework.context.annotation.Profile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Profile("dev")
@RestController
public class AITestController {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    public AITestController(ChatClientRegistry registry, EmbeddingModel embeddingModel) throws IllegalAccessException {
        this.chatClient = registry.get(null);
        this.embeddingModel = embeddingModel;
    }

    @GetMapping("/api/test/chat")
    public Map<String, String> testChat() {
        String response = chatClient.prompt()
                .user("Explain what Spring Boot is in one sentence.")
                .call()
                .content();

        return Map.of("response", response);
    }

    @GetMapping("/api/test/embedding")
    public Map<String, Object> testEmbedding() {

        String code = """
                public boolean validateToken(String token) {
                    return jwtService.verify(token);
                }
                """;

        float[] vector = embeddingModel.embed(code);

        return Map.of(
                "model", "gemini-embedding-001",
                "dimensions", vector.length,
                "firstFiveValues", java.util.Arrays.copyOf(vector, 5)
        );
    }
}
