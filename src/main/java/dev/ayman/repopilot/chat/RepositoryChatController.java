package dev.ayman.repopilot.chat;

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
            @RequestBody ChatRequest request
    ) {
        return chatService.ask(
                request.repositoryId(),
                request.question()
        );
    }

    public record ChatRequest (
            String repositoryId,
            String question
    ) {}
}
