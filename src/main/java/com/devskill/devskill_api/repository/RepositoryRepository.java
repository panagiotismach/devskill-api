package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {
    // You can define custom query methods here if needed

    // Find a repository by both name and URL (case-insensitive)
    Optional<RepositoryEntity> findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(String repoName, String repoUrl);

    Page<RepositoryEntity> findByRepoNameOrRepoUrl(String repoName, String repoUrl, Pageable pageable);

    Page<RepositoryEntity> findAllByTrending(boolean trending, Pageable pageable);

}