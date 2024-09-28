package com.devskill.devskill_api.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@Component
public class Utils {

    // URL patterns as a final string
    private static final String urlArchive = "https://data.gharchive.org/%s.json.gz";
    private static final String urlSofwareHeritage = "https://archive.softwareheritage.org";

    // Constructor
    public Utils() {

    }
    // Method to construct the URL
    public static String constructUrl(String type, String path) {
        System.out.println("i");
        String urlPattern = type.equals("softwareheritage") ? urlSofwareHeritage : urlArchive;
        return String.format( urlPattern, path); // Use the instance variable
    }
}
