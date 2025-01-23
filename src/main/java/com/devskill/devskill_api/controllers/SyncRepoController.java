package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.services.RepoService;
import com.devskill.devskill_api.services.RepositorySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class SyncRepoController {

    @Autowired
    private RepositorySyncService repositorySyncService;

    @Autowired
    private RepoService repoService;

    @GetMapping("/syncRepo")
    public ResponseEntity<?> syncRepo(@RequestParam String repoName) {
        try {
            Map<String, Object> syncData =  repositorySyncService.syncRepositoryData(repoName, 1000, 300, false);
            return ResponseEntity.ok(syncData); // Return 200 OK with the syncData
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Internal Server Error: \{e.getMessage()}"); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/syncRepositories")
    public String syncRepositories(@RequestParam(defaultValue = "1000") int files , @RequestParam(defaultValue = "300") long megabyte) throws Exception {

        List<String> repositories = repositorySyncService.readRepositoryNamesFromJson();
        repositorySyncService.executeSync(files,megabyte, repositories, false);

        return "The process of syncing the repositories data has been started.";
    }


    @GetMapping("/syncTrendingRepositories")
    public String syncTrendingRepositories(@RequestParam(defaultValue = "1") int files, @RequestParam(defaultValue = "1") long megabyte) throws Exception {

        List<String> trendingRepositories =  repoService.getTrendingRepositories();
        repositorySyncService.executeSync(files,megabyte, trendingRepositories, true);

        return "The process of syncing the trending repositories data has been started.";

    }
}
