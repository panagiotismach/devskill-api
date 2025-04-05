package com.devskill.devskill_api.services;

import com.devskill.devskill_api.models.ExtensionDTO;
import com.devskill.devskill_api.repository.ExtensionRepository;
import com.devskill.devskill_api.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ExtensionService {

    @Autowired
    private ExtensionRepository extensionRepository;

    @Autowired
    private Utils utils;

    public Map<String, Object> getExtensions(Pageable pageable) {
        Page<ExtensionDTO> extensions = extensionRepository.findAll(pageable)
                .map(s -> new ExtensionDTO(
                        s.getExtensionName(),
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
                        s.getFileCount(),
                        s.getRepoCount(),
                        s.getLastUsed()
                ));

        return utils.constructPageResponse(extensions);
    }
}