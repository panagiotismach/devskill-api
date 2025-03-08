package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.ContributorRepositoryEntity;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.ContributionRepository;
import com.devskill.devskill_api.repository.ContributorRepository;
import com.devskill.devskill_api.repository.ContributorRepositoryRepository;
import com.devskill.devskill_api.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ContributorsService {

    @Autowired
    private ContributorRepository contributorRepository;
    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private ContributorRepositoryRepository contributorRepositoryRepository;

    @Autowired
    private Utils utils;

    public ContributorsService() {
    }

    public List<Contributor> getContributors(String repoName) throws IOException, InterruptedException {
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
            throw new IllegalArgumentException(STR."Repository folder not found: \{folderPath}");
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

    public List<Contribution> getContributions(RepositoryEntity repository, Path repositoryPath, boolean isExisted, LocalDate preLastCommitDate) throws Exception {

        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repositoryPath.toString());
        command.add("log");
        command.add("--pretty=format:%H - %an - %ae");
        command.add("--numstat");
        command.add("--date=short");

        if (isExisted && preLastCommitDate != null) {

            String formattedDate = preLastCommitDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            command.add("--since=" + formattedDate);
        }


        // Git command to get commit history
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Map to aggregate contributions (keyed by contributor and file extension)
        Map<String, Contribution> contributionsMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            Contributor currentContributor = null;
            String authorEmail = null;

            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                // Parse contributor details
                String[] parts = line.split(" - ");
                if (parts.length == 3) {
                    String authorName = parts[1].trim();
                    authorEmail = parts[2].trim();

                    // Fetch or create the contributor
                    currentContributor = contributorRepository.findByEmail(authorEmail);

                    if (currentContributor == null) {
                        currentContributor = new Contributor(authorName, authorEmail, authorEmail);
                        contributorRepository.save(currentContributor);

                        saveContributorRepository(currentContributor, repository);
                    }
                } else if (utils.isFileChangeLine(line)) {
                    // Parse file change line
                    String[] fileChangeParts = line.trim().split("\\s+");

                    String filePath = fileChangeParts[2];

                    String fileExtension = utils.getFileExtension(filePath);

                    int insertions = fileChangeParts[0].equals("-") ? 0 : Integer.parseInt(fileChangeParts[0]);
                    int deletions = fileChangeParts[1].equals("-") ? 0 : Integer.parseInt(fileChangeParts[1]);

                    // Aggregate contributions by contributor and extension
                    String key = authorEmail + ":" + fileExtension;
                    Contribution contribution = getOrCreateContribution(currentContributor, fileExtension, insertions, deletions);

                    contributionsMap.put(key, contribution);
                }

            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Error occurred while executing git command, exit code: " + exitCode);
        }

        // Save and return contributions
        return new ArrayList<>(contributionsMap.values());
    }

    private void saveContributorRepository(Contributor contributor, RepositoryEntity repository) {

        Optional<ContributorRepositoryEntity> contributorRepositoryEntity = contributorRepositoryRepository.findByContributorAndRepository(contributor, repository);

        if (contributorRepositoryEntity.isEmpty()) {
            ContributorRepositoryEntity contributorRepositoryEntry = new ContributorRepositoryEntity(contributor, repository);
            contributorRepositoryRepository.save(contributorRepositoryEntry);
        }

    }

    private List<Contribution> saveContributions(List<Contribution> contributions) {
        return contributionRepository.saveAll(contributions);
    }

    public Contribution getOrCreateContribution(Contributor contributor, String extension, int insertions, int deletions) {
        Optional<Contribution> optionalContribution = contributionRepository.findByContributorAndExtension(contributor, extension);

        if (optionalContribution.isPresent()) {
            // Update the existing contribution
            Contribution existingContribution = optionalContribution.get();
            existingContribution.setInsertions(existingContribution.getInsertions() + insertions);
            existingContribution.setDeletions(existingContribution.getDeletions() + deletions);
            return contributionRepository.save(existingContribution);
        } else {
            // Create a new contribution
            Contribution newContribution = new Contribution(contributor, extension, insertions, deletions);
            return contributionRepository.save(newContribution);
        }
    }

    public Page<Contributor> findContributorsByRepository(Long repositoryId, Pageable pageable) {

        RepositoryEntity repository = new RepositoryEntity();
        repository.setId(repositoryId);


        // Find all associations for the repository
        Page<ContributorRepositoryEntity> associations = contributorRepositoryRepository.findByRepository(repository, pageable);

        // Extract contributors from the associations
        List<Contributor> contributors = associations.stream()
                .map(ContributorRepositoryEntity::getContributor)
                .distinct() // Optional, to ensure unique contributors
                .toList();

        return new PageImpl<>(contributors, pageable, associations.getTotalElements());
    }

    public Page<RepositoryEntity> findRepositoriesByContributor(Long contributorId, Pageable pageable) {

        Contributor contributor = new Contributor();
        contributor.setId(contributorId);


        // Find all associations for the repository
        Page<ContributorRepositoryEntity> associations = contributorRepositoryRepository.findByContributor(contributor, pageable);

        List<RepositoryEntity> repositories = associations.stream()
                .map(ContributorRepositoryEntity::getRepository)
                .distinct()
                .toList();

        return new PageImpl<>(repositories, pageable, associations.getTotalElements());
    }

    public Map<String, Object> findContributors(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Retrieve repositories with pagination
        Page<Contributor> contributorsPage = contributorRepository.findAll(pageable);

        return utils.constructPageResponse(contributorsPage);
    }

    public Map<String, Object> retrieveContributorsForRepository(Long repoId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Retrieve repositories with pagination
        Page<Contributor> contributorsPage = findContributorsByRepository(repoId, pageable);

        return utils.constructPageResponse(contributorsPage);
    }

    public Map<String,Object> findByGithubUsernameOrFullName(String username, String name, int page, int size){

        Pageable pageable = PageRequest.of(page, size);

        // Retrieve repositories with pagination
        Page<Contributor> contributorPage = contributorRepository.findByGithubUsernameOrFullName(username,name, pageable);


        return utils.constructPageResponse(contributorPage);
    }

    public List<Object[]> findTopContributors(){
        return contributionRepository.findTopContributors();
    }

    public List<Object[]> findContributionsPerContributor(Long contributorId) {
        List<Object[]> contributions = contributionRepository.findContributionsByContributor(contributorId);

        // Add "total changes" (insertions + deletions) dynamically
        return contributions.stream()
                .map(record -> new Object[]{
                        record[0],  // extension
                        record[1],  // insertions
                        record[2],  // deletions
                        ((Long) record[1]) + ((Long) record[2]) // total changes
                })
                .collect(Collectors.toList());
    }
}
