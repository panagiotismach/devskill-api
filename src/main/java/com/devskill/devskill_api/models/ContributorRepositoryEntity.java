package com.devskill.devskill_api.models;

import jakarta.persistence.*;

@Entity
@Table(name = "contributor_repository")
public class ContributorRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @ManyToOne
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    public ContributorRepositoryEntity() {
    }

    public ContributorRepositoryEntity(Contributor contributor, RepositoryEntity repository) {
        this.contributor = contributor;
        this.repository = repository;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public RepositoryEntity getRepository() {
        return repository;
    }

    public void setRepository(RepositoryEntity repository) {
        this.repository = repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContributorRepositoryEntity)) return false;
        ContributorRepositoryEntity that = (ContributorRepositoryEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
