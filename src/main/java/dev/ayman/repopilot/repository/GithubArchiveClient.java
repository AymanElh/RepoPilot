package dev.ayman.repopilot.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class GithubArchiveClient {

    private final RestClient restClient;

    public GithubArchiveClient() {
        this("https://api.github.com");
    }

    GithubArchiveClient(String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build()))
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    public Path download(String owner, String repo, String sha) throws IOException {

        byte[] archive = restClient.get()
                .uri("/repos/{owner}/{repo}/zipball/{sha}", owner, repo, sha)
                .retrieve()
                .body(byte[].class);

        if (archive == null || archive.length == 0) {
            throw new IllegalArgumentException("Empty repository archive");
        }

        Path tempFile  = Files.createTempFile("repopilot", ".zip");

        try {
            Files.write(tempFile, archive);
            return tempFile;
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }

    }
}
