package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Commit; // Assume you have a Commit model
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommitRepository extends JpaRepository<Commit, Long> {
    // You can define custom query methods here if needed

}