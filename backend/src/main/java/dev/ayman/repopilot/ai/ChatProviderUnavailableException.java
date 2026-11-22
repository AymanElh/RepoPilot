package dev.ayman.repopilot.ai;

public class ChatProviderUnavailableException extends RuntimeException {
    public ChatProviderUnavailableException(ChatProvider provider, Throwable cause) {
        super((provider == null ? "The AI provider" : provider.name())
                + " is unreachable. Check that it is running or choose another provider.", cause);
    }
}
