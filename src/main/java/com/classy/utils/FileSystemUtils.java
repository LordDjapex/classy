package com.classy.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.classy.utils.Constants.templateBreakDelimiter;
import static com.classy.utils.Constants.templateFileName;

public class FileSystemUtils {

    public static List<String> readAndSplitTemplate() {
        List<String> segments = new ArrayList<>();
        try {
            // Load the file from the resources directory
            ClassLoader classLoader = FileSystemUtils.class.getClassLoader();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    classLoader.getResourceAsStream(templateFileName), StandardCharsets.UTF_8))) {

                // Read the file content into a single string
                StringBuilder contentBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    contentBuilder.append(line).append(System.lineSeparator());
                }

                // Split the content by the delimiter
                String[] splitContent = contentBuilder.toString().split(templateBreakDelimiter);
                for (String part : splitContent) {
                    segments.add(part.trim()); // Trim to remove unnecessary whitespace
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading template file: " + e.getMessage(), e);
        }
        return segments;
    }
}
