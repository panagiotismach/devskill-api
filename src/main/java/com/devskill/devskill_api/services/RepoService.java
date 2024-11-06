package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.RepositoryRepository;
import com.devskill.devskill_api.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            throw new RuntimeException("Error saving JSON response: " + e.getMessage());
        }

        return response;
    }

    public Map<String, Integer> findUniqueExtensions() {
        Path folderPath = Paths.get("repos");
        File projectDirectory = new File(String.valueOf(folderPath));
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
                .limit(4)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existingValue, newValue) -> existingValue,
                        LinkedHashMap::new
                ));
    }

    public RepositoryEntity getRepoDetails(String repoName) throws IOException, InterruptedException, ResponseStatusException {

        Path repositoryPath = utils.getPathOfRepository(repoName);

        // Build the command to run the git command
        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", repositoryPath.toString(), "config", "--get", "remote.origin.url");
        processBuilder.redirectErrorStream(true);  // Combine stdout and stderr

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process
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
            throw new IOException("Error occurred while executing git command, exit code: " + exitCode);
        }

        // Extract the repository name from the URL
        String extractedRepoName = utils.extractRepoNameFromUrl(repoUrl);

        // Call the saveRepository method to save or update the repository details
        return saveRepository(extractedRepoName, repoUrl);
    }

    private RepositoryEntity saveRepository(String repoName, String repoUrl) {

        // Check if a repository exists by both repoName and repoUrl
        Optional<RepositoryEntity> existingRepo = repositoryRepository.findByRepoNameIgnoreCaseAndRepoUrlIgnoreCase(repoName, repoUrl);

        if (existingRepo.isPresent()) {
            return existingRepo.get();
        }

        // Create a new repository entity
        RepositoryEntity repositoryEntity = new RepositoryEntity(repoName, repoUrl);

        return repositoryRepository.save(repositoryEntity); // Save the new entity and return it
    }

}
