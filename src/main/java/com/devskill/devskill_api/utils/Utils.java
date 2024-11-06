package com.devskill.devskill_api.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class Utils {

    // URL patterns as final strings
    private static final String URL_ARCHIVE = "https://data.gharchive.org/";
    private static final String URL_SOFTWARE_HERITAGE = "https://archive.softwareheritage.org";
    private static final String URL_WAYBACK = "https://codeload.github.com/";

    private final RestTemplate restTemplate;

    /**
     * Constructor for Utils.
     * @param restTemplate The RestTemplate instance used for making HTTP requests.
     */
    @Autowired
    public Utils(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Constructs a URL based on the type and path.
     * @param type The type of archive (softwareheritage or gharchive).
     * @param path The dynamic part of the URL.
     * @return The full constructed URL.
     */
    public String constructUrl(String type, String path) {
        String urlPattern = type.equals("softwareheritage") ?
                STR."\{URL_SOFTWARE_HERITAGE}/api/1/vault/flat/%s" :
                STR."\{URL_ARCHIVE}%s.json.gz";
        return String.format(urlPattern, path);
    }

    public String constructUrl(String type, String organization, String repository) {

        String urlPattern = "";
        if(!type.equals("wayback")){
            return  urlPattern;
        }
        urlPattern =  STR."\{URL_WAYBACK}/%s/%s/zip/refs/heads/master";
        return String.format(urlPattern, organization, repository);
    }

    /**
     * Downloads a repository file (ZIP) from the given URL and saves it.
     * @param url The URL from which to download the ZIP file.
     * @return A success message if the download succeeds.
     * @throws IOException if the download or file writing fails.
     */
    public String downloadRepo(String url) throws IOException {
        // Define the file name for saving the TAR.GZ file
        String fileName = "downloaded.tar.gz";

        // Fetch the TAR.GZ file bytes from the URL as a ResponseEntity
        ResponseEntity<byte[]> responseEntity = restTemplate.getForEntity(url, byte[].class);

        // Check if the response status is OK
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new IOException(STR."Failed to download file: \{responseEntity.getStatusCode()}");
        }

        // Write the response body to the file
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            byte[] fileBytes = responseEntity.getBody();
            if (fileBytes != null) {
                outputStream.write(fileBytes);
            } else {
                throw new IOException("Failed to download file: Response body is null");
            }
        }

        return STR."Successfully downloaded and saved TAR.GZ file as \{fileName}";
    }

    public Path getPathOfRepository(String repoName){
        // Specify the path to the repositories folder
        Path repositoryPath = Path.of("repos", repoName);

        // Check if the directory exists
        if (!Files.exists(repositoryPath) || !Files.isDirectory(repositoryPath)) {
            throw new IllegalArgumentException("Repository folder not found: " + repositoryPath);
        }

        return repositoryPath;
    }

    public String extractRepoNameFromUrl(String url) {
        // Remove the ".git" suffix if it exists
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }

        // Handle SSH format
        if (url.startsWith("git@")) {
            // Remove everything up to the first colon
            url = url.substring(url.indexOf(":") + 1); // This will give "organization/repo"
        } else if (url.startsWith("https://")) {
            // Remove the 'https://github.com/' part
            url = url.substring(url.indexOf("github.com/") + "github.com/".length()); // This will give "organization/repo"
        } else if (url.startsWith("http://")) {
            // Similar for HTTP URLs
            url = url.substring(url.indexOf("github.com/") + "github.com/".length());
        }

        // Split the URL and get the organization and repository name
        String[] parts = url.split("/");
        if (parts.length >= 2) {
            String organization = parts[0];
            String repository = parts[1];
            return organization + "/" + repository; // Return in "organization/repo" format
        }

        throw new IllegalArgumentException("Invalid repository URL format: " + url);
    }


}
