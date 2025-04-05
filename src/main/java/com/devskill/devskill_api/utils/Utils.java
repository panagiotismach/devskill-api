package com.devskill.devskill_api.utils;

import com.devskill.devskill_api.models.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Component
public class Utils {

    public Utils() {}

    public Path getPathOfRepository(String repoName){
        // Specify the path to the repositories folder
        Path repositoryPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(repositoryPath) || !Files.isDirectory(repositoryPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + repositoryPath);
        }

        return repositoryPath;
    }

    public Path getPathOfRepositories(String name){

        // Path to the repositories folder
        Path outputDir = Path.of(General.OUTPUT_FOLDER.getText());

        // Check if the 'output' directory exists
        if (!Files.exists(outputDir) || !Files.isDirectory(outputDir)) {
            throw new IllegalArgumentException("Output folder not found: " + outputDir);
        }

        // Path to the requested file in the 'output' folder
        Path repositoryFile = outputDir.resolve(name);

        // Check if the file exists
        if (!Files.exists(repositoryFile) || Files.isDirectory(repositoryFile)) {
            throw new IllegalArgumentException("Repository file not found: " + repositoryFile);
        }

        return repositoryFile;
    }

    public Path getPath(String repoName){

        String name;
        Path repositoryPath;

        if (repoName.startsWith(General.GITHUB.getText())) {
            name = extractRepoNameFromUrl(repoName);
        } else {
            name = repoName;
        }

        // Replace all '/' with '-' in the repoName
        name = name.replace("/", "-");

        repositoryPath = Path.of(General.REPOS_FOLDER.getText(), name);

        return repositoryPath;
    }

    public String extractRepoNameFromUrl(String url) {
        // Remove the ".git" suffix if it exists
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }

        // Handle SSH format
        if (url.startsWith("git@")) {
            // Remove everything up to the first colon
            url = url.substring(url.indexOf(":") + 1); // This will give "organization/repo"
        } else if (url.startsWith("https://")) {
            // Remove the 'https://github.com/' part
            url = url.substring(url.indexOf("github.com/") + "github.com/".length()); // This will give "organization/repo"
        } else if (url.startsWith("http://")) {
            // Similar for HTTP URLs
            url = url.substring(url.indexOf("github.com/") + "github.com/".length());
        }

        // Split the URL and get the organization and repository name
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            String organization = parts[0];
            String repository = parts[1];
            return organization + "/" + repository; // Return in "organization/repo" format
        }

        throw new IllegalArgumentException("Invalid repository URL format: " + url);
    }

    public boolean isFileChangeLine(String line) {
        String[] parts = line.trim().split("\\s+");

        // A valid file change line must have exactly 3 parts
        if (parts.length != 3) {
            return false;
        }

        // Check if the first two parts are numeric
        if (!isNumeric(parts[0]) || !isNumeric(parts[1])) {
            return false;
        }

        // Parse insertions and deletions
        int insertions = Integer.parseInt(parts[0]);
        int deletions = Integer.parseInt(parts[1]);

        // Both insertions and deletions must not be zero
        if (insertions == 0 && deletions == 0) {
            return false;
        }

        // The third part (file path) must be non-empty
        return !parts[2].isEmpty();
    }

    public boolean isNumeric(String str) {
        return str.matches("\\d+");
    }

    public LocalDate parseDate(String dateStr) {
        try {

            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public int executeGitFileCount(Path repositoryPath) throws IOException, InterruptedException {

        String command = "git -C " + repositoryPath.toString() + " ls-files";

        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        Process process = processBuilder.start();

        int fileCount = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                fileCount++;  // Each line represents a file
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed with exit code: " + exitCode);
        }

        return fileCount;
    }

    public long getGitRepoSizeInMB(Path repositoryPath) throws IOException {
        // Use an array or other mutable object to hold the total size
        final long[] totalSizeInBytes = {0}; // Array to hold the total size value

        // Walk the file tree starting from the repository path
        Files.walkFileTree(repositoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // Add the size of the current file to the total size
                totalSizeInBytes[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // Handle the case where a file can't be accessed (e.g., permission denied)
                return FileVisitResult.CONTINUE;
            }
        });

        // Convert bytes to MB and return
        return totalSizeInBytes[0] / (1024 * 1024);
    }

    public String getFileExtension(String filePath) throws Exception {

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new Exception("The file path is empty"); // Return empty for null or empty input
        }

        // Normalize file path to use '/' as the directory separator
        String normalizedPath = filePath.replace("\\", "/").trim();

        // Extract the file name after the last '/'
        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        String fileName = (lastSlashIndex >= 0)
                ? normalizedPath.substring(lastSlashIndex + 1)
                : normalizedPath;

        // Special case: if the file starts with a dot and has no other dots, return the whole name
        if (fileName.startsWith(".") && fileName.lastIndexOf('.') == 0) {
            return fileName;
        }

        // Find the last dot in the file name
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            // Return the substring after the last dot
            return fileName.substring(lastDotIndex + 1);
        }

        // If no valid extension, return empty string
        return "noExtension";
    }

    public Map<String, Object> constructPageResponse(Page<?> pageObject) {

        Map<String, Object> response = new HashMap<>();

        // Add a content-specific key based on the content type
        if (!pageObject.getContent().isEmpty()) {
            Object firstItem = pageObject.getContent().getFirst();
            if (firstItem instanceof RepositoryEntity || firstItem instanceof TrendingRepository) {
                response.put("repositories", pageObject.getContent());
            } else if (firstItem instanceof Contributor) {
                response.put("contributors", pageObject.getContent());
            } else if (firstItem instanceof Extension || firstItem instanceof ExtensionDTO){
                response.put("extensions", pageObject.getContent());
            }
            else {
                response.put("items", pageObject.getContent());
            }
        } else {
            response.put("items", pageObject.getContent()); // Default for empty content
        }

        response.put("currentPage", pageObject.getNumber());
        response.put("totalItems", pageObject.getTotalElements());
        response.put("totalPages", pageObject.getTotalPages());
        response.put("pageSize", pageObject.getSize());

        return response;
    }
}
