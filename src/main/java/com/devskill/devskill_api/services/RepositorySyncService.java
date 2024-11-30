package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.models.Contributor;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    public Map<String, Object> syncRepositoryData(String repoName) throws IOException, InterruptedException {

        RepositoryEntity repository;
        List<Contribution> Contribution;
        List<Commit> commits;

        try {

            Path repositoryPath = utils.getPathOfRepository(repoName);

            int filesCount = utils.executeGitFileCount(repositoryPath);

            long repoSize = utils.getGitRepoSizeInMB(repositoryPath);

            if(filesCount < 200 || repoSize < 100){
                return null;
            }

            repository =  repoService.getRepoDetails(repoName);

            // Step 1: Fetch and save contributors
            Contribution = contributorsService.getContributions(repository);

            // Create a map to store contributors and commits
            Map<String, Object> result = new HashMap<>();
            result.put("contributors", Contribution);

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to synchronize repository data", e);
        }

    }
}
