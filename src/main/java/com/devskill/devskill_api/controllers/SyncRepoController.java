package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.services.RepositorySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class SyncRepoController {

    @Autowired
    private RepositorySyncService repositorySyncService;

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
