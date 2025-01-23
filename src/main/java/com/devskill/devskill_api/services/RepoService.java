package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.RepositoryRepository;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RepoService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private Utils utils;

    public Map<String, Object> getRepositories(String name) {
        Path filePath = Paths.get("files", name);  // 'files' is the folder name

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
        Path folderPath = Paths.get("files");
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


    private Map<String, Integer> findUniqueExtensions(Path repositoryPath) {
        File projectDirectory = new File(String.valueOf(repositoryPath));
        Stack<File> stack = new Stack<>();
        Map<String, Integer> fileExtensionCount = new HashMap<>();

        if (projectDirectory.exists() && projectDirectory.isDirectory()) {
            stack.push(projectDirectory);

            while (!stack.isEmpty()) {
                File currentFile = stack.pop();
                File[] filesInDirectory = currentFile.listFiles();

                if (filesInDirectory != null) {
                    for (File file : filesInDirectory) {
                        if (file.isDirectory()) {
                            stack.push(file);
                        } else {
                            String fileName = file.getName();
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

        return fileExtensionCount.entrySet()
                .stream()
                .sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingValue, newValue) -> existingValue,
                        LinkedHashMap::new
                ));
    }

    public RepositoryEntity getRepoDetails(Path repositoryPath, boolean isTrending) throws Exception {

       String repoUrl = retrieveRepoUrl(repositoryPath);

       if(repoUrl.isEmpty()){
         throw new Exception("The url is empty");
       }

       LocalDate lastCommitDate = retrieveLastCommitDate(repositoryPath);

        String extractedRepoName = utils.extractRepoNameFromUrl(repoUrl);

        Optional<RepositoryEntity> existingRepo = repositoryRepository.findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(extractedRepoName, repoUrl);

        if (existingRepo.isPresent()) {
          RepositoryEntity  repo = existingRepo.get();
          repo.setLast_commit_date(lastCommitDate);
          List<String> extensions = findUniqueExtensions(repositoryPath).keySet().stream().toList();
          repo.setExtensions(extensions);
          repositoryRepository.save(repo);
          return repo;
        }

        LocalDate creationDate = retrieveCreationDate(repositoryPath);

        List<String> extensions = findUniqueExtensions(repositoryPath).keySet().stream().toList();

        // Create and save a new RepositoryEntity with the relevant details
        RepositoryEntity repositoryEntity = new RepositoryEntity(extractedRepoName, repoUrl,creationDate,lastCommitDate, extensions, isTrending);

        // Save the repository entity and return it
        return repositoryRepository.save(repositoryEntity);
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
                String url = "https://github.com/trending";

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
}

