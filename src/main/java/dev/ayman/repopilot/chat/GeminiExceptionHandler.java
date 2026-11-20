package dev.ayman.repopilot.chat;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GeminiExceptionHandler {

    @ExceptionHandler(com.google.genai.errors.ServerException.class)
    public ResponseEntity<Map<String, String>> handleGeminiException(com.google.genai.errors.ServerException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "AI_SERVICE_UNAVAILABLE",
                        "message",
                        "Gemini is temporarily unavailable. Please retry later."
                ));
    }
}
