package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.services.*;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Path;
import java.util.*;

@RestController
public class GitHubArchiveController {

    @Autowired
    private  ContributorsService contributorService;

    @Autowired
    private  RepoService repoService;

    @Autowired
    private Utils utils;

    @GetMapping("/getRepositories")
    public ResponseEntity<?> getRepositories(@RequestParam String name) {
        try {
            Map<String, Object> response = repoService.getRepositories(name);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/getContributorsByRepoName")
    public ResponseEntity<?> getContributorsByRepoName(@RequestParam String repoName) {
        try{
            List<Contributor> contributors = contributorService.getContributors(repoName);

            return ResponseEntity.ok(contributors);
        }catch (Exception e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/getChangedFilesForContributor")
    public List<String> getChangedFilesForContributor(@RequestParam String repoName,@RequestParam String name,@RequestParam String email) throws Exception {
        return contributorService.getChangedFilesForContributor(repoName,name,email);
    }

    @GetMapping("/getRepoDetails")
    public ResponseEntity<?> getRepoDetails(@RequestParam String repoName) {
        try {
            Path repositoryPath = utils.getPathOfRepository(repoName);
            RepositoryEntity repository = (RepositoryEntity) repoService.getRepoDetails(repositoryPath,false).get("repository");
            return ResponseEntity.ok(repository); // Return 200 OK with the repo details
        } catch (Exception e) {
            return ResponseEntity.status(500).body(STR."Internal Server Error: \{e.getMessage()}"); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/output")
    public ResponseEntity<?> output() {
        try {
            Map<String, Object> i = repoService.processFiles();
            return ResponseEntity.ok(i); // Return 200 OK with the repo commits
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return 400 Bad Request
        } catch ( Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage()); // Return 500 Internal Server Error
        }
    }
}