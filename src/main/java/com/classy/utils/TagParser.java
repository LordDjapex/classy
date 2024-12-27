package com.classy.utils;

public class TagParser {

    public static String[] parseClassName(String templateSegment) {

        String className = null;
        int start = templateSegment.indexOf("#CLASS_NAME=");

        if (start != -1) {
            int end = templateSegment.indexOf("\n", start);  // Find next newline
            if (end == -1) end = templateSegment.length();   // Handle last line (no newline at end)

            className = templateSegment.substring(start + 12, end).trim();  // Extract class name
            templateSegment = templateSegment.substring(0, start) + templateSegment.substring(end + 1);

            String[] result = new String[2];

            result[0] = className;
            result[1] = templateSegment;

            return result;
        }

        throw new RuntimeException(Constants.CLASS_NAME_NOT_FOUND_EXCEPTION);
    }
}
