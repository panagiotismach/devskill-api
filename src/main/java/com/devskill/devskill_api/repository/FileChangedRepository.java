package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.FileChanged;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileChangedRepository extends JpaRepository<FileChanged, Long> {
    // You can define custom query methods here if needed

}
