package com.devskill.devskill_api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "contributors")
public class Contributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "github_username", nullable = false, unique = true)
    private String githubUsername;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email", unique = true)
    private String email;

    @OneToMany(mappedBy = "contributor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ContributorRepositoryEntity> contributorRepositories = new HashSet<>();

    public Contributor() {
    }

    public Contributor(String githubUsername, String fullName, String email) {
        this.githubUsername = githubUsername;
        this.fullName = fullName;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public void setGithubUsername(String githubUsername) {
        this.githubUsername = githubUsername;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<ContributorRepositoryEntity> getContributorRepositories() {
        return contributorRepositories;
    }

    public void setContributorRepositories(Set<ContributorRepositoryEntity> contributorRepositories) {
        this.contributorRepositories = contributorRepositories;
    }

    @Override
    public boolean equals(Object o) {
        // Your equality logic here
        return false;
    }

    @Override
    public int hashCode() {
        // Your hashcode logic here
        return 0;
    }
}
