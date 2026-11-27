package dev.ayman.repopilot.chat;

import java.util.List;

public record ChatResponse(
        String answer,
        List<SourceReference> sources
) {
}
