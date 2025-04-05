package com.devskill.devskill_api.models;

import java.time.LocalDate;

public class ExtensionDTO {
    private String name;
    private int fileCount;
    private int repoCount;
    private LocalDate lastUsed;

    public ExtensionDTO(String name, int fileCount, int repoCount, LocalDate lastUsed) {
        this.name = name;
        this.fileCount = fileCount;
        this.repoCount = repoCount;
        this.lastUsed = lastUsed;
    }

    public String getName() {
        return name;
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