package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contributor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContributorRepository extends JpaRepository<Contributor, Long> {
    Contributor findByEmail(String email);

    Page<Contributor> findAll(Pageable pageable);

   Page<Contributor> findByGithubUsernameOrFullName(String username, String name, Pageable pageable);

    Page<Contributor> findByFullName(String name, Pageable pageable);

}