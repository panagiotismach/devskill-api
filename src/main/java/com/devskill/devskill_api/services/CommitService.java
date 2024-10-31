package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.CommitRepository;
import com.devskill.devskill_api.repository.ContributorRepository;
import com.devskill.devskill_api.repository.RepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CommitService {


    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private ContributorRepository contributorsRepository;

    public List<Commit> getCommitsForRepo(String repoName, Long repoId) throws IOException, InterruptedException, ResponseStatusException {

        // Find the RepositoryEntity using the repository ID
        Optional<RepositoryEntity> optionalRepo = repositoryRepository.findById(repoId);
        if (!optionalRepo.isPresent()) {
            throw new IllegalArgumentException("Repository with ID " + repoId + " not found.");
        }

        RepositoryEntity repository = optionalRepo.get(); // Get the repository entity

        // Specify the path to the repositories folder
        Path folderPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + folderPath);
        }

        // Build the command to run the git log command
        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", folderPath.toString(),
                "log", "--pretty=format:%H - %s - %cd - %an - %ae", "--date=short");
        processBuilder.redirectErrorStream(true);  // Combine stdout and stderr

        // Start the process
        Process process = processBuilder.start();

        // Read the output from the process
        List<Commit> commits = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Split the line into components
                String[] parts = line.split(" - ");
                if (parts.length >= 5) { // Adjusted length to account for author info
                    String commitHash = parts[0].trim();
                    String message = parts[1].trim();
                    if (message.length() > 500) {
                        message = message.substring(0, 500); // Cut off at 500 characters
                    }

                    LocalDate date;
                    try {
                        date = LocalDate.parse(parts[2].trim()); // Parsing the date
                    } catch (DateTimeParseException e) {
                        date = null;
                    }
                    String authorName = parts[3].trim(); // Author name
                    String authorEmail = parts[4].trim(); // Author email

                    Contributor contributor = contributorsRepository.findByEmail(authorEmail);

                    if(contributor == null){
                          continue;
                    }

                    // Create a new Commit object
                    Commit commit = new Commit(contributor,commitHash, message, date, repository);
                    commits.add(commit); // Add the commit to the list
                }
            }
        }

        // Wait for the process to finish
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Error occurred while executing git command, exit code: " + exitCode);
        }

        return saveCommits(commits);
    }



    private List<Commit> saveCommits(List<Commit> commits) {

        commitRepository.saveAll(commits);

        return commits;
    }
}
