package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {
    // You can define custom query methods here if needed

    // Find a repository by both name and URL (case-insensitive)
    Optional<RepositoryEntity> findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(String repoName, String repoUrl);
}