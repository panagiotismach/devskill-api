package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Contributor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ContributorsService {

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

    public List<Contributor> getContributors(String repoName) throws IOException {
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

        return parseContributors(contributors);
    }

    private List<Contributor> parseContributors(List<String> contributors) {
        List<Contributor> parsedList = new ArrayList<>();
        for (String contributor : contributors) {
            // Replace escaped characters
            String cleaned = contributor
                    .replace("\\u003C", "<")
                    .replace("\\u003E", ">");

            // Use regex to extract name and email
            Pattern pattern = Pattern.compile("(.+) <(.+?)>");
            Matcher matcher = pattern.matcher(cleaned);
            if (matcher.find()) {
                String name = matcher.group(1).trim();
                String email = matcher.group(2).trim();
                parsedList.add(new Contributor(name, email));
            }
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
