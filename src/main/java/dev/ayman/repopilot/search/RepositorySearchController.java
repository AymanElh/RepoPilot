package dev.ayman.repopilot.search;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dev/repositories")
@RequiredArgsConstructor
public class RepositorySearchController {

    private final RepositorySearchService searchService;

    @GetMapping("/search")
    public List<CodeSearchResult> search(
            @RequestParam String repositoryId,
            @RequestParam String q
    ) {
        return searchService.search(repositoryId, q);
    }
}
