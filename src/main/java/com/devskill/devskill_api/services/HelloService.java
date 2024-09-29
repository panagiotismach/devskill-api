package com.devskill.devskill_api.services;

import com.devskill.devskill_api.config.AppConfig;
import com.devskill.devskill_api.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Service
public class HelloService {


    // ObjectMapper for parsing JSON data
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private Utils utils;


    public HelloService() {

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

    /**
     * Retrieves and decompresses the archive data from a specified path.
     * It reads the GZIP-compressed JSON data and converts each line to a JsonNode, storing them in an ArrayNode.
     *
     * @param path The path to the archive data.
     * @return An ArrayNode containing the parsed JSON data.
     * @throws IOException if the data cannot be retrieved or parsed.
     */
    public ArrayNode getArchiveData(String path) throws IOException {
        // Construct the full URL for the GZIP JSON data

        String url = utils.constructUrl("archivegh", path);

        ArrayNode jsonArray = objectMapper.createArrayNode();

        // Read and decompress the GZIP input stream from the URL
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new URL(url).openStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(gzipInputStream))) {
            String line;
            // Parse each line of JSON data and add it to the array node
            while ((line = reader.readLine()) != null) {
                JsonNode jsonNode = objectMapper.readTree(line);
                jsonArray.add(jsonNode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e); // Handle exceptions
        }

        return jsonArray; // Return the populated JSON array
    }

    /**
     * Counts the number of occurrences of each event type in the archive data.
     * It iterates through the JSON nodes and aggregates the counts of event types into a map.
     *
     * @param jsonNodes The ArrayNode containing the JSON data.
     * @return A map where the keys are event types and the values are their respective counts.
     */
    public Map<String, Integer> countEventTypesInArchive(ArrayNode jsonNodes) {
        Map<String, Integer> eventCounts = new HashMap<>();
        // Iterate through each JSON node in the array
        for (JsonNode node : jsonNodes) {
            // Check if the node has a "type" field
            if (node.has("type")) {
                String eventType = node.get("type").asText();
                // Merge the event count for this type
                eventCounts.merge(eventType, 1, Integer::sum);
            }
        }
        return eventCounts; // Return the map of event type counts
    }

    /**
     * Counts the number of unique users present in the archive data.
     * It extracts the usernames from the actor field in the JSON nodes and aggregates the counts.
     *
     * @param jsonNodes The ArrayNode containing the JSON data.
     * @return A map where the keys are usernames and the values are their respective counts.
     */
    public Map<String, Integer> countUsersInArchive(ArrayNode jsonNodes) {
        Map<String, Integer> userCounts = new HashMap<>();
        // Iterate through each JSON node in the array
        for (JsonNode node : jsonNodes) {
            // Check if the node has an "actor" with a "login" field
            if (node.has("actor") && node.get("actor").has("login")) {
                String username = node.get("actor").get("login").asText();
                // Merge the user count
                userCounts.merge(username, 1, Integer::sum);
            }
        }
        return userCounts; // Return the map of user counts
    }

    /**
     * Counts the number of PullRequest and Push events for each user in the archive data.
     * It organizes the counts into a map where each key is a username and each value is a list of event type counts.
     *
     * @param jsonNodes The ArrayNode containing the JSON data.
     * @return A map where the keys are usernames and the values are lists of maps containing event type counts.
     */
    public Map<String, List<Map<String, Integer>>> countPullRequestAndPushEvents(ArrayNode jsonNodes) {
        Map<String, List<Map<String, Integer>>> userEventCounts = new HashMap<>();

        // Iterate through each event in the JSON array
        for (JsonNode node : jsonNodes) {
            // Check if the event has both "type" and "actor" fields
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

        return userEventCounts; // Return the map of user event counts
    }

    /**
     * Identifies users with the maximum number of events in the archive data.
     * It returns a map containing the usernames with the highest event counts and the maximum count.
     *
     * @param jsonNodes The ArrayNode containing the JSON data.
     * @return A map containing the list of users with the maximum event counts and the max count value.
     */
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

        return result; // Return the map containing users with max events
    }

    public String getArchiveSH(String repoUrl) throws IOException {

        String url = utils.constructUrl("softwareheritage", repoUrl);

        return utils.downloadRepo(url);
    }
}
