package dev.ayman.repopilot.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class GithubArchiveClient {

    private static final long MAX_ARCHIVE_BYTES = 100L * 1024 * 1024;

    private final RestClient restClient;

    public GithubArchiveClient() {
        this("https://api.github.com", "");
    }

    @Autowired
    public GithubArchiveClient(@Value("${repopilot.github.token:}") String token) {
        this("https://api.github.com", token);
    }

    GithubArchiveClient(String baseUrl, String token) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()))
                .defaultHeader("Accept", "application/vnd.github+json");

        if (token != null && !token.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        this.restClient = builder.build();
    }

    public Path download(String owner, String repo, String sha) throws IOException {
        Path tempFile = Files.createTempFile("repopilot", ".zip");

        try {
            restClient.get()
                    .uri("/repos/{owner}/{repo}/zipball/{sha}", owner, repo, sha)
                    .exchange((req, resp) -> {
                        if (resp.getStatusCode().isError()) {
                            throw new RestClientResponseException(
                                    "Github returned " + resp.getStatusCode(),
                                    resp.getStatusCode(),
                                    resp.getStatusText(),
                                    resp.getHeaders(),
                                    null, null
                            );
                        }
                        try (InputStream in = resp.getBody()) {
                            copyWithLimit(in, tempFile);
                        }
                        return null;
                    });

            if (Files.size(tempFile) == 0) {
                throw new IllegalArgumentException("Empty repository archive");
            }

            return tempFile;
        } catch (RuntimeException | IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }
    }

    private void copyWithLimit(InputStream in, Path target) throws IOException {
        try (var out = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_ARCHIVE_BYTES) {
                    throw new IllegalArgumentException("Repository archive is too large (limit 100 MB)");
                }
                out.write(buffer, 0, read);
            }
        }
    }
}
