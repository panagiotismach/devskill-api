package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.utils.Utils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class RepositorySyncService {

    @Autowired
    private RepoService repoService;
    @Autowired
    private CommitService commitService;

    @Autowired
    private ContributorsService contributorsService;

    @Autowired
    private Utils utils;

    private static final Logger logger = LoggerFactory.getLogger(RepositorySyncService.class);


    public Map<String, Object> syncRepositoryData(String repoName, int files, long megabyte, boolean isTrending) throws Exception {

        RepositoryEntity repository;
        List<Contribution> Contribution;

        Map<String, Object> result = new HashMap<>();
        Path repositoryPath = null;
        try {

            repositoryPath = utils.getPath(repoName);

            cloneRepository(repoName, repositoryPath);
            checkRepository(repositoryPath,files,megabyte);

            repository = repoService.getRepoDetails(repositoryPath, isTrending);
            Contribution = contributorsService.getContributions(repository, repositoryPath);

            // Create a map to store contributors and commits
            result.put("contributors", Contribution);

            removeRepository(repositoryPath);

            logger.info(STR."Repository \{repoName} has successfully processed in the system");

            return result;

        } catch (Exception e) {
            result.put("error", STR."\{e.getMessage()} Error reading the repository \{repoName}");
            logger.info(STR."\{e.getMessage()} Error reading the repository \{repoName}");
            removeRepository(repositoryPath);
            return result;
        }

    }

    private void checkRepository(Path repositoryPath, int files, long megabyte) throws Exception {

        int filesCount = utils.executeGitFileCount(repositoryPath);

        long repoSize = utils.getGitRepoSizeInMB(repositoryPath);


        if(filesCount < files || repoSize < megabyte){
            throw new Exception(STR."The files are \{filesCount} and the repo size is \{repoSize} mb. The repository have to has over \{files} files and has to be over \{megabyte} mb.");
        }
    }

    private void cloneRepository(String repoName, Path repositoryPath) throws Exception {

        String url;

        if(repoName.startsWith("https://github.com/")){
            url = repoName;
        }else {
            url = STR."https://github.com/\{repoName}";
        }

        // Check if the directory exists
        if (!Files.exists(repositoryPath) || !Files.isDirectory(repositoryPath)) {
            Files.createDirectories(repositoryPath);
            ProcessBuilder builder = new ProcessBuilder("git", "clone", url, repositoryPath.toString());
            builder.redirectErrorStream(true);
            Process process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Failed to clone repository: " + repoName);
            }
        }

    }

    public List<String> readRepositoryNamesFromJson() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();


        Path jsonFilePath = utils.getPathOfRepositories("output-1-2015.json");


        JsonNode rootNode = objectMapper.readTree(jsonFilePath.toFile());


        JsonNode repositoriesNode = rootNode.get("uniqueRepoNames");

        if (repositoriesNode == null || !repositoriesNode.isArray()) {
            throw new IllegalArgumentException("Field 'repositories' not found or is not an array in the JSON file.");
        }

        List<String> repositoryNames = new ArrayList<>();
        for (JsonNode node : repositoriesNode) {
            repositoryNames.add(node.asText());
        }

        return repositoryNames;
    }


    private void removeRepository(Path repositoryPath) throws IOException {

        // Check if the directory exists
        if (Files.exists(repositoryPath) && Files.isDirectory(repositoryPath)) {
            // Delete the directory and its contents
            try {
                Files.walk(repositoryPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                logger.info("Repository removed successfully: {}", repositoryPath);
            } catch (IOException e) {
                logger.info("Failed to remove repository: {}", repositoryPath);
                throw new IOException("Failed to remove repository: " + repositoryPath, e);
            }
        } else {
            logger.info("Repository does not exist: {}", repositoryPath);
        }
    }
}
