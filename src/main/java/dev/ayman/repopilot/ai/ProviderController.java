package dev.ayman.repopilot.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/providers")
public class ProviderController {

    private final ChatProvider defaultProvider;

    public ProviderController(
            @Value("${repopilot.ai.default-provider:GEMINI}") ChatProvider defaultProvider
    ) {
        this.defaultProvider = defaultProvider;
    }

    public record ProvidersResponse(ChatProvider defaultProvider, List<ChatProvider> providers) {}

    @GetMapping
    public ProvidersResponse getProviders() {
        return new ProvidersResponse(defaultProvider, List.of(ChatProvider.values()));
    }
}
