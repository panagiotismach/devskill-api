package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.services.ExtensionAggregationService;
import com.devskill.devskill_api.services.RepoService;
import com.devskill.devskill_api.services.RepositorySyncService;
import com.devskill.devskill_api.utils.General;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SyncRepoController {

    @Autowired
    private RepositorySyncService repositorySyncService;

    @Autowired
    private RepoService repoService;

    @Autowired
    private ExtensionAggregationService aggregationService;

    @Autowired
    private Utils utils;

    @GetMapping("/syncRepo")
    public ResponseEntity<?> syncRepo(@RequestParam String repoName) {
        try {
            List<String> trendingRepositories =  repoService.getTrendingRepositories();
           boolean isTrending = trendingRepositories.stream().anyMatch(trendingRepo -> trendingRepo.equalsIgnoreCase(repoName));

            repositorySyncService.executeSync(General.FILES.getValue(),General.MB.getValue(), List.of(repoName) , isTrending);

            return ResponseEntity.ok("\"The process of syncing the repository data has been started.\""); // Return 200 OK with the syncData
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Internal Server Error: \{e.getMessage()}"); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/syncRepositories")
    public String syncRepositories(@RequestParam(defaultValue = "1000") int files , @RequestParam(defaultValue = "150") long megabyte, @RequestParam(defaultValue = "16") String from , @RequestParam(defaultValue = "19") String to) throws Exception {

        if (utils.checkPeriodSync(to, from)) {
            from = "16";
            to = "19";
        }

        List<String> repositories = repositorySyncService.readRepositoryNamesFromJson(from, to);
        repositorySyncService.executeSync(files,megabyte, repositories, false);

        return "The process of syncing the repositories data has been started.";
    }

    @GetMapping("/syncTrendingRepositories")
    public String syncTrendingRepositories(@RequestParam(defaultValue = "1") int files, @RequestParam(defaultValue = "1") long megabyte) throws Exception {

        List<String> trendingRepositories =  repoService.getTrendingRepositories();
        repositorySyncService.executeSync(files,megabyte, trendingRepositories, true);

        return "The process of syncing the trending repositories data has been started.";
    }

    @GetMapping("/retrieveRepoProgress")
    public ResponseEntity<Map<String, Object>> retrieveRepoProgress( @RequestParam(defaultValue = "16") String from , @RequestParam(defaultValue = "19") String to) throws Exception {

        if (utils.checkPeriodSync(to, from)) {
            from = "16";
            to = "19";
        }

        List<String> repositoriesList =  repositorySyncService.readRepositoryNamesFromJson(from, to);
        RepositoryEntity lastRepository = repoService.findFirstByOrderByCreation_dateDesc();
        
        int index = repositoriesList.indexOf(lastRepository.getRepoName());

        Map<String, Object> response =  new HashMap<>();
        response.put("repositoryIndex", index);
        response.put("repositoriesListSize", repositoriesList.size());
        response.put("RemainingRepositories", repositoriesList.size() - index);
        response.put("lastRepository", lastRepository);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/aggregate-extensions")
    public ResponseEntity<String> aggregateExtensions() {
        aggregationService.updateExtensionSummary();
        return ResponseEntity.ok("Extension summary updated");
    }
}
