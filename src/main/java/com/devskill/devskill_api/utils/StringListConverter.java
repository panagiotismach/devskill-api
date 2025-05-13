package com.devskill.devskill_api.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        return list != null ? String.join(SEPARATOR, list) : "";
    }

    @Override
    public List<String> convertToEntityAttribute(String joined) {
        return joined != null && !joined.isEmpty()
                ? Arrays.stream(joined.split(SEPARATOR)).map(String::trim).collect(Collectors.toList())
                : List.of();
    }
}
