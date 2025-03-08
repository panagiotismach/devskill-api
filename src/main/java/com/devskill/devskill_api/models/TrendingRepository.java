package com.devskill.devskill_api.models;

import jakarta.persistence.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

@Entity
@Table(name = "trending_repositories")
public class TrendingRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "repo_id", nullable = false, unique = true)
    private RepositoryEntity repository;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public TrendingRepository() {}

    public TrendingRepository(RepositoryEntity repository) {
        this.repository = repository;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public RepositoryEntity getRepository() {
        return repository;
    }

    public void setRepository(RepositoryEntity repository) {
        this.repository = repository;
    }
}
