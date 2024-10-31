package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.services.CommitService;
import com.devskill.devskill_api.services.ContributorsService;
import com.devskill.devskill_api.services.GitHubArchiveService;
import com.devskill.devskill_api.services.RepoService;
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

    private final GitHubArchiveService gitHubArchiveService;
    private final ContributorsService contributorService;
    private final RepoService repoService;

    private final CommitService commitService;

    @Autowired
    public GitHubArchiveController(GitHubArchiveService gitHubArchiveService, ContributorsService contributorService, RepoService repoService, CommitService commitService) {
        this.gitHubArchiveService = gitHubArchiveService;
        this.contributorService = contributorService;
        this.repoService = repoService;
        this.commitService = commitService;
    }

    @Operation(summary = "Get GitHub archive data", description = "Retrieve GitHub archive data from the provided file path.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved archive data"),
            @ApiResponse(responseCode = "500", description = "Error reading or processing the archive file")
    })
    @GetMapping("/getDataFromGhArchive")
    public ResponseEntity<JsonNode> getDataFromGhArchive(
            @Parameter(description = "Path to the archive file", required = true)
            @RequestParam String path) {
        try {
            // Call the service to get the archive data as a JSON array
            ArrayNode jsonArray = gitHubArchiveService.getArchiveData(path);
            return ResponseEntity.ok(jsonArray);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with reading the archive
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @Operation(summary = "Count event types in a month", description = "Count the types of GitHub events that occurred during the given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully counted event types"),
            @ApiResponse(responseCode = "500", description = "Error processing the event data")
    })
    @GetMapping("/countEventTypesInAMonth")
    public ResponseEntity<?> countEventTypesInAMonth(
            @Parameter(description = "Date of the month to count users", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = gitHubArchiveService.getArchiveData(date);
            // Count event types from the archive data
            Map<String, Integer> eventCounts = gitHubArchiveService.countEventTypesInArchive(jsonArray);
            return ResponseEntity.ok(eventCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }
    }

    @Operation(summary = "Count GitHub users in a month", description = "Count the number of users participating in events during a given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully counted users"),
            @ApiResponse(responseCode = "500", description = "Error processing the user data")
    })
    @GetMapping("/usersInAMonth")
    public ResponseEntity<?> findUsersInAMonth(
            @Parameter(description = "Date of the month to count users", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = gitHubArchiveService.getArchiveData(date);
            // Count the number of users from the archive data
            Map<String, Integer> userCounts = gitHubArchiveService.countUsersInArchive(jsonArray);
            return ResponseEntity.ok(userCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }
    }

    @Operation(summary = "Count pull request and push events", description = "Count the pull request and push events for users in a given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully counted pull request and push events"),
            @ApiResponse(responseCode = "500", description = "Error processing the event data")
    })
    @GetMapping("/pullRequestAndPushEventsInAMonth")
    public ResponseEntity<?> pullRequestAndPushEventsInAMonth(
            @Parameter(description = "Date of the month to find the maximum user activity", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = gitHubArchiveService.getArchiveData(date);
            // Count pull request and push events for users
            Map<String, List<Map<String, Integer>>> userEventCounts = gitHubArchiveService.countPullRequestAndPushEvents(jsonArray);
            return ResponseEntity.ok(userEventCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the data
            return ResponseEntity.internalServerError().body(STR."Error processing event counts: \{e.getMessage()}");
        }
    }

    @Operation(summary = "Find user with maximum events", description = "Find the user(s) with the maximum number of events in a given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully found user with maximum events"),
            @ApiResponse(responseCode = "500", description = "Error processing the event data")
    })
    @GetMapping("/maxUsersInAMonth")
    public ResponseEntity<?> findMaxUsersInAMonth(
            @Parameter(description = "Date of the month to find the maximum user activity", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = gitHubArchiveService.getArchiveData(date);
            // Find the user(s) with the maximum number of events
            Map<String, Object> maxUsersAndValue = gitHubArchiveService.findUsersWithMaxEvents(jsonArray);
            return ResponseEntity.ok(maxUsersAndValue);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the data
            return ResponseEntity.internalServerError().body(STR."Error processing max users: \{e.getMessage()}");
        }
    }

    @GetMapping("/getRepositories")
    public ResponseEntity<?> getRepositories(@RequestParam String name) {
        try {
            Map<String, Object> response = repoService.getRepositories(name);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/process-files")
    public ResponseEntity<?> processFiles() {
        try {
            Map<String, Object> response = repoService.processFiles();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/findUniqueExtensions")
    public ResponseEntity<Map<String, Integer>> findUniqueExtensions() {
        Map<String, Integer> result = repoService.findUniqueExtensions();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getContributors")
    public List<Contributor> getCommitsAndContributors(String repoName) throws Exception {
        return contributorService.getCommitsAndContributors(repoName);
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
    public ResponseEntity<?> getCommits(@RequestParam String repoName, @RequestParam Long repoId) {
        try {
            List<Commit> commits = commitService.getCommitsForRepo(repoName, repoId);
            return ResponseEntity.ok(commits); // Return 200 OK with the repo commits
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // Return 400 Bad Request
        } catch (IOException | InterruptedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error: " + e.getMessage()); // Return 500 Internal Server Error
        }
    }



}
