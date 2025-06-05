package com.devskill.devskill_api.controllers;

import com.devskill.devskill_api.models.Extension;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.ContributionRepository;
import com.devskill.devskill_api.repository.ContributorRepository;
import com.devskill.devskill_api.repository.ExtensionRepository;
import com.devskill.devskill_api.repository.RepositoryRepository;
import com.devskill.devskill_api.services.*;
import com.devskill.devskill_api.utils.General;
import com.devskill.devskill_api.utils.Utils;
import com.opencsv.CSVWriter;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class SyncRepoController {

    @Autowired
    private RepositorySyncService repositorySyncService;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ExtensionRepository extensionRepository;

    @Autowired
    private ExtensionService extensionService;

    @Autowired
    private ContributorsService contributorsService;

    @Autowired
    private ContributionRepository contributionRepository;

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

        if (!utils.checkPeriodSync(from, to)) {
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

        if (!utils.checkPeriodSync(from, to)) {
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

    @GetMapping("/csv/repositories")
    public ResponseEntity<Resource> exportRepositoriesCsv() throws Exception {
        List<RepositoryEntity> repositories = repositoryRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write header
            writer.writeNext(new String[]{"repository_name", "creation_date", "last_commit_date", "extensions_number", "extensions", "committers_number", "committers"});

            // Write data
            for (RepositoryEntity repo : repositories) {
                String extensions = repo.getExtensions() != null ? String.join(";", repo.getExtensions()) : "";
                String committers = repo.getContributorRepositories().stream()
                        .map(cr -> cr.getContributor().getGithubUsername())
                        .collect(Collectors.joining(";"));
                writer.writeNext(new String[]{
                        repo.getRepoName(),
                        repo.getCreationDate() != null ? repo.getCreationDate().toString() : "",
                        repo.getLast_commit_date() != null ? repo.getLast_commit_date().toString() : "",
                        String.valueOf(repo.getExtensions() != null ? repo.getExtensions().size() : 0),
                        extensions,
                        String.valueOf(repo.getContributorRepositories().size()),
                        committers
                });
            }
        }

        ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repositories.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/csv/repositories-languages")
    public ResponseEntity<Resource> exportRepositoriesLanguagesCsv() throws Exception {
        List<RepositoryEntity> repositories = repositoryRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write header
            writer.writeNext(new String[]{"repository_name", "creation_date", "last_commit_date", "language_number", "languages", "committers_number", "committers"});

            // Write data
            for (RepositoryEntity repo : repositories) {
                // Convert extensions to languages
                List<String> extensions = repo.getExtensions() != null ? repo.getExtensions() : List.of();
                Set<String> languages = new HashSet<>();
                boolean hasOther = false;

                for (String ext : extensions) {
                    List<String> mappedLanguages = extensionService.getLanguages(ext.toLowerCase());
                    if (mappedLanguages != null && !mappedLanguages.isEmpty() && !mappedLanguages.contains("other")) {
                        languages.add(mappedLanguages.size() == 1 ? mappedLanguages.getFirst() : ext);
                    } else {
                        hasOther = true;
                    }
                }

                // Add "other" only once if any extensions mapped to it
                if (hasOther) {
                    languages.add("other");
                }

                String languagesStr = languages.isEmpty() ? "" : String.join(";", languages);
                String committers = repo.getContributorRepositories().stream()
                        .map(cr -> cr.getContributor().getGithubUsername())
                        .collect(Collectors.joining(";"));

                writer.writeNext(new String[]{
                        repo.getRepoName(),
                        repo.getCreationDate() != null ? repo.getCreationDate().toString() : "",
                        repo.getLast_commit_date() != null ? repo.getLast_commit_date().toString() : "",
                        String.valueOf(languages.size()),
                        languagesStr,
                        String.valueOf(repo.getContributorRepositories().size()),
                        committers
                });
            }
        }

        ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repositories-languages.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/csv/extensions")
    public ResponseEntity<Resource> exportExtensionsCsv() throws Exception {
        List<Extension> extensions = extensionRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Write header
            writer.writeNext(new String[]{"extension","language", "file_counts", "repositories_count", "most_recent_use"});

            // Write data
            for (Extension ext : extensions) {
                String languages = ext.getLanguage() != null ? String.join(";", ext.getLanguage()) : "";
                String extension = ext.getExtensionName();
                writer.writeNext(new String[]{
                        extension,
                        languages,
                        String.valueOf(ext.getFileCount()),
                        String.valueOf(ext.getRepoCount()),
                        ext.getLastUsed() != null ? ext.getLastUsed().toString() : ""
                });
            }
        }

        ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=extensions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping("/csv/committer-contributions")
    public ResponseEntity<?> getCommitterContributionsCsv() {
        try {
            List<Object[]> contributions = contributionRepository.findAllContributions();

            System.out.println(contributions);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                csvWriter.writeNext(new String[]{"committer", "extension", "total", "insert", "delete"});
                contributions
                        .forEach(record -> {
                            String committer = record[0] != null ? record[0].toString() : "";
                            String extension = record[1] != null ? record[1].toString() : "";
                            Long insertions = record[2] != null ? ((Number) record[2]).longValue() : 0L;
                            Long deletions = record[3] != null ? ((Number) record[3]).longValue() : 0L;
                            Long total = insertions + deletions;
                            csvWriter.writeNext(new String[]{
                                    committer,
                                    extension,
                                    String.valueOf(total),
                                    String.valueOf(insertions),
                                    String.valueOf(deletions)
                            });
                        });
            }

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=committer-contributions.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(out.size())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }

    @GetMapping("/csv/committer-contributions-languages")
    public ResponseEntity<?> getCommitterContributionsLanguagesCsv() {
        try {
            List<Object[]> contributions = contributionRepository.findAllContributions();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
                csvWriter.writeNext(new String[]{"committer", "extension", "languages", "total", "insert", "delete"});
                contributions
                        .forEach(record -> {
                            String committer = record[0] != null ? record[0].toString() : "";
                            String extension = record[1] != null ? record[1].toString() : "";
                            List<String> languages = extensionService.getLanguages(extension.toLowerCase());
                            String languageStr = (languages != null && !languages.isEmpty() && !languages.contains("other"))
                                    ? String.join(";", languages)
                                    : "other";
                            Long insertions = record[2] != null ? ((Number) record[2]).longValue() : 0L;
                            Long deletions = record[3] != null ? ((Number) record[3]).longValue() : 0L;
                            Long total = insertions + deletions;
                            csvWriter.writeNext(new String[]{
                                    committer,
                                    extension,
                                    languageStr,
                                    String.valueOf(total),
                                    String.valueOf(insertions),
                                    String.valueOf(deletions)
                            });
                        });
            }

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=committer-contributions-languages.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .contentLength(out.size())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Internal Server Error: " + e.getMessage());
        }
    }
}
