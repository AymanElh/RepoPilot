package dev.ayman.repopilot;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiEmbeddingConnectionAutoConfiguration;
import org.springframework.ai.model.google.genai.autoconfigure.embedding.GoogleGenAiTextEmbeddingAutoConfiguration;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoogleGenAiConfigurationTest {

    @Test
    void configuresChatAndEmbeddingModelsWithoutExternalServices() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("GOOGLE_API_KEY=test-key", "DB_PASSWORD=unused")
                .withBean(ToolCallingManager.class, () -> DefaultToolCallingManager.builder().build())
                .withConfiguration(AutoConfigurations.of(
                        GoogleGenAiChatAutoConfiguration.class,
                        GoogleGenAiEmbeddingConnectionAutoConfiguration.class,
                        GoogleGenAiTextEmbeddingAutoConfiguration.class))
                .run(context -> {
                    assertNotNull(context.getBean(ChatModel.class));
                    assertNotNull(context.getBean(EmbeddingModel.class));
                });
    }
}
