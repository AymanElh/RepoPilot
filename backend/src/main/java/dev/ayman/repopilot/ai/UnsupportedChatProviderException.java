package dev.ayman.repopilot.ai;

public class UnsupportedChatProviderException extends RuntimeException {
    public UnsupportedChatProviderException(ChatProvider provider) {
        super("Unsupported chat Provider: " + provider);
    }
}
