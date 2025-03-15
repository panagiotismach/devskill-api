package com.devskill.devskill_api.utils;

public enum General {

     FILES(1000),
     MB(300),
     GITHUB("https://github.com/"),
     GITHUB_TRENDING("https://github.com/trending"),
     FILES_FOLDER("files"),
     OUTPUT_FOLDER("output"),
     REPOS_FOLDER("repos");

     private final Integer value; // Nullable Integer field
     private final String text;    // Nullable String field

     // Constructor for integer values
     General(int value) {
          this.value = value;
          this.text = null;  // Default null for non-string enums
     }

     // Constructor for string values
     General(String text) {
          this.value = null; // Default null for non-integer enums
          this.text = text;
     }

     // Getter for integer value (returns null if not applicable)
     public Integer getValue() {
          return value;
     }

     // Getter for URL string (returns null if not applicable)
     public String getText() {
          return text;
     }
}
