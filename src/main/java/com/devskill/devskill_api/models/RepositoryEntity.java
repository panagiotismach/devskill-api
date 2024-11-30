package com.devskill.devskill_api.models;

import com.devskill.devskill_api.utils.ListToJsonConverter;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.*;


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

    @Column(name = "creation_date")
    private LocalDate creation_date;

    @Column(name = "last_commit_date")
    private LocalDate last_commit_date;

    @Column(name = "extensions", columnDefinition = "TEXT")
    @Convert(converter = ListToJsonConverter.class)
    private List<String> extensions;

    @OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ContributorRepositoryEntity> contributorRepositories = new HashSet<>();


    // Constructors, Getters, Setters, Equals, and Hashcode

    public RepositoryEntity() {
    }

    public RepositoryEntity(String repoName, String repoUrl, LocalDate creation_date, LocalDate last_commit_date, List<String> extensions) {
        this.repoName = repoName;
        this.repoUrl = repoUrl;
        this.creation_date = creation_date;
        this.extensions = extensions;
        this.last_commit_date = last_commit_date;
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

    public String getName() {
        return repoName.split("/")[1];
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

    public LocalDate getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(LocalDate creation_date) {
        this.creation_date = creation_date;
    }

    public LocalDate getLast_commit_date() {
        return last_commit_date;
    }

    public void setLast_commit_date(LocalDate last_commit_date) {
        this.last_commit_date = last_commit_date;
    }

    public Set<ContributorRepositoryEntity> getContributorRepositories() {
        return contributorRepositories;
    }

    public void setContributorRepositories(Set<ContributorRepositoryEntity> contributorRepositories) {
        this.contributorRepositories = contributorRepositories;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
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
