package dev.ayman.repopilot.repository;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Getter @Setter
@NoArgsConstructor
public class IndexedRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String name;

    private String defaultBranch;

    private String indexedCommitSha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepositoryStatus status = RepositoryStatus.PENDING;

    @Column(columnDefinition = "text")
    private String errorMessage;

    private int javaFiles;

    private int chunkCount;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String fullName() {
        return owner + "/" + name;
    }
}
