package com.devskill.devskill_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class HelloService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HelloService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String downloadRepositoryZip(String organization, String repository) throws IOException {
        String downloadUrl = STR."https://codeload.github.com/\{organization}/\{repository}/zip/refs/heads/master";
        Path filePath = FileSystems.getDefault().getPath("downloaded.zip");

        byte[] zipFileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

        if (zipFileBytes == null || zipFileBytes.length == 0) {
            throw new IOException("Failed to download ZIP file");
        }

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(zipFileBytes);
        }

        return STR."Successfully downloaded and saved ZIP file for \{organization}/\{repository}";
    }

    public ArrayNode getArchiveData(String path) throws IOException {
        String fullUrl = constructUrl(path);
        ArrayNode jsonArray = objectMapper.createArrayNode();

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new URL(fullUrl).openStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                JsonNode jsonNode = objectMapper.readTree(line);
                jsonArray.add(jsonNode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jsonArray;
    }

    public Map<String, Integer> countEventTypesInArchive(ArrayNode jsonNodes) {
        Map<String, Integer> eventCounts = new HashMap<>();
        for (JsonNode node : jsonNodes) {
            if (node.has("type")) {
                String eventType = node.get("type").asText();
                eventCounts.merge(eventType, 1, Integer::sum);
            }
        }
        return eventCounts;
    }

    public Map<String, Integer> countUsersInArchive(ArrayNode jsonNodes) {
        Map<String, Integer> userCounts = new HashMap<>();
        for (JsonNode node : jsonNodes) {
            if (node.has("actor") && node.get("actor").has("login")) {
                String username = node.get("actor").get("login").asText();
                userCounts.merge(username, 1, Integer::sum);
            }
        }
        return userCounts;
    }

    public Map<String, List<Map<String, Integer>>> countPullRequestAndPushEvents(ArrayNode jsonNodes) {
        Map<String, List<Map<String, Integer>>> userEventCounts = new HashMap<>();

        // Iterate through each event in the JSON array
        for (JsonNode node : jsonNodes) {
            // Check if the event has both the "type" and "actor" fields
            if (node.has("type") && node.has("actor") && node.get("actor").has("login")) {
                String eventType = node.get("type").asText();
                String username = node.get("actor").get("login").asText();

                // Check if the event is either PullRequestEvent or PushEvent
                if ("PullRequestEvent".equals(eventType) || "PushEvent".equals(eventType)) {
                    // Create a list for the user if it doesn't exist
                    userEventCounts.putIfAbsent(username, new ArrayList<>());

                    // Find if this event type already exists in the list for this user
                    List<Map<String, Integer>> eventList = userEventCounts.get(username);
                    Optional<Map<String, Integer>> existingEventMapOpt = eventList.stream()
                            .filter(eventMap -> eventMap.containsKey(eventType))
                            .findFirst();

                    if (existingEventMapOpt.isPresent()) {
                        // Increment the count for the existing event type
                        Map<String, Integer> eventMap = existingEventMapOpt.get();
                        eventMap.put(eventType, eventMap.get(eventType) + 1);
                    } else {
                        // Add a new map for this event type with a count of 1
                        Map<String, Integer> newEventMap = new HashMap<>();
                        newEventMap.put(eventType, 1);
                        eventList.add(newEventMap);
                    }
                }
            }
        }

        return userEventCounts;
    }

    public Map<String, Object> getUsersWithMaxEvents(ArrayNode jsonNodes) {
        Map<String, Integer> userCounts = countUsersInArchive(jsonNodes);

        // Find the maximum event count
        int maxCount = userCounts.values().stream().max(Integer::compare).orElse(0);

        // Collect users who have the maximum count
        List<String> maxUsers = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : userCounts.entrySet()) {
            if (entry.getValue() == maxCount) {
                maxUsers.add(entry.getKey());
            }
        }

        // Prepare a map to hold both the max count and the users
        Map<String, Object> result = new HashMap<>();
        result.put("maxUsers", maxUsers);
        result.put("maxValue", maxCount);

        return result;
    }

    private String constructUrl(String path) {
        return STR."https://data.gharchive.org/\{path}.json.gz";
    }
}
