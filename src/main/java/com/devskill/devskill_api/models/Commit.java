package com.devskill.devskill_api.models;

import java.util.List;

public class Commit {
    private String commitHash;
    private String message;
    private String date;
    private List<String> changedFiles;

    public Commit(String commitHash, String message, String date, List<String> changedFiles) {
        this.commitHash = commitHash;
        this.message = message;
        this.date = date;
        this.changedFiles = changedFiles;
    }

    // Getters and Setters
    public String getCommitHash() {
        return commitHash;
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }
}
