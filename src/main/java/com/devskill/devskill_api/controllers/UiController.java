package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.ContributionRepository;
import com.devskill.devskill_api.repository.ContributorRepository;
import com.devskill.devskill_api.repository.RepositoryRepository;
import com.devskill.devskill_api.services.ContributorsService;
import com.devskill.devskill_api.services.RepoService;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class UiController {

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private RepoService repoService;

    @Autowired
    private ContributorsService contributorsService;

    @Autowired
    private ContributionRepository contributionRepository;

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
            Page<RepositoryEntity> repositoryPage = repositoryRepository.findAllByTrending(true, pageable);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(repositoryPage);

            return ResponseEntity.ok(response); // Return 200 OK with the paginated results
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format("Internal Server Error: %s", e.getMessage())); // Return 500 Internal Server Error
        }
    }

    @GetMapping("/retrieveFilteredContributors")
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


}
