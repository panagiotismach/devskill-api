package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.ExtensionDTO;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.models.TrendingRepository;
import com.devskill.devskill_api.repository.ContributionRepository;
import com.devskill.devskill_api.repository.TrendingRepositoryRepository;
import com.devskill.devskill_api.services.ContributorsService;
import com.devskill.devskill_api.services.ExtensionService;
import com.devskill.devskill_api.services.RepoService;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class UiController {

    @Autowired
    private TrendingRepositoryRepository trendingRepositoryRepository;

    @Autowired
    private RepoService repoService;

    @Autowired
    private ContributorsService contributorsService;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private ExtensionService extensionService;


    @Autowired
    private Utils utils;

    @GetMapping("/retrieveRepositories")
    public ResponseEntity<?> retrieveRepositories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            Map<String, Object> response = repoService.retrieveRepositories(page,size);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveContributorsForRepository")
    public ResponseEntity<?> retrieveContributorsForRepository(
            @RequestParam Long repoId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            if (repoId == null) {
                throw new IllegalArgumentException("Repository id must be provided");
            }

            Map<String, Object> response = contributorsService.retrieveContributorsForRepository(repoId,page,size);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveContributors")
    public ResponseEntity<?> retrieveContributors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            Map<String, Object> response = contributorsService.findContributors(page,size);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveRepository")
    public ResponseEntity<?> retrieveRepository(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            if ((name == null || name.isEmpty()) || (url == null || url.isEmpty())) {
                throw new IllegalArgumentException("Either 'name' and 'url' must be provided");
            }

            Map<String, Object> response = repoService.findByRepoNameOrRepoUrl(name,url,page,size);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveContributorsRepositories")
    public ResponseEntity<?> retrieveContributorsRepositories(
            @RequestParam Long conId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            if (conId == null) {
                throw new IllegalArgumentException("Contributor id must be provided");
            }

            Pageable pageable = PageRequest.of(page, size);

            // Retrieve repositories with pagination
            Page<RepositoryEntity> repositoryPage = contributorsService.findRepositoriesByContributor(conId, pageable);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(repositoryPage);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveContributor")
    public ResponseEntity<?> retrieveContributor(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            if ((name == null || name.isEmpty()) && (username == null || username.isEmpty())) {
                throw new IllegalArgumentException("Either 'name' and 'username' must be provided");
            }

            // Customize the response to include metadata
            Map<String, Object> response = contributorsService.findByGithubUsernameOrFullName(username,name,page,size);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveTrendingRepositories")
    public ResponseEntity<?> retrieveTrendingRepositories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {

            Pageable pageable = PageRequest.of(page, size);

            // Retrieve repositories with pagination
            Page<TrendingRepository> trendingRepositoryPage = trendingRepositoryRepository.findAll(pageable);

           Page<RepositoryEntity> trendingRepositories = trendingRepositoryPage.map(TrendingRepository::getRepository);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(trendingRepositories);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @PostMapping("/retrieveFilteredContributors")
    public ResponseEntity<?> retrieveFilteredContributors(
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String language = (String) requestBody.get("language");

            if(language == null){
                throw new IllegalArgumentException("Language must be provided");
            }

            Pageable pageable = PageRequest.of(page, size);

            // Retrieve repositories with pagination
            Page<Contributor> contributorsPage = contributionRepository.findTopContributorsByLanguage(language, pageable);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(contributorsPage);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @PostMapping("/retrieveFilteredRepositories")
    public ResponseEntity<?> retrieveFilteredRepositories(
            @RequestBody Map<String, Object> requestBody,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            String language = (String) requestBody.get("language");

            if(language == null){
                throw new IllegalArgumentException("Language must be provided");
            }

            Pageable pageable = PageRequest.of(page, size);

            // Retrieve repositories with pagination
            Page<RepositoryEntity> repositoryPage = repoService.getRepositoriesWithInsertionsAndDeletionsByLanguage(language, pageable);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(repositoryPage);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/findTop5MostUsedExtensions")
    public ResponseEntity<?> findTop5MostUsedExtensions( @RequestParam(required = false) Set<String> extensions) {
        try {


            List<Map.Entry<String, Long>> e = repoService.findTop5MostUsedExtensions(extensions);

            return ResponseEntity.ok(e); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/findAllExtensions")
    public ResponseEntity<?> findAllExtensions( @RequestParam(required = false) Set<String> extensions) {
        try {


            List<Map.Entry<String, Long>> e = repoService.findAllExtensions(extensions, null);

            return ResponseEntity.ok(e.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())); 
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/findTopContributors")
    public ResponseEntity<?> findTopContributors() {
        try {


            List<Object[]> contributors = contributorsService.findTopContributors();

            return ResponseEntity.ok(contributors); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/findTop5RepositoriesBasedOnContributors")
    public ResponseEntity<?> findTopRepositoriesBasedOnContributors() {
        try {

            List<Object[]> repositories = repoService.findTopRepositories(5);

            return ResponseEntity.ok(repositories); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/findContributionsPerContributor")
    public ResponseEntity<?> findContributionsPerContributor(@RequestParam Long conId) {
        try {

            List<Object[]> contributions = contributorsService.findContributionsPerContributor(conId);

            return ResponseEntity.ok(contributions); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveExtensions")
    public ResponseEntity<?> getExtensions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "repoCount") String type,
            @RequestParam(defaultValue = "desc") String function) {
        
        if(!type.equals("repoCount") && !type.equals("lastUsed")){
            type = "repoCount";
        }

        // Create Pageable with default sorting if function is invalid
        Pageable pageable;
        if (function.equals("asc")) {
            pageable = PageRequest.of(page, size, Sort.by(type).ascending());
        } else if (function.equals("desc")) {
            pageable = PageRequest.of(page, size, Sort.by(type).descending());
        } else {
            // Default to descending order for invalid function values (e.g., "alter")
            pageable = PageRequest.of(page, size, Sort.by(type).descending());
        }
        
        return ResponseEntity.ok(extensionService.getExtensions(pageable));
    }

    @PostMapping("/retrieveFilteredExtensions")
    public ResponseEntity<?> getFilteredExtensions(
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String extensionName = request.get("extension");
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(extensionService.getFilteredExtensions(extensionName, pageable));
    }



}