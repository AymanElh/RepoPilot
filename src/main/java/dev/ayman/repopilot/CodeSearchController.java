package dev.ayman.repopilot;

import org.springframework.context.annotation.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Profile("dev")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/code-search")
public class CodeSearchController {

    private final CodeSearchService codeSearchService;


    @PostMapping("/seed")
    public Map<String, String> seed() {
        codeSearchService.indexSampleCode();
        return Map.of("message", "Sample Code indexed successfully");
    }

    @GetMapping
    public List<SearchResult> search(@RequestParam String q) {
        return codeSearchService.search(q)
                .stream()
                .map(doc -> new SearchResult(
                        doc.getText(),
                        doc.getMetadata(),
                        doc.getScore()
                ))
                .toList();
    }

    public record SearchResult (
            String content,
            Map<String, Object> metadata,
            Double score
    ) {}
}
