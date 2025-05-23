package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.ExtensionDTO;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.models.TrendingRepository;
import com.devskill.devskill_api.repository.*;
import com.devskill.devskill_api.utils.General;
import com.devskill.devskill_api.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RepoService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private TrendingRepositoryRepository trendingRepositoryRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private ContributorRepositoryRepository contributorRepositoryRepository;

    @Autowired
    private ExtensionService extensionService;

    @Autowired
    private Utils utils;

    public Map<String, Object> getRepositories(String name) {
        Path filePath = Paths.get(General.FILES_FOLDER.getText(), name);  // 'files' is the folder name

        Set<String> uniqueRepoNames = new HashSet<>();
        int lineCount = 0;
        int repoCount = 0;
        int validJsonObjectCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                try {
                    JsonNode jsonObject = objectMapper.readTree(line);
                    validJsonObjectCount++;

                    if (jsonObject.has("repo") && jsonObject.get("repo").isObject()) {
                        JsonNode repoObject = jsonObject.get("repo");
                        if (repoObject.has("name")) {
                            String repoName = repoObject.get("name").asText();
                            repoCount++;
                            uniqueRepoNames.add(repoName);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Invalid JSON on line " + lineCount + ": " + e.getMessage());
                }
            }

            ArrayNode responseArray = objectMapper.createArrayNode();
            uniqueRepoNames.forEach(responseArray::add);

            return Map.of(
                    "lineCount", lineCount,
                    "reposCount", repoCount,
                    "validJsonObjectCount", validJsonObjectCount,
                    "uniqueRepoNames", responseArray,
                    "uniqueCount", uniqueRepoNames.size()
            );
        } catch (IOException e) {
            throw new RuntimeException("Error reading or parsing file: " + e.getMessage());
        }
    }

    public Map<String, Object> processFiles() {
        Path folderPath = Paths.get(General.FILES_FOLDER.getText());
        Set<String> uniqueRepoNames = new TreeSet<>();
        int totalLineCount = 0;
        int totalRepoCount = 0;
        int totalValidJsonCount = 0;

        File folder = folderPath.toFile();
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            throw new RuntimeException("No files found in the directory.");
        }

        for (File file : files) {
            if (file.isFile() && file.length() > 0) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        totalLineCount++;
                        try {
                            JsonNode jsonObject = objectMapper.readTree(line);
                            totalValidJsonCount++;
                            if (jsonObject.has("repo") && jsonObject.get("repo").isObject()) {
                                JsonNode repoObject = jsonObject.get("repo");
                                if (repoObject.has("name")) {
                                    String repoName = repoObject.get("name").asText();
                                    totalRepoCount++;
                                    uniqueRepoNames.add(repoName);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing JSON in file " + file.getName() + ": " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Error reading file " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        ArrayNode responseArray = objectMapper.createArrayNode();
        uniqueRepoNames.forEach(responseArray::add);

        Map<String, Object> response = Map.of(
                "totalLineCount", totalLineCount,
                "totalRepoCount", totalRepoCount,
                "totalValidJsonCount", totalValidJsonCount,
                "uniqueRepoNames", responseArray,
                "uniqueRepoCount", uniqueRepoNames.size()
        );

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("output.json"), response);
        } catch (IOException e) {
            throw new RuntimeException(STR."Error saving JSON response: \{e.getMessage()}");
        }

        return response;
    }


    private List<ExtensionDTO> findUniqueExtensions(Path repositoryPath) {
        File projectDirectory = new File(String.valueOf(repositoryPath));
        Stack<File> stack = new Stack<>();
        Map<String, Integer> fileExtensionCount = new HashMap<>();

        if (projectDirectory.exists() && projectDirectory.isDirectory()) {
            stack.push(projectDirectory);

            while (!stack.isEmpty()) {
                File currentFile = stack.pop();
                File[] filesInDirectory = currentFile.listFiles();

                if (filesInDirectory != null && !currentFile.isHidden()) {
                    for (File file : filesInDirectory) {
                        if (file.isDirectory()) {
                            stack.push(file);
                        } else {
                            String fileName = file.getName();
                            if(fileName.equals("pack")){
                                System.out.println(fileName);
                            }
                            int lastDotIndex = fileName.lastIndexOf('.');
                            if (lastDotIndex != -1) {
                                String fileExtension = fileName.substring(lastDotIndex + 1).toLowerCase();
                                fileExtensionCount.put(fileExtension, fileExtensionCount.getOrDefault(fileExtension, 0) + 1);
                            }
                        }
                    }
                }
            }
        }

        List<ExtensionDTO> result = new ArrayList<>();
        for (String ext : fileExtensionCount.keySet()) {
            int fileCount = fileExtensionCount.get(ext);
            long lastModifiedMillis = getLastGitCommitTimestampForExtension(repositoryPath,ext);
            LocalDate lastUsedDate = Instant.ofEpochMilli(lastModifiedMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Placeholder for language list. You can map it using a predefined map.
            List<String> language = Collections.emptyList();

            ExtensionDTO dto = new ExtensionDTO(
                    ext,
                    language,
                    fileCount,
                    1, // repoCount = 1 since this method works per repository
                    lastUsedDate
            );
            result.add(dto);
        }

        result.sort(Comparator.comparingInt(ExtensionDTO::getFileCount).reversed());
        return result;
    }

    private long getLastGitCommitTimestampForExtension(Path repoPath, String extension) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "git", "log", "-1", "--pretty=format:%ct", "--", "*." + extension
            );
            builder.directory(repoPath.toFile());
            builder.redirectErrorStream(true);

            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                if (output != null && !output.isEmpty()) {
                    long epochSeconds = Long.parseLong(output.trim());
                    return epochSeconds * 1000; // convert to milliseconds
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Log if needed
        }
        return 0L; // Fallback if git fails
    }

    public Map<String, Object> getRepoDetails(Path repositoryPath, boolean isTrending) throws Exception {

       String repoUrl = retrieveRepoUrl(repositoryPath);

       boolean isExisted = false;

        Map<String, Object> results = new HashMap<>();

       if(repoUrl.isEmpty()){
         throw new Exception("The url is empty");
       }

       LocalDate lastCommitDate = retrieveLastCommitDate(repositoryPath);

        String extractedRepoName = utils.extractRepoNameFromUrl(repoUrl);

        Optional<RepositoryEntity> existingRepo = repositoryRepository.findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(extractedRepoName, repoUrl);

        if (existingRepo.isPresent()) {
            isExisted =  true;
          RepositoryEntity  repo = existingRepo.get();
          LocalDate preLastCommitDate = repo.getLast_commit_date();
          repo.setLast_commit_date(lastCommitDate);
          List<ExtensionDTO> extensions = findUniqueExtensions(repositoryPath);
          repo.setExtensions(extensions.stream().map(ExtensionDTO::getName).toList());
          repositoryRepository.save(repo);
            if(isTrending){
                TrendingRepository trendingRepository = new TrendingRepository(repo);
                trendingRepositoryRepository.save(trendingRepository);
            }
            results.put("repository", repo);
            results.put("isExisted", isExisted);
            results.put("preLastCommitDate", preLastCommitDate);
          return results;
        }

        LocalDate creationDate = retrieveCreationDate(repositoryPath);

        List<ExtensionDTO> extensions = findUniqueExtensions(repositoryPath);

        // Create and save a new RepositoryEntity with the relevant details
        RepositoryEntity repositoryEntity = new RepositoryEntity(extractedRepoName, repoUrl,creationDate,lastCommitDate, extensions.stream().map(ExtensionDTO::getName).toList());

        RepositoryEntity persistedRepository = repositoryRepository.save(repositoryEntity);

        if(isTrending){
            TrendingRepository trendingRepository = new TrendingRepository(repositoryEntity);
            trendingRepositoryRepository.save(trendingRepository);
        }
        

        extensionService.updateExtensionTable(isExisted, extensions);

        results.put("repository", persistedRepository);
        results.put("isExisted", isExisted);

        return results;
    }

    private String retrieveRepoUrl(Path repositoryPath) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", repositoryPath.toString(), "config", "--get", "remote.origin.url");
        processBuilder.redirectErrorStream(true);  // Combine stdout and stderr

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process (repo URL)
        String repoUrl = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            if ((line = reader.readLine()) != null) {
                repoUrl = line.trim(); // Assuming only one URL will be returned
            }
        }

        // Wait for the process to finish
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException(STR."Error occurred while executing git command, exit code: \{exitCode}");
        }

        return repoUrl;
    }

    private LocalDate retrieveCreationDate(Path repositoryPath) throws IOException {

        ProcessBuilder firstCommitDateProcessBuilder = new ProcessBuilder(
                "git", "-C", repositoryPath.toString(), "log", "--pretty=format:%cd", "--date=short", "--reverse"
        );
        firstCommitDateProcessBuilder.redirectErrorStream(true);

        LocalDate firstCommitDate;


        Process firstCommitDateProcess = firstCommitDateProcessBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(firstCommitDateProcess.getInputStream()))) {

            String commitDate = reader.readLine();
            if (commitDate == null || commitDate.isEmpty()) {
                throw new IOException("Failed to retrieve any commits.");
            }

            firstCommitDate = utils.parseDate(commitDate.trim());
        }


        return firstCommitDate;
    }

        private LocalDate retrieveLastCommitDate (Path repositoryPath) throws IOException, InterruptedException {

            ProcessBuilder lastCommitDateProcessBuilder = new ProcessBuilder(
                    "git", "-C", repositoryPath.toString(), "log", "-1", "--pretty=format:%cd", "--date=short"
            );
            lastCommitDateProcessBuilder.redirectErrorStream(true);

            LocalDate lastCommitDate;


            Process lastCommitDateProcess = lastCommitDateProcessBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(lastCommitDateProcess.getInputStream()))) {
                lastCommitDate = utils.parseDate(reader.readLine().trim());
            }
            if (lastCommitDateProcess.waitFor() != 0) {
                throw new IOException("Failed to retrieve the last commit date.");
            }

            return lastCommitDate;
        }

        public List<String> getTrendingRepositories() throws Exception {
            try {
                String url = General.GITHUB_TRENDING.getText();

                Document document = Jsoup.connect(url).get();

                // List to store repository names
                List<String> repositoryNames = new ArrayList<>();

                for (Element repoElement : document.select("article.Box-row")) {
                    String repoName = repoElement.select("h2").text().replace(" ", "").replace("\n", "");
                    repositoryNames.add(repoName);
                }


                return repositoryNames;

            } catch (Exception e) {

                throw new Exception(e);
            }
        }

        public Map<String, Object> retrieveRepositories(int page, int size){

            Pageable pageable = PageRequest.of(page, size);

            // Retrieve repositories with pagination
            Page<RepositoryEntity> repositoryPage = repositoryRepository.findAll(pageable);

            // Customize the response to include metadata
            Map<String, Object> response = utils.constructPageResponse(repositoryPage);

            return response;
        }

        public Map<String,Object> findByRepoNameOrRepoUrl(String name, String url, int page, int size){

            Pageable pageable = PageRequest.of(page, size);

            Page<RepositoryEntity> repositoryPage = repositoryRepository.findByRepoNameOrRepoUrl(name,url, pageable);

            Map<String, Object> response = utils.constructPageResponse(repositoryPage);

            return response;
        }

    public Page<RepositoryEntity> getRepositoriesWithInsertionsAndDeletionsByLanguage(String languageExtension, Pageable pageable) {
        return contributionRepository.findRepositoriesWithInsertionsAndDeletionsByLanguage(languageExtension, pageable);
    }

    public List<Map.Entry<String, Long>> findAllExtensions(Set<String> allowedExtensions,Integer limit) {
        List<RepositoryEntity> repositories = repositoryRepository.findAll(); // Fetch all repositories

        // Flatten extensions list and count occurrences
        Stream<String> extensionsStream = repositories.stream()
                .flatMap(repo -> repo.getExtensions().stream()); // Flatten the list of extensions

        // Apply the filter only if allowedExtensions is not null
        if (allowedExtensions != null) {
            extensionsStream = extensionsStream.filter(allowedExtensions::contains); // Filter extensions if provided
        }

        // Count occurrences
        Map<String, Long> extensionCount = extensionsStream
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Sort by count descending and take top 5
        Stream<Map.Entry<String, Long>> sortedStream = extensionCount.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()));

        return (limit != null) ? sortedStream.limit(limit).toList() : sortedStream.toList();
    }

    public List<Map.Entry<String, Long>> findTop5MostUsedExtensions(Set<String> allowedExtensions) {
       return this.findAllExtensions(allowedExtensions, 5);
    }

    public List<Object[]> findTopRepositories(int pageSize) {
        return contributorRepositoryRepository.findTopRepositories(PageRequest.of(0, pageSize));
    }

    public RepositoryEntity findFirstByOrderByCreation_dateDesc(){
        return repositoryRepository.findFirstByOrderByCreationDateDesc();
    }



}