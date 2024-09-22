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
import java.util.Map;

@RestController
public class HelloController {

    private final HelloService helloService;

    @Autowired
    public HelloController(HelloService helloService) {
        this.helloService = helloService;
    }

    @GetMapping("/mine-skills")
    public ResponseEntity<String> mineSkills(@RequestParam String url) {
        // Extract the organization and repository from the URL
        String[] parts = url.split("/");
        if (parts.length < 5 || !"github.com".equals(parts[2])) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid GitHub URL format");
        }

        String organization = parts[3];
        String repository = parts[4];

        try {
            String message = helloService.downloadRepositoryZip(organization, repository);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to download or save ZIP file: " + e.getMessage());
        }
    }

    @GetMapping("/archivegh")
    public ResponseEntity<JsonNode> archive(@RequestParam String path) {
        try {
            ArrayNode jsonArray = helloService.getArchiveData(path);
            return ResponseEntity.ok(jsonArray);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/countEventTypesInAMonth")
    public ResponseEntity<?> countEventTypesInAMonth(@RequestParam String date) {
        try {
            ArrayNode jsonArray = helloService.getArchiveData(date);
            Map<String, Integer> eventCounts = helloService.countEventTypesInArchive(jsonArray);
            return ResponseEntity.ok(eventCounts);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading or processing the file: " + e.getMessage());
        }
    }

    @GetMapping("/usersInAMonth")
    public ResponseEntity<?> usersInAMonth(@RequestParam String date) {
        try {
            ArrayNode jsonArray = helloService.getArchiveData(date);
            Map<String, Integer> userCounts = helloService.countUsersInArchive(jsonArray);
            return ResponseEntity.ok(userCounts);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading or processing the file: " + e.getMessage());
        }
    }

    @GetMapping("/maxUsersInAMonth")
    public ResponseEntity<?> maxUsersInAMonth(@RequestParam String date) {
        try {
            ArrayNode jsonArray = helloService.getArchiveData(date);
            Map<String, Object> maxUsersAndValue = helloService.getUsersWithMaxEvents(jsonArray);
            return ResponseEntity.ok(maxUsersAndValue);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error processing max users: " + e.getMessage());
        }
    }
}
