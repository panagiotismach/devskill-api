package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.FileChanged;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.CommitRepository;
import com.devskill.devskill_api.repository.ContributorRepository;
import com.devskill.devskill_api.repository.FileChangedRepository;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CommitService {


    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private ContributorRepository contributorRepository;

    @Autowired
    private FileChangedRepository fileChangedRepository;

    @Autowired
    private Utils utils;

    private static final Logger logger = LoggerFactory.getLogger(CommitService.class);

    public List<Commit> getCommits(RepositoryEntity repository) throws IOException, InterruptedException, ResponseStatusException {
        Path repositoryPath = utils.getPathOfRepository(repository.getName());

        // Git command to get commit history
        ProcessBuilder processBuilder = new ProcessBuilder("git", "-C", repositoryPath.toString(),
                "log", "--pretty=format:%H - %s - %cd - %an - %ae", "--numstat", "--date=short");
        processBuilder.redirectErrorStream(true);

        List<Commit> commits = new ArrayList<>();
        List<FileChanged> filesChanged = new ArrayList<>();
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            Commit currentCommit = null;
            int insertions = 0;
            int deletions = 0;
            List<String> numStatLines = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                // If the line is empty, it's the end of the current commit block
                if (line.isEmpty()) {
                    if (currentCommit != null) {
                        // Log the current commit details
                        logger.info("Commit: " + currentCommit.getCommitHash() + " - " + currentCommit.getMessage());
                        // Log numstat information
                        if (numStatLines.isEmpty()) {
                            logger.info("(no changes)");
                        } else {
                            for (String statLine : numStatLines) {
                                logger.info(statLine);
                            }
                        }
                        logger.info(""); // Add an empty line for separation between commits

                        // Finalize and add the commit to the list
                        currentCommit.setInsertions(insertions);
                        currentCommit.setDeletions(deletions);
                        currentCommit.setFilesChanged(numStatLines.size());
                        commits.add(currentCommit);

                        // Reset for the next commit
                        currentCommit = null;
                        insertions = 0;
                        deletions = 0;
                        numStatLines.clear();
                    }
                    continue; // Skip to the next line
                }

                // Check if the line is a commit line
                if (currentCommit == null) {
                    Commit parsedCommit = parseCommitLine(line, repository);
                    if (parsedCommit != null) {
                        currentCommit = parsedCommit; // Successfully parsed a commit
                    } else {
                        logger.warn("Skipping unrecognized line: " + line);
                    }
                } else {
                    // Check if it's a file change line
                    if (isFileChangeLine(line)) {
                        FileChanged fileChanged = parseFileChangeLine(line, currentCommit);
                        if (fileChanged != null) {
                            insertions += fileChanged.getInsertions();
                            deletions += fileChanged.getDeletions();
                            filesChanged.add(fileChanged);
                            numStatLines.add(String.format("%d\t%d\t%s", fileChanged.getInsertions(), fileChanged.getDeletions(), fileChanged.getFileName()));
                        }
                    } else {

                        // Check if the line indicates another commit
                        Commit nextCommit = parseCommitLine(line, repository);
                        if (nextCommit != null) {

                            // Finalize and add the current commit to the list
                            currentCommit.setInsertions(insertions);
                            currentCommit.setDeletions(deletions);
                            currentCommit.setFilesChanged(numStatLines.size());
                            commits.add(currentCommit);

                            // Move to the next commit
                            currentCommit = nextCommit;
                            // Reset stats
                            insertions = 0;
                            deletions = 0;
                            numStatLines.clear();
                        }
                    }
                }
            }

            // Handle the last commit if there are no trailing empty lines
            if (currentCommit != null) {
                logger.info("Commit: " + currentCommit.getCommitHash() + " - " + currentCommit.getMessage());
                if (numStatLines.isEmpty()) {
                    logger.info("(no changes)");
                } else {
                    for (String statLine : numStatLines) {
                        logger.info(statLine);
                    }
                }

                // Finalize and add the last commit to the list
                currentCommit.setInsertions(insertions);
                currentCommit.setDeletions(deletions);
                currentCommit.setFilesChanged(numStatLines.size());
                commits.add(currentCommit);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.error("Git command failed with exit code: " + exitCode);
            throw new IOException("Error occurred while executing git command, exit code: " + exitCode);
        }


       List<Commit> savedCommits = saveCommits(commits);

       List<FileChanged> savedFileChanged = saveFilesChanged(filesChanged);

        return savedCommits;
    }


    private boolean isFileChangeLine(String line) {
        // A file change line must contain at least 3 parts: insertions, deletions, and file path

        String[] parts = line.trim().split("\\s+");
        return parts.length >= 3 && isNumeric(parts[0]) && isNumeric(parts[1]);
    }

    private boolean isNumeric(String str) {
        return str.matches("\\d+");
    }

    private Commit parseCommitLine(String line, RepositoryEntity repository) {
        String[] parts = line.split(" - ");
        if (parts.length < 5) {
            logger.warn("Skipping malformed commit line: " + line);
            return null;
        }

        String commitHash = parts[0].trim();
        String message = parts[1].trim();
        message = message.length() > 500 ? message.substring(0, 500) : message;
        LocalDate date = parseDate(parts[2].trim());
        String authorName = parts[3].trim();
        String authorEmail = parts[4].trim();

        Contributor contributor = contributorRepository.findByEmail(authorEmail);
        if (contributor == null) {
            contributor = new Contributor(authorName, authorEmail, authorEmail);
            contributorRepository.save(contributor);
        }

        return new Commit(contributor, commitHash, message, date, repository);
    }

    private FileChanged parseFileChangeLine(String line, Commit commit) {
        String[] parts = line.trim().split("\\s+");

        // Check if the line has the expected number of parts
        if (parts.length < 3) {
            logger.warn("Skipping malformed file change line: " + line);
            return null;
        }

        int insertions = 0;
        int deletions = 0;
        String filePath = parts[2];

        try {
            // Attempt to parse the insertions and deletions, providing additional validation
            if (!parts[0].equals("-")) {
                insertions = Integer.parseInt(parts[0]);
            }

            if (!parts[1].equals("-")) {
                deletions = Integer.parseInt(parts[1]);
            }
        } catch (NumberFormatException e) {
            logger.warn("Error parsing insertions or deletions for line: " + line + ". Error: " + e.getMessage());
            return null; // Return null to indicate parsing failed
        }

        String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
        String fileExtension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1) : "";

        // Create and return the FileChanged object
        return new FileChanged(fileName, filePath, fileExtension, insertions, deletions, LocalDateTime.now(), LocalDateTime.now(), commit);
    }

    private LocalDate parseDate(String dateStr) {
        try {

            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private List<Commit> saveCommits(List<Commit> commits) {
        return commitRepository.saveAll(commits);
    }

    private List<FileChanged> saveFilesChanged(List<FileChanged> fileChanged) {
        return fileChangedRepository.saveAll(fileChanged);
    }
}
