package com.devskill.devskill_api.repository;

import com.devskill.devskill_api.models.Extension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtensionRepository extends JpaRepository<Extension, String> {
    Page<Extension> findAll(Pageable pageable);
    Page<Extension> findByExtensionName(String extensionName, Pageable pageable);
}