package dev.ayman.repopilot.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IndexedRepositoryRepository extends JpaRepository<IndexedRepository, UUID> {
    Optional<IndexedRepository> findByOwnerAndName(String owner, String name);
}
