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


    public String findArchiveSH(String repoUrl) throws IOException {

        String url = utils.constructUrl("softwareheritage", repoUrl);

        return utils.downloadRepo(url);
    }
}

