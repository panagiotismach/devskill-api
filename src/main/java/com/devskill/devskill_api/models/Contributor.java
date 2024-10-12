package com.devskill.devskill_api.models;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Contributor {
    private String name;
    private String email;
    private Map<String, List<String>> filesByCommit; // Key: commit hash, Value: list of changed files

    public Contributor(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

}
