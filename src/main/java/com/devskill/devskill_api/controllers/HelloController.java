package com.devskill.devskill_api.controllers;



import com.devskill.devskill_api.services.HelloService;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RestController
public class HelloController {

    private final HelloService helloService;

    // Constructor-based dependency injection of HelloService
    @Autowired
    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    /**
     * Endpoint to download a repo from GitHub given its URL.
     * It extracts the organization and repository name from the URL and triggers a service to download the repository ZIP file.
     *
     * @param url The GitHub repository URL.
     * @return ResponseEntity with a message indicating the result of the operation.
     */
    @Operation(summary = "Download GitHub repository src", description = "Download and save the source of a GitHub repository based on its URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully downloaded the repository"),
            @ApiResponse(responseCode = "400", description = "Invalid GitHub URL format"),
            @ApiResponse(responseCode = "500", description = "Failed to download or save ZIP file")
    })
    @GetMapping("/getRepositorySrc")
    public ResponseEntity<String> getRepositorySrc(
            @Parameter(description = "GitHub repository URL", required = true)
            @RequestParam String url) {

        String[] parts = url.split("/");
        if (parts.length < 5 || !"github.com".equals(parts[2])) {
            // Return 400 Bad Request if the URL is not in the expected format
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid GitHub URL format");
        }

        String organization = parts[3];  // GitHub organization
        String repository = parts[4];    // GitHub repository

        try {
            // Call the service to download and save the repository as a ZIP file
            String message = helloService.downloadRepositoryZip(organization, repository);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with downloading the ZIP file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Failed to download or save ZIP file: \{e.getMessage()}");
        }
    }

    /**
     * Endpoint to retrieve GitHub archive data from a given path.
     *
     * @param path The path of the archive file.
     * @return JSON array representing the archive data.
     */
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
            ArrayNode jsonArray = helloService.getArchiveData(path);
            return ResponseEntity.ok(jsonArray);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with reading the archive
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Endpoint to count the different types of GitHub events in a given month.
     *
     * @param date The date string representing the month.
     * @return Map with event types as keys and their counts as values.
     */
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
            ArrayNode jsonArray = helloService.getArchiveData(date);
            // Count event types from the archive data
            Map<String, Integer> eventCounts = helloService.countEventTypesInArchive(jsonArray);
            return ResponseEntity.ok(eventCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }
    }

    /**
     * Endpoint to count the number of users involved in GitHub events in a given month.
     *
     * @param date The date string representing the month.
     * @return Map with usernames as keys and the number of events they participated in as values.
     */
    @Operation(summary = "Count GitHub users in a month", description = "Count the number of users participating in events during a given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully counted users"),
            @ApiResponse(responseCode = "500", description = "Error processing the user data")
    })
    @GetMapping("/usersInAMonth")
    public ResponseEntity<?> usersInAMonth(
            @Parameter(description = "Date of the month to count users", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = helloService.getArchiveData(date);
            // Count the number of users from the archive data
            Map<String, Integer> userCounts = helloService.countUsersInArchive(jsonArray);
            return ResponseEntity.ok(userCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }
    }

    /**
     * Endpoint to count pull request and push events for users in a given month.
     *
     * @param date The date string representing the month.
     * @return Map of users and their pull request/push event counts.
     */
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
            ArrayNode jsonArray = helloService.getArchiveData(date);
            // Count pull request and push events for users
            Map<String, List<Map<String, Integer>>> userEventCounts = helloService.countPullRequestAndPushEvents(jsonArray);
            return ResponseEntity.ok(userEventCounts);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the data
            return ResponseEntity.internalServerError().body(STR."Error processing event counts: \{e.getMessage()}");
        }
    }

    /**
     * Endpoint to find the user with the maximum number of events in a given month.
     *
     * @param date The date string representing the month.
     * @return Map containing the user(s) with the maximum events and the count.
     */
    @Operation(summary = "Find user with maximum events", description = "Find the user(s) with the maximum number of events in a given month.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully found user with maximum events"),
            @ApiResponse(responseCode = "500", description = "Error processing the event data")
    })
    @GetMapping("/maxUsersInAMonth")
    public ResponseEntity<?> maxUsersInAMonth(
            @Parameter(description = "Date of the month to find the maximum user activity", required = true)
            @RequestParam String date) {
        try {
            // Retrieve archive data for the given date
            ArrayNode jsonArray = helloService.getArchiveData(date);
            // Find the user(s) with the maximum number of events
            Map<String, Object> maxUsersAndValue = helloService.getUsersWithMaxEvents(jsonArray);
            return ResponseEntity.ok(maxUsersAndValue);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the data
            return ResponseEntity.internalServerError().body(STR."Error processing max users: \{e.getMessage()}");
        }
    }

    @GetMapping("/getArchiveSH")
    public ResponseEntity<?> getArchiveSH(
            @Parameter(description = "Path to the archive", required = true)
            @RequestParam String path) {
        try {
            // Retrieve archive data for the given date
            String message = helloService.getArchiveSH(path);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with processing the data
            return ResponseEntity.internalServerError().body(STR."Error processing max users: \{e.getMessage()}");
        }
    }

}
