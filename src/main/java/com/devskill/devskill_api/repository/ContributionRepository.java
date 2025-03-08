package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.models.Contributor;
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
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    // Method to find a contribution by contributor and extension
    Optional<Contribution> findByContributorAndExtension(Contributor contributor, String extension);
    @Query("SELECT c.contributor, SUM(c.insertions) AS totalInsertions, SUM(c.deletions) AS totalDeletions " +
            "FROM Contribution c " +
            "WHERE c.extension = :language " +
            "GROUP BY c.contributor " +
            "ORDER BY totalInsertions DESC, totalDeletions DESC")
    Page<Contributor> findTopContributorsByLanguage(String language, Pageable pageable);

    @Query("SELECT r, SUM(c.insertions), SUM(c.deletions) " +
            "FROM Contribution c " +
            "JOIN ContributorRepositoryEntity cr ON c.contributor.id = cr.contributor.id " +
            "JOIN cr.repository r " +
            "WHERE c.extension = :extension " +
            "GROUP BY r.id " +
            "ORDER BY SUM(c.insertions) DESC, SUM(c.deletions) DESC")
    Page<RepositoryEntity> findRepositoriesWithInsertionsAndDeletionsByLanguage(@Param("extension") String extension, Pageable pageable);

    @Query("SELECT c.contributor.email, SUM(c.insertions + c.deletions) AS totalContributions " +
            "FROM Contribution c " +
            "GROUP BY c.contributor " +
            "ORDER BY totalContributions DESC " +
            "LIMIT 5")
    List<Object[]> findTopContributors();

    @Query("SELECT c.extension, SUM(c.insertions), SUM(c.deletions) FROM Contribution c WHERE c.contributor.id = :contributorId GROUP BY c.extension")
    List<Object[]> findContributionsByContributor(@Param("contributorId") Long contributorId);

}

