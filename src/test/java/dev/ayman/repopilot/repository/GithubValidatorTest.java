package dev.ayman.repopilot.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GithubValidatorTest {
    private final GithubUrlValidator validator =
            new GithubUrlValidator();


    @Test
    void shouldAcceptValidGithubUrl() {
        assertTrue(validator.isValid("https://github.com/spring-projects/spring-petclinic"));
    }

    @Test
    void shouldRejectInvalidGithubUrl() {
        assertFalse(validator.isValid("https://gitlab.com/user/repo"));
    }

    @Test
    void shouldRejectGithubNonHttpsUrl() {
        assertFalse(validator.isValid("http://github.com/user/repo"));
    }

    @Test
    void shouldRejectGitHubSubpage() {
        assertFalse(validator.isValid(
                "https://github.com/user/repo/issues"
        ));
    }
}
