package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.Extension;
import com.devskill.devskill_api.models.ExtensionDTO;
import com.devskill.devskill_api.models.RepositoryEntity;
import com.devskill.devskill_api.repository.ExtensionRepository;
import com.devskill.devskill_api.utils.General;
import com.devskill.devskill_api.utils.Utils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExtensionService {

    @Autowired
    private ExtensionRepository extensionRepository;

    @Autowired
    private Utils utils;

    private final Map<String, List<String>> extensionToLanguages = new HashMap<>();

    @PostConstruct
    public void init() {
        loadLanguageMappings();
    }


    public Map<String, Object> getExtensions(Pageable pageable) {
        Page<ExtensionDTO> extensions = extensionRepository.findAll(pageable)
                .map(s -> new ExtensionDTO(
                        s.getExtensionName(),
                        s.getLanguage(),
                        s.getFileCount(),
                        s.getRepoCount(),
                        s.getLastUsed()
                ));

        return utils.constructPageResponse(extensions);
    }

    public Map<String, Object> getFilteredExtensions(String extensionName, Pageable pageable) {
        Page<ExtensionDTO> extensions =  extensionRepository.findByExtensionName(extensionName, pageable)
                .map(s -> new ExtensionDTO(
                        s.getExtensionName(),
                        s.getLanguage(),
                        s.getFileCount(),
                        s.getRepoCount(),
                        s.getLastUsed()
                ));

        return utils.constructPageResponse(extensions);
    }

    public void updateExtensionTable(boolean repoExist, Map<String, Integer> extensionCounts, LocalDate lastCommitDate) {
        for (Map.Entry<String, Integer> entry : extensionCounts.entrySet()) {
            String extensionName = entry.getKey();
            int fileCount = entry.getValue();

            // Find or create extension entity
            Optional<Extension> optionalExtension = extensionRepository.findById(extensionName);
            Extension extension;

            if (optionalExtension.isPresent()) {
                // Extension exists, update file_count and last_used
                extension = optionalExtension.get();
                if(!repoExist){
                    extension.setRepoCount(extension.getRepoCount() + 1);
                }
                extension.setFileCount(extension.getFileCount() + fileCount);
                LocalDate newLastUsed = extension.getLastUsed() != null && lastCommitDate != null ?
                        (lastCommitDate.isAfter(extension.getLastUsed()) ? lastCommitDate : extension.getLastUsed()) :
                        lastCommitDate;
                extension.setLastUsed(newLastUsed);
            } else {
                // Extension does not exist, create new with repo_count=1
                extension = new Extension(extensionName , extensionToLanguages.get(extensionName) != null? extensionToLanguages.get(extensionName) : List.of("other") , 1, fileCount, lastCommitDate);
            }


            extensionRepository.save(extension);
        }
    }

    public void loadLanguageMappings() {
        Yaml yaml = new Yaml();
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("languages.yml");) {
            if (inputStream == null) {
                System.out.println("i");
                return;
            }
            Map<String, Map<String, Object>> languages = yaml.load(inputStream);
            if (languages == null) {
                System.out.println("y");
                return;
            }

            for (Map.Entry<String, Map<String, Object>> entry : languages.entrySet()) {
                String language = entry.getKey();
                Map<String, Object> properties = entry.getValue();

                Object extensionsObj = properties.get("extensions");
                System.out.println(extensionsObj);
                if (extensionsObj instanceof List) {
                    List<String> extensions = (List<String>) extensionsObj;
                    for (String ext : extensions) {
                        String cleanExt = ext.startsWith(".") ? ext.substring(1) : ext;
                        String key = cleanExt.toLowerCase();
                        extensionToLanguages.computeIfAbsent(key, k -> new ArrayList<>()).add(language);
                    }
                }

                Object filenamesObj = properties.get("filenames");
                if (filenamesObj instanceof List) {
                    List<String> filenames = (List<String>) filenamesObj;
                    for (String filename : filenames) {
                        String key = filename.toLowerCase();
                        extensionToLanguages.computeIfAbsent(key, k -> new ArrayList<>()).add(language);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}