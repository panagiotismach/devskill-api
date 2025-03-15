package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.RepositoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {
    // You can define custom query methods here if needed
    Optional<RepositoryEntity> findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(String repoName, String repoUrl);
    Page<RepositoryEntity> findByRepoNameOrRepoUrl(String repoName, String repoUrl, Pageable pageable);

    @Query(value = "SELECT jsonb_array_elements_text(r.extensions) AS extension, COUNT(*) AS count " +
            "FROM repositories r " +
            "GROUP BY extension " +
            "ORDER BY count DESC " +
            "LIMIT 5",
            nativeQuery = true)
    List<Object[]> findTopExtensionsWithRepositoryCount();

}