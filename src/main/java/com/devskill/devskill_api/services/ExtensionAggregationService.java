package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Contribution;
import com.devskill.devskill_api.repository.ContributionRepository;
import com.devskill.devskill_api.repository.RepositoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExtensionAggregationService {

    @Autowired
    private RepositoryRepository repoRepository;

    @Autowired
    private ContributionRepository contributionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Run manually for thesis, or schedule daily with @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public void updateExtensionSummary() {
        // Clear the table
        jdbcTemplate.update("TRUNCATE TABLE extension");

        // Aggregate repo counts from repositories
        Map<String, Integer> repoCounts = repoRepository.findAll().stream()
                .flatMap(r -> r.getExtensions().stream()
                        .map(ext -> new AbstractMap.SimpleEntry<>(ext, r)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.collectingAndThen(Collectors.toSet(), Set::size)
                        )
                ));

        // Aggregate last used dates from repositories
        Map<String, LocalDate> lastUsed = repoRepository.findAll().stream()
                .flatMap(r -> r.getExtensions().stream()
                        .map(ext -> new AbstractMap.SimpleEntry<>(ext, r.getLast_commit_date())))
                .collect(Collectors.groupingBy(
                        (Map.Entry<String, LocalDate> entry) -> entry.getKey(),
                        Collectors.mapping(
                                (Map.Entry<String, LocalDate> entry) -> entry.getValue(),
                                Collectors.maxBy(Comparator.naturalOrder())
                        )
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().orElse(null)
                ));

        // Aggregate file counts from contributions
        Map<String, Integer> fileCounts = contributionRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Contribution::getExtension,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));

        // Insert aggregated data into extension table
        repoCounts.forEach((ext, repoCount) -> {
            jdbcTemplate.update(
                    "INSERT INTO extension (extension_name, repo_count, file_count, last_used) VALUES (?, ?, ?, ?)",
                    ext,
                    repoCount,
                    fileCounts.getOrDefault(ext, 0),
                    lastUsed.get(ext)
            );
        });
    }

}