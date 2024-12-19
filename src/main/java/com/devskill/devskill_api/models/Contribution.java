package com.devskill.devskill_api.models;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "contributions")
public class Contribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "contributor_id", nullable = false)
    private Contributor contributor;

    @Column(name = "extension", nullable = false, length = 50)
    private String extension;

    @Column(name = "insertions", nullable = false)
    private int insertions;

    @Column(name = "deletions", nullable = false)
    private int deletions;

    public Contribution() {
    }

    public Contribution(Contributor contributor, String extension, int insertions, int deletions) {
        this.contributor = contributor;
        this.extension = extension;
        this.insertions = insertions;
        this.deletions = deletions;
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

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public int getInsertions() {
        return insertions;
    }

    public void setInsertions(int insertions) {
        this.insertions = insertions;
    }

    public int getDeletions() {
        return deletions;
    }

    public void setDeletions(int deletions) {
        this.deletions = deletions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contribution that = (Contribution) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(contributor, that.contributor) &&
                Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, contributor, extension);
    }
}
