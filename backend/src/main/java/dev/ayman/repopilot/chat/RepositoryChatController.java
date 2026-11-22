package dev.ayman.repopilot.chat;

import dev.ayman.repopilot.ai.ChatProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repositories/{owner}/{name}")
@RequiredArgsConstructor
public class RepositoryChatController {

    private final RepositoryChatService chatService;

    @PostMapping("/chat")
    public ChatResponse chat(
            @PathVariable String owner,
            @PathVariable String name,
            @Valid @RequestBody ChatRequest request
    ) {
        return chatService.ask(
                owner + "/" + name,
                request.question(),
                request.provider()
        );
    }

    public record ChatRequest (
            @NotBlank String question,
            ChatProvider provider
    ) {}
}
