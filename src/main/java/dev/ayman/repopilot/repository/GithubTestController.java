package dev.ayman.repopilot.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dev/github")
public class GithubTestController {

    private final GithubUrlValidator validator;
    private final GithubRepositoryClient client;
    private final GithubArchiveClient archiveClient;

    @GetMapping("/info")
    public GithubRepositoryClient.RepositoryInfo info(
            @RequestParam String url
    ) {
        log.debug("url: {}", url);
        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid Github Url");
        }

        return client.fetchRepo(url);
    }

    @GetMapping("/download")
    public Map<String, String> download(@RequestParam String url) throws IOException {
        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid github url");
        }

        var info = client.fetchRepo(url);

        Path archive = archiveClient.download(
                info.owner(),
                info.name(),
                info.commitSha()
        );

        return Map.of(
                "file", archive.toString(),
                "commitSha", info.commitSha(),
                "sizeBytes", String.valueOf(Files.size(archive))
        );
    }
}
