package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.RepositoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepositoryRepository extends JpaRepository<RepositoryEntity, Long> {
    // You can define custom query methods here if needed
    Optional<RepositoryEntity> findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(String repoName, String repoUrl);
    Page<RepositoryEntity> findByRepoNameOrRepoUrl(String repoName, String repoUrl, Pageable pageable);

    @Query(value = """
    SELECT ext_a, ext_b, co_count
    FROM extension_co_occurrence
    WHERE co_count >= :minCount
    ORDER BY co_count DESC
    LIMIT :limit
""", nativeQuery = true)
    List<Object[]> findExtensionCoOccurrence(
            @Param("minCount") int minCount,
            @Param("limit") int limit
    );


    @Query(value = "SELECT * FROM repositories r ORDER BY r.creation_date DESC LIMIT 1", nativeQuery = true)
    RepositoryEntity findFirstByOrderByCreationDateDesc();

    @Query("SELECT SUBSTRING(r.repoName, 1, LOCATE('/', r.repoName) - 1) as org, COUNT(r) " +
            "FROM RepositoryEntity r GROUP BY org ORDER BY COUNT(r) DESC")
    List<Object[]> countReposPerOrganization();

    @Query(value = """
        SELECT
          ext.extension AS extension,
          to_char(r.last_commit_date, 'YYYY-MM') AS month,
          COUNT(*) AS count
        FROM repositories r
        CROSS JOIN LATERAL jsonb_array_elements_text(r.extensions::jsonb) AS ext(extension)
        WHERE r.last_commit_date IS NOT NULL
        GROUP BY ext.extension, month
        ORDER BY month, ext.extension
""", nativeQuery = true)
    List<Object[]> findMonthlyExtensionTrends();


    @Query(value = """
    SELECT
        SPLIT_PART(r.repo_name, '/', 1) AS organization,
        ext.value AS extension,
        COUNT(ext.value) AS occurrences
    FROM repositories r,
         JSON_ARRAY_ELEMENTS_TEXT(r.extensions::json) AS ext(value)
    GROUP BY organization, ext.value
    ORDER BY SUM(COUNT(ext.value)) OVER (PARTITION BY SPLIT_PART(r.repo_name, '/', 1)) DESC, occurrences DESC
    """, nativeQuery = true)
    List<Object[]> getExtensionsPerOrganization();



}