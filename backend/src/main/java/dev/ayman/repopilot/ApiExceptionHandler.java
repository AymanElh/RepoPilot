package dev.ayman.repopilot;

import dev.ayman.repopilot.ai.ChatProviderUnavailableException;
import dev.ayman.repopilot.ai.UnsupportedChatProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), String.valueOf(error.getDefaultMessage())));
        Map<String, Object> body = body("VALIDATION_FAILED", "Request validation failed");
        body.put("fields", fields);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Malformed JSON, or a provider value that is not in the enum.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(body("MALFORMED_REQUEST",
                "Request body is missing or invalid. Provider must be one of: GEMINI, OLLAMA."));
    }

    @ExceptionHandler(UnsupportedChatProviderException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedProvider(UnsupportedChatProviderException ex) {
        return respond(HttpStatus.BAD_REQUEST, "UNSUPPORTED_PROVIDER", ex.getMessage());
    }

    /**
     * e.g. "Invalid GitHub URL".
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage());
    }

    /**
     * e.g. "Repository is already being indexed".
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
        return respond(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage());
    }

    /**
     * HTTP errors coming back from the GitHub API.
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleGithub(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();
        if (status == 404) {
            return respond(HttpStatus.NOT_FOUND, "GITHUB_NOT_FOUND",
                    "Repository not found on GitHub (or it is private).");
        }
        if (status == 403 || status == 429) {
            return respond(HttpStatus.TOO_MANY_REQUESTS, "GITHUB_RATE_LIMITED",
                    "GitHub rate limit reached. Please retry later.");
        }
        return respond(HttpStatus.BAD_GATEWAY, "GITHUB_ERROR", "GitHub returned an error (" + status + ").");
    }

    @ExceptionHandler(RepositoryNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRepositoryNotFound(RepositoryNotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, "REPOSITORY_NOT_FOUND", ex.getMessage());
    }

    /**
     * The indexing queue is full.
     */
    @ExceptionHandler(TaskRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleQueueFull(TaskRejectedException ex) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "INDEXING_QUEUE_FULL",
                "Too many repositories are being indexed. Please retry later.");
    }

    /**
     * Chat on a repository that exists but is still INDEXING (or FAILED / PENDING).
     */
    @ExceptionHandler(RepositoryNotReadyException.class)
    public ResponseEntity<Map<String, Object>> handleNotReady(RepositoryNotReadyException ex) {
        return respond(HttpStatus.CONFLICT, "REPOSITORY_NOT_READY", ex.getMessage());
    }

    /**
     * The chosen chat provider (e.g. Ollama) cannot be reached.
     */
    @ExceptionHandler(ChatProviderUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleProviderUnavailable(ChatProviderUnavailableException ex) {
        log.warn("Chat provider unavailable: {}", ex.getMessage());
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER_UNAVAILABLE", ex.getMessage());
    }

    /**
     * Any other remote service we could not connect to (GitHub, embeddings...).
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnreachable(ResourceAccessException ex) {
        log.warn("Remote service unreachable", ex);
        return respond(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNREACHABLE",
                "A remote service could not be reached. Please retry later.");
    }

    /**
     * Last resort: same JSON shape, details only in the log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong on our side.");
    }

    private ResponseEntity<Map<String, Object>> respond(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(body(code, message));
    }

    private Map<String, Object> body(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
