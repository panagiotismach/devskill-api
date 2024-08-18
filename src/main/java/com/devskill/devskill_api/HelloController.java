package com.devskill.devskill_api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class HelloController {

    private final RestTemplate restTemplate;

    @Autowired
    public HelloController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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

        // Construct the URL to download the master branch as a ZIP file
        String downloadUrl = "https://codeload.github.com/" + organization + "/" + repository + "/zip/refs/heads/master";

        // Define the path where the file will be saved
        Path filePath = Paths.get("downloaded.zip");

        try {
            // Download the file
            byte[] zipFileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

            if (zipFileBytes == null || zipFileBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to download ZIP file");
            }

            // Save the file
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(zipFileBytes);
            }

            return ResponseEntity.ok("Successfully downloaded and saved ZIP file for " + organization + "/" + repository);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to download or save ZIP file: " + e.getMessage());
        }
    }
}
