package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Commit;
import com.devskill.devskill_api.models.Contributor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class RepositorySyncService {

    @Autowired
    private CommitService commitService;

    @Autowired
    private ContributorsService contributorsService;

    public SyncReport syncRepositoryData(String repoName, Long repoId) throws IOException, InterruptedException {
        SyncReport syncReport = new SyncReport();

        try {
            // Step 1: Fetch and save contributors
            List<Contributor> contributors = contributorsService.getCommitsAndContributors(repoName);
            syncReport.setContributors(contributors);

            // Step 2: Fetch and save commits
            List<Commit> commits = commitService.getCommitsForRepo(repoName, repoId);
            syncReport.setCommits(commits);

        } catch (Exception e) {
            throw new RuntimeException("Failed to synchronize repository data", e);
        }

        return syncReport;
    }

    public static class SyncReport {
        private List<Contributor> contributors;
        private List<Commit> commits;

        public List<Contributor> getContributors() {
            return contributors;
        }

        public void setContributors(List<Contributor> contributors) {
            this.contributors = contributors;
        }

        public List<Commit> getCommits() {
            return commits;
        }

        public void setCommits(List<Commit> commits) {
            this.commits = commits;
        }
    }
}

