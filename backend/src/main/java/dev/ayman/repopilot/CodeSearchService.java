package dev.ayman.repopilot;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;


import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CodeSearchService {

    private final VectorStore vectorStore;

    public void indexSampleCode() {
        List<Document> documents = List.of(
                new Document(
                        """
                                public boolean validateToken(String token) {
                                    return jwtService.verify(token);
                                }
                                """,
                        Map.of(
                                "filePath", "auth/JwtService.java",
                                "startLine", 10,
                                "endLine", 12,
                                "repositoryId", "demo-repo"
                        )
                ),
                new Document(
                        """
                                public User registerUser(RegisterRequest request) {
                                    User user = new User(request.email());
                                    return userRepository.save(user);
                                }
                                """,
                        Map.of(
                                "filePath", "user/UserService.java",
                                "startLine", 20,
                                "endLine", 23,
                                "repositoryId", "demo-repo"
                        )
                ),
                new Document(
                        """
                                public Payment processPayment(PaymentRequest request) {
                                    return paymentGateway.charge(request.amount());
                                }
                                """,
                        Map.of(
                                "filePath", "payment/PaymentService.java",
                                "startLine", 30,
                                "endLine", 32,
                                "repositoryId", "demo-repo"
                        )
                )
        );

        vectorStore.add(documents);
    }

    public List<Document> search(String question) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(3)
                .similarityThreshold(0.0)
                .filterExpression("repositoryId == 'demo-repo'")
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }
}
