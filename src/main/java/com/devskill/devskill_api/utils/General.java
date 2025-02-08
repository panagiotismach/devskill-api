package com.devskill.devskill_api.utils;

public enum General {

     FILES(1000),
     MB(300);

     private final int value; // Field to store the value

     // Constructor to initialize the value
     General(int value) {
          this.value = value;
     }

     // Getter to retrieve the value
     public int getValue() {
          return value;
     }
}

