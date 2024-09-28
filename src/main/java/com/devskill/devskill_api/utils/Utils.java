package com.devskill.devskill_api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

@Component
public class Utils {

    // URL patterns as a final string
    private static final String urlArchive = "https://data.gharchive.org/%s.json.gz";
    private static final String urlSofwareHeritage = "https://archive.softwareheritage.org";


    public Utils() {

    }

    // Method to construct the URL
    public static String constructUrl(String type, String path) {
        String urlPattern = type.equals("softwareheritage") ? urlSofwareHeritage : urlArchive;
        return String.format( urlPattern, path); // Use the instance variable
    }

}
