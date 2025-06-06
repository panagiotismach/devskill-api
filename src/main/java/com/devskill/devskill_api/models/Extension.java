package com.devskill.devskill_api.models;

import com.devskill.devskill_api.utils.StringListConverter;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "extension")
public class Extension {

    @Id
    @Column(name = "extension_name", nullable = false, length = 250, unique = true)
    private String extensionName;

    @Convert(converter = StringListConverter.class)
    @Column(name = "language", nullable = false, length = 1000)
    private List<String> language;

    @Column(name = "repo_count", nullable = false)
    private int repoCount;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Column(name = "last_used")
    private LocalDate lastUsed;

    // Default constructor required by JPA
    public Extension() {
    }

    // Constructor for creating instances
    public Extension(String extensionName, List<String> language, int repoCount, int fileCount, LocalDate lastUsed) {
        this.extensionName = extensionName;
        this.language = language;
        this.repoCount = repoCount;
        this.fileCount = fileCount;
        this.lastUsed = lastUsed;
    }

    // Getters and Setters
    public String getExtensionName() {
        return extensionName;
    }

    public void setExtensionName(String extensionName) {
        this.extensionName = extensionName;
    }

    public int getRepoCount() {
        return repoCount;
    }

    public void setRepoCount(int repoCount) {
        this.repoCount = repoCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public LocalDate getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(LocalDate lastUsed) {
        this.lastUsed = lastUsed;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    // Optional: Override equals and hashCode for entity consistency
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Extension)) return false;
        Extension that = (Extension) o;
        return extensionName != null && extensionName.equals(that.extensionName);
    }

    @Override
    public int hashCode() {
        return extensionName != null ? extensionName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ExtensionSummary{" +
                "extensionName='" + extensionName + '\'' +
                ", repoCount=" + repoCount +
                ", fileCount=" + fileCount +
                ", lastUsed=" + lastUsed +
                '}';
    }
}