package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.services.SoftwareHeritageService;
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
public class SoftwareHeritageController {

    private final SoftwareHeritageService softwareHeritageService;

    @Autowired
    public SoftwareHeritageController(SoftwareHeritageService softwareHeritageService) {
        this.softwareHeritageService = softwareHeritageService;
    }

    @Operation(summary = "Retrieve archive data from Software Heritage",
            description = "Fetches and returns archive data from the Software Heritage repository using the provided path.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved archive data"),
            @ApiResponse(responseCode = "400", description = "Invalid path provided"),
            @ApiResponse(responseCode = "500", description = "Internal server error while processing the request")
    })
    @GetMapping("/getArchiveSH")
    public ResponseEntity<?> getArchiveSH(
            @Parameter(description = "Path to the archive in Software Heritage", required = true)
            @RequestParam String path) {
        try {
            // Call the service to retrieve the archive data
            String message = softwareHeritageService.getArchiveSH(path);
            return ResponseEntity.ok(message);
        } catch (IOException e) {
            // Handle any exceptions that occur during processing
            return ResponseEntity.internalServerError().body("Error processing archive data: " + e.getMessage());
        }
    }

}
