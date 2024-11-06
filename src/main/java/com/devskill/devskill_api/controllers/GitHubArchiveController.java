package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.RepositoryRepository;
import com.devskill.devskill_api.services.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.*;


@RestController
public class GitHubArchiveController {

    @Autowired
    private  ContributorsService contributorService;

    @Autowired
    private  RepoService repoService;

    @Autowired
    private CommitService commitService;

    @Autowired
    private  RepositoryRepository repositoryRepository;

    @Autowired
    private  RepositorySyncService repositorySyncService;



    @GetMapping("/getRepositories")
    public ResponseEntity<?> getRepositories(@RequestParam String name) {
        try {
            Map<String, Object> response = repoService.getRepositories(name);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/getContributors")
    public List<Contributor> getCommitsAndContributors(String repoName) throws Exception {
        return (List<Contributor>) contributorService.getContributors(repoName);
    }
    @GetMapping("/getChangedFilesForContributor")
    public List<String> getChangedFilesForContributor(String repoName, String name, String email) throws Exception {
        return contributorService.getChangedFilesForContributor(repoName,name,email);
    }

    @GetMapping("/getRepoDetails")
    public ResponseEntity<?> getRepoDetails(@RequestParam String repoName) {
        try {
            RepositoryEntity repository = repoService.getRepoDetails(repoName);
            return ResponseEntity.ok(repository); // Return 200 OK with the repo details
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason()); // Return the error status and message
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return 400 Bad Request
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(500).body("Internal Server Error: " + e.getMessage()); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/getCommits")
    public ResponseEntity<?> getCommits(@RequestParam String repoName, @RequestParam String repoUrl) {
        try {
            // Check if a repository exists by both repoName and repoUrl
            Optional<RepositoryEntity> existingRepo = repositoryRepository.findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(repoName, repoUrl);

            if (!existingRepo.isPresent()) {
                throw new Exception("The repo is not");
            }
            List<Commit> commits = commitService.getCommits(existingRepo.get());
            return ResponseEntity.ok(commits); // Return 200 OK with the repo commits
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return 400 Bad Request
        } catch ( Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage()); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/syncRepo")
    public ResponseEntity<?> syncRepo(@RequestParam String repoName) {
        try {
            Map<String, Object> syncData =  repositorySyncService.syncRepositoryData(repoName);
            return ResponseEntity.ok(syncData); // Return 200 OK with the syncData
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return 400 Bad Request
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Internal Server Error: \{e.getMessage()}"); // Return 500 Internal Server Error
        }
    }



}
