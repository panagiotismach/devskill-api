package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.models.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {
    // Method to find a contribution by contributor and extension
    Optional<Contribution> findByContributorAndExtension(Contributor contributor, String extension);
}

