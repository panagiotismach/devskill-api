package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.services.SoftwareHeritageService;
import com.devskill.devskill_api.services.WaybackService;
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

import java.io.IOException;

@RestController
public class WaybackController {

    private final WaybackService waybackService;

    @Autowired
    public WaybackController(WaybackService waybackService) {
        this.waybackService = waybackService;
    }

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
            String message = waybackService.downloadRepositoryFromWayBack(organization, repository);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            // Return 500 Internal Server Error if there's an issue with downloading the ZIP file
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Failed to download or save ZIP file: \{e.getMessage()}");
        }
    }

}

