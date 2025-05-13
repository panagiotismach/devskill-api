package com.devskill.devskill_api.models;

import java.time.LocalDate;
import java.util.List;

public class ExtensionDTO {
    private String name;

    private List<String> language;
    private int fileCount;
    private int repoCount;
    private LocalDate lastUsed;

    public ExtensionDTO(String name,List<String> language , int fileCount, int repoCount, LocalDate lastUsed) {
        this.name = name;
        this.language = language;
        this.fileCount = fileCount;
        this.repoCount = repoCount;
        this.lastUsed = lastUsed;
    }

    public String getName() {
        return name;
    }

    public List<String> getLanguage() {
        return language;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getRepoCount() {
        return repoCount;
    }

    public LocalDate getLastUsed() {
        return lastUsed;
    }
}