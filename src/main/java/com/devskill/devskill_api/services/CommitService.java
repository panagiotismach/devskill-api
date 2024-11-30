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

    private Commit parseCommitLine(String line, RepositoryEntity repository) {
        String[] parts = line.split(" - ");
        if (parts.length < 5) {
            logger.warn("Skipping malformed commit line: " + line);
            return null;
        }

        String commitHash = parts[0].trim();
        String message = parts[1].trim();
        message = message.length() > 500 ? message.substring(0, 500) : message;
        LocalDate date = utils.parseDate(parts[2].trim());
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


    private List<Commit> saveCommits(List<Commit> commits) {
        return commitRepository.saveAll(commits);
    }

    private List<FileChanged> saveFilesChanged(List<FileChanged> fileChanged) {
        return fileChangedRepository.saveAll(fileChanged);
    }

}
