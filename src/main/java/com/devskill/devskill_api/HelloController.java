package com.devskill.devskill_api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RestController
public class HelloController {

    private final RestTemplate restTemplate;

    @Autowired
    public HelloController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/mine-skills")
    public ResponseEntity<String> mineSkills(@RequestParam String url) {
        // Extract the organization and repository from the URL
        String[] parts = url.split("/");
        if (parts.length < 5 || !"github.com".equals(parts[2])) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid GitHub URL format");
        }

        String organization = parts[3];
        String repository = parts[4];

        // Construct the URL to download the master branch as a ZIP file
        String downloadUrl = "https://codeload.github.com/" + organization + "/" + repository + "/zip/refs/heads/master";

        // Define the path where the file will be saved
        Path filePath = Paths.get("downloaded.zip");

        try {
            // Download the file
            byte[] zipFileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

            if (zipFileBytes == null || zipFileBytes.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to download ZIP file");
            }

            // Save the file
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(zipFileBytes);
            }

            return ResponseEntity.ok("Successfully downloaded and saved ZIP file for " + organization + "/" + repository);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to download or save ZIP file: " + e.getMessage());
        }
    }

    @GetMapping("/archivegh")
    public JsonNode  archive(@RequestParam String path) throws IOException {

      ArrayNode jsonArray = gettingJSON(path);

      return jsonArray;
    }

    private static final String FILE_PATH = "C:/Users/Panos/Downloads/2015-01-01-15 (1).json/2015-01-01-15 (1).json";

    private String constructUrl(String path){

        // Define the base URL within the method
        String baseUrl = "https://data.gharchive.org/";

        // Construct the full URL by combining base URL, path, and ".json.gz"
        return STR."\{baseUrl}\{path}.json.gz";
    }

    private ArrayNode gettingJSON(String path) throws IOException {

        String fullUrl = constructUrl(path);

        // Initialize ObjectMapper for JSON parsing
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode jsonArray = objectMapper.createArrayNode(); // To hold multiple JSON objects

        try {
            // Open the GZIP stream from the constructed URL
            GZIPInputStream gzipInputStream = new GZIPInputStream(new URL(fullUrl).openStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream));
            String line;

            // Read each line from the decompressed data
            while ((line = reader.readLine()) != null) {
                // Parse each line as a JSON object and add it to the array
                JsonNode jsonNode = objectMapper.readTree(line);
                jsonArray.add(jsonNode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Return the accumulated array of JSON objects
            return jsonArray;
    }

    @GetMapping("/countEventTypesInAMonth")
    public ResponseEntity<?> countEventTypesInAMonth(@RequestParam String date) {


        Map<String, Integer> eventCounts;

        try {
            ArrayNode jsonArray = gettingJSON(date);
            eventCounts = countEventTypes(jsonArray);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }

        // Return 200 OK with the map of event counts as the response body
        return ResponseEntity.ok(eventCounts);
    }

    @GetMapping("/usersInAMonth")
    public ResponseEntity<?> usersInAMonth(@RequestParam String date) {


        Map<String, Integer> usersInAMonth;

        try {
            ArrayNode jsonArray = gettingJSON(date);
            usersInAMonth = countUsersInAMonth(jsonArray);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(STR."Error reading or processing the file: \{e.getMessage()}");
        }

        // Return 200 OK with the map of event counts as the response body
        return ResponseEntity.ok(usersInAMonth);
    }

    private Map<String, Integer> countUsersInAMonth(ArrayNode jsonNodes) {
        Map<String, Integer> userCounts = new HashMap<>();

        // Iterate over each node in the ArrayNode
        for (JsonNode node : jsonNodes) {
            if ( node.has("actor") ) {
                JsonNode actor = node.get("actor");
                if(actor.has("login")){
                    String username = actor.get("login").asText();
                    // Update the count for this event type
                    userCounts.merge(username, 1, Integer::sum);
                }
            }
        }

        return userCounts;
    }

    private Map<String, Integer> countEventTypes(ArrayNode jsonNodes) {
        Map<String, Integer> eventCounts = new HashMap<>();

        // Iterate over each node in the ArrayNode
        for (JsonNode node : jsonNodes) {
            if (node.has("type")) {
                String eventType = node.get("type").asText();

                // Update the count for this event type
                eventCounts.merge(eventType, 1, Integer::sum);
            }
        }

        return eventCounts;
    }

}
