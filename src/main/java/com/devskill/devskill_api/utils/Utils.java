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

}
