package dev.ayman.repopilot.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;

@Component
public class GithubRepositoryClient {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();


    public RepositoryInfo fetchRepo(String url) {
        URI uri = URI.create(url);

        String[] parts = uri.getPath().substring(1).split("/");

        String owner = parts[0];
        String repo = parts[1];

        GithubRepoResponse repository = restClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .retrieve()
                .body(GithubRepoResponse.class);

        if (repository == null) {
            throw new  IllegalArgumentException("Repository not found");
        }

        GithubCommitResponse commit = restClient.get()
                .uri("/repos/{owner}/{repo}/commits/{branch}", owner, repo, repository.default_branch())
                .retrieve()
                .body(GithubCommitResponse.class);

        if (commit == null)  {
            throw new IllegalArgumentException("Empty commit response");
        }

        return new RepositoryInfo(owner, repo, repository.default_branch(), commit.sha());
    }


    public record RepositoryInfo (
            String owner,
            String name,
            String defaultBranch,
            String commitSha
    ) {}

    private record GithubRepoResponse (String default_branch) {}

    private record GithubCommitResponse(String sha) {}
}
