package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {
    // You can define custom query methods here if needed

    Contributor findByEmail(String email);

}