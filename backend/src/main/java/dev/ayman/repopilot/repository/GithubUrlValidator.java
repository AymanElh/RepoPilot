package dev.ayman.repopilot.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GithubUrlValidator {

    private static final Pattern PATH_PATTERN =
            Pattern.compile("^/([a-zA-Z0-9-]+)/([a-zA-Z0-9._-]+)$");

    public boolean isValid(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "github.com".equalsIgnoreCase(uri.getHost())
                    && uri.getRawUserInfo() == null
                    && uri.getPort() == -1
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null


                    && PATH_PATTERN.matcher(uri.getPath()).matches();
        } catch (IllegalArgumentException e) {
            log.debug(e.getMessage());
            return false;
        }
    }
}
