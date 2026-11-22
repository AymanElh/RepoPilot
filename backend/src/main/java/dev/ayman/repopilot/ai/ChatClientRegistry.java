package dev.ayman.repopilot.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatClientRegistry {

    private final Map<ChatProvider, ChatClient> clients;
    private final ChatProvider defaultChatProvider;

    public ChatClientRegistry(
            GoogleGenAiChatModel gemini,
            OllamaChatModel ollama,
            @Value("${repopilot.ai.default-provider:GEMINI}")
            ChatProvider defaultChatProvider
    ) {
        this.clients = Map.of(
                ChatProvider.GEMINI, ChatClient.create(gemini),
                ChatProvider.OLLAMA, ChatClient.create(ollama)
        );
        this.defaultChatProvider = defaultChatProvider;
    }

    public ChatClient get(ChatProvider requestChatProvider) {
        ChatProvider provider = requestChatProvider == null ? defaultChatProvider : requestChatProvider;

        ChatClient client = clients.get(provider);

        if (client == null) {
            throw new UnsupportedChatProviderException(provider);
        }

        return client;
    }
}
