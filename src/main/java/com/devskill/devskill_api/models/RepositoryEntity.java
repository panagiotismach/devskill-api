package com.devskill.devskill_api.models;

import jakarta.persistence.*;


@Entity
@Table(name = "repositories")
public class RepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_name", nullable = false)
    private String repoName;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    // Constructors, Getters, Setters, Equals, and Hashcode

    public RepositoryEntity() {
    }

    public RepositoryEntity(String repoName, String repoUrl) {
        this.repoName = repoName;
        this.repoUrl = repoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    @Override
    public boolean equals(Object o) {
        // Equality logic based on ID or other fields
        if (this == o) return true;
        if (!(o instanceof RepositoryEntity)) return false;
        RepositoryEntity that = (RepositoryEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
