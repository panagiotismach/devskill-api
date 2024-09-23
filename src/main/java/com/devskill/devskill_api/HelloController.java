package com.devskill.devskill_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    @GetMapping("/mine-skills")
    public ResponseEntity<String> mineSkills(@RequestParam String url) {

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
    @GetMapping("/getDataFromGhArchive")
    public ResponseEntity<JsonNode> getDataFromGhArchive(@RequestParam String path) {
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
    @GetMapping("/countEventTypesInAMonth")
    public ResponseEntity<?> countEventTypesInAMonth(@RequestParam String date) {
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
    @GetMapping("/usersInAMonth")
    public ResponseEntity<?> usersInAMonth(@RequestParam String date) {
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
    @GetMapping("/pullRequestAndPushEventsInAMonth")
    public ResponseEntity<?> pullRequestAndPushEventsInAMonth(@RequestParam String date) {
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
    @GetMapping("/maxUsersInAMonth")
    public ResponseEntity<?> maxUsersInAMonth(@RequestParam String date) {
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
}
