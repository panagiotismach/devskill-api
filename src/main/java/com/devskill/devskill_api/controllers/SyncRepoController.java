package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.services.RepoService;
import com.devskill.devskill_api.services.RepositorySyncService;
import com.devskill.devskill_api.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class SyncRepoController {

    @Autowired
    private RepositorySyncService repositorySyncService;

    @Autowired
    private RepoService repoService;

    @Autowired
    private Utils utils;

    private static final Logger logger = LoggerFactory.getLogger(frontController.class);

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
        // Read repository names from JSON
        List<String> repositories = repositorySyncService.readRepositoryNamesFromJson();
        executeSync(files,megabyte, repositories, false);

        return "The process of syncing the repositories data has been started.";
    }

    private void executeSync(int files, long megabyte, List<String> repositories, boolean isTrending) throws Exception {
        // Create a background executor to manage the task
        ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();

        AtomicBoolean trending = new AtomicBoolean(isTrending);

        List<String> trendingRepositories;

        if(!isTrending){
            trendingRepositories =  repoService.getTrendingRepositories();
        } else {
            trendingRepositories = new ArrayList<String>();
        }


// Submit the task to the background executor
        backgroundExecutor.submit(() -> {
            try {
                // Create a fixed thread pool with 5 threads for parallel processing
                ExecutorService executorService = Executors.newFixedThreadPool(5);

                // Store results in a thread-safe map
                Map<String, Object> results = new ConcurrentHashMap<>();

                // Submit tasks for each repository
                List<Future<?>> futures = new ArrayList<>();
                for (String repo : repositories) {

                   if (!trendingRepositories.isEmpty() && trendingRepositories.contains(repo)){
                       trending.set(true);
                   }else {
                       trending.set(false);
                   }

                    futures.add(executorService.submit(() -> {
                        try {
                            // Process each repository and store results
                            Map<String, Object> syncData = repositorySyncService.syncRepositoryData(repo, files, megabyte, trending.get());
                            results.put(repo, syncData);
                        } catch (Exception e) {
                            results.put(repo, "Error: " + e.getMessage());
                        }
                    }));
                }

                // Wait for all tasks to complete
                for (Future<?> future : futures) {
                    future.get(); // Blocks until each task is finished
                }

                logger.info("Results: {}", results);

                // Shutdown the executor service after completing all tasks
                executorService.shutdown();

            } catch (Exception e) {
                logger.info(e.getMessage());
            } finally {
                backgroundExecutor.shutdown();
            }
        });
    }


    @GetMapping("/syncTrendingRepositories")
    public String syncTrendingRepositories(@RequestParam(defaultValue = "1") int files, @RequestParam(defaultValue = "1") long megabyte) throws Exception {
       List<String> trendingRepositories =  repoService.getTrendingRepositories();

        executeSync(files,megabyte, trendingRepositories, true);

        return "The process of syncing the trending repositories data has been started.";

    }
}
