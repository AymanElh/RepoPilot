package dev.ayman.repopilot;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BackendApplicationTests {

    @Test
    void contextLoadsWithChatAndEmbeddingModels(@Autowired ChatModel chatModel,
            @Autowired EmbeddingModel embeddingModel) {
        assertNotNull(chatModel);
        assertNotNull(embeddingModel);
    }
}
