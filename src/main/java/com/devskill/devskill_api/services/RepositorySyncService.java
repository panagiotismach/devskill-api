package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RepositorySyncService {

    @Autowired
    private CommitService commitService;

    @Autowired
    private ContributorsService contributorsService;


    public Map<String, Object> syncRepositoryData(String repoName, Long repoId) throws IOException, InterruptedException {

        List<Contributor> contributors;
        List<Commit> commits;
        try {
            // Step 1: Fetch and save contributors
            contributors = contributorsService.getCommitsAndContributors(repoName);

            // Step 2: Fetch and save commits
            commits = commitService.getCommitsForRepo(repoName, repoId);

        } catch (Exception e) {
            throw new RuntimeException("Failed to synchronize repository data", e);
        }

        // Create a map to store contributors and commits
        Map<String, Object> result = new HashMap<>();
        result.put("contributors", contributors);
        result.put("commits", commits);

        return result;
    }
}
