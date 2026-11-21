package dev.ayman.repopilot.chat;

import dev.ayman.repopilot.ai.ChatProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/repositories")
@RequiredArgsConstructor
public class RepositoryChatController {

    private final RepositoryChatService chatService;

    @PostMapping("/chat")
    public ChatResponse chat(
            @Valid @RequestBody ChatRequest request
    ) {
        return chatService.ask(
                request.repositoryId(),
                request.question(),
                request.provider()
        );
    }

    public record ChatRequest (
            @NotBlank String repositoryId,
            @NotBlank String question,
            ChatProvider provider
    ) {}
}
