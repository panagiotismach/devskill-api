package com.devskill.devskill_api.controllers;


import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.RepositoryRepository;
import com.devskill.devskill_api.services.ExtensionService;
import com.devskill.devskill_api.services.RepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class SkillabController {

    @Autowired
    private ExtensionService extensionService;

    @Autowired
    private RepoService repoService;

    @GetMapping("/getAggregatedExtensions")
    public ResponseEntity<?> getAggregatedExtensions() {

        return ResponseEntity.ok(extensionService.getExtensions(null));
    }

    @GetMapping("/countReposPerOrganization")
    public ResponseEntity<?> countReposPerOrganization() {

        return ResponseEntity.ok(repoService.countReposPerOrganization());
    }

    @GetMapping("/extensions-per-organization")
    public ResponseEntity<?> getExtensionsPerOrganization() {

        return ResponseEntity.ok(repoService.getExtensionsPerOrganization());
    }

    @GetMapping("/extensions/trends/monthly")
    public ResponseEntity<?> getMonthlyTrends() {
        return ResponseEntity.ok(repoService.getMonthlyExtensionTrends());
    }

    @GetMapping("/extension-co-occurrence")
    public ResponseEntity<?> getExtensionCoOccurrence(
            @RequestParam(defaultValue = "5") int minCount,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(repoService.getExtensionCoOccurrence(minCount, limit));
    }



}