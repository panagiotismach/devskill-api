package com.devskill.devskill_api.utils;

import org.springframework.stereotype.Component;

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
        // A file change line must contain at least 3 parts: insertions, deletions, and file path

        String[] parts = line.trim().split("\\s+");
        return parts.length >= 3 && isNumeric(parts[0]) && isNumeric(parts[1]);
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

    public String getFileExtension(String filePath) {
        String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
        return fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "unknown";
    }



}
