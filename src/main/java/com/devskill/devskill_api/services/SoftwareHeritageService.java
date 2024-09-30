package com.devskill.devskill_api.services;

import com.devskill.devskill_api.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class SoftwareHeritageService {


    // ObjectMapper for parsing JSON data
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Utils utils;

    public SoftwareHeritageService() {

    }

    /**
     * Downloads a ZIP file of a GitHub repository.
     * It constructs the download URL using the organization and repository names and saves the ZIP file locally.
     *
     * @param organization The name of the GitHub organization.
     * @param repository The name of the GitHub repository.
     * @return A success message indicating the download status.
     * @throws IOException if the download fails or the file cannot be written.
     */
    public String downloadRepositoryFromWayBack(String organization, String repository) throws IOException {
        // Construct the download URL for the repository's ZIP file
        String url = utils.constructUrl("wayback", organization, repository);

        return utils.downloadRepo(url);
    }

    public String getArchiveSH(String repoUrl) throws IOException {

        String url = utils.constructUrl("softwareheritage", repoUrl);

        return utils.downloadRepo(url);
    }
}

