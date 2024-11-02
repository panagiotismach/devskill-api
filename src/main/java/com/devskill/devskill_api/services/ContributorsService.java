package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.repository.ContributorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContributorsService {

    @Autowired
    private ContributorRepository contributorRepository;

   public ContributorsService (){

   }


    private List<String> fetchRawContributors(String repoName) throws Exception {
        List<String> contributors = new ArrayList<>();
        Path folderPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + folderPath);
        }

        // Build the command to run the git log command
        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", folderPath.toString(), "log", "--pretty=%an <%ae>");

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contributors.add(line.trim());
            }
        }

        return contributors;
    }

    public List<Contributor> getCommitsAndContributors(String repoName) throws IOException, InterruptedException {
        // Create a TreeSet to automatically sort and enforce uniqueness based on email (case-insensitive)
        Set<Contributor> uniqueContributors = new TreeSet<>(
                Comparator.comparing(
                        Contributor::getEmail,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)
                )
        );

        Path folderPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + folderPath);
        }

        // Build the command to run the git log command
        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", folderPath.toString(), "log", "--pretty=%an <%ae>");
        processBuilder.redirectErrorStream(true);  // Combine stdout and stderr

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line.trim();  // Clean the line
                Pattern pattern = Pattern.compile("(.+) <(.+?)>");
                Matcher matcher = pattern.matcher(cleaned);
                if (matcher.find()) {
                    String fullName = matcher.group(1).trim();
                    String email = matcher.group(2).trim().equals("(null)") ? null : matcher.group(2).trim(); // Keep email as is for storage

                    // Create a new Contributor and add it to the TreeSet
                    Contributor contributor = new Contributor(fullName, fullName, email);
                    uniqueContributors.add(contributor); // TreeSet handles uniqueness
                }
            }
        }

        // Wait for the process to finish
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Error occurred while executing git command, exit code: " + exitCode);
        }

        // Convert the TreeSet to a List for further processing
        return parseAndSaveContributors(new ArrayList<>(uniqueContributors));
    }

    private List<Contributor> parseAndSaveContributors(List<Contributor> rawContributors) {
        List<Contributor> parsedList = new ArrayList<>();
        for (Contributor contributor : rawContributors) {
            // Check if contributor already exists in the database
            Contributor existingContributor = contributorRepository.findByEmail(contributor.getEmail());
            // If not, create and save a new Contributor
            parsedList.add(Objects.requireNonNullElseGet(existingContributor, () -> contributorRepository.save(contributor)));
        }
        return parsedList;
    }

    public List<String> getChangedFilesForContributor(String repoName, String name, String email) throws Exception {
        List<String> changedFiles = new ArrayList<>();
        Path folderPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + folderPath);
        }

        // Build the command to get files changed by the specific contributor
        ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "-C", folderPath.toString(), "log", "--author=" + name + " <" + email + ">", "--name-only", "--pretty=format:"
        );

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    changedFiles.add(line.trim());
                }
            }
        }

        return changedFiles;
    }
}
