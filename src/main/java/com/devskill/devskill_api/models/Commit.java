package com.devskill.devskill_api.models;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "commits")
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @Column(name = "commit_hash", nullable = false)
    private String commitHash;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "date")
    private LocalDate date;

    @ManyToOne
    @JoinColumn(name = "repository_id", nullable = false)
    private RepositoryEntity repository;

    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL)
    private Set<FileChanged> filesChanged;

    @OneToMany(mappedBy = "commit", cascade = CascadeType.ALL)
    private Set<LibraryChanged> librariesChanged;

    // Constructors, Getters, Setters, Equals, and Hashcode

    public Commit() {
    }

    public Commit(Contributor contributor, String commitHash, String message, LocalDate date, RepositoryEntity repository) {
        this.commitHash = commitHash;
        this.message = message;
        this.date = date;
        this.repository = repository;
        this.contributor = contributor;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public RepositoryEntity getRepository() {
        return repository;
    }

    public void setRepository(RepositoryEntity repository) {
        this.repository = repository;
    }

    public Set<FileChanged> getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(Set<FileChanged> filesChanged) {
        this.filesChanged = filesChanged;
    }

    public Set<LibraryChanged> getLibrariesChanged() {
        return librariesChanged;
    }

    public void setLibrariesChanged(Set<LibraryChanged> librariesChanged) {
        this.librariesChanged = librariesChanged;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Commit)) return false;
        Commit that = (Commit) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
