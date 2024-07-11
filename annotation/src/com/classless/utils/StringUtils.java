package com.classless.utils;

public class StringUtils {

    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // Split the string into words using whitespace as delimiter
        String[] words = str.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            // Capitalize the first letter of each word and append the rest of the word
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }

        // Remove the extra space at the end and return the result
        return result.toString().trim();
    }

    public static String transferColumnNameIntoFieldName(String columnName, boolean isForeignColumn) {
        String[] words = columnName.split("_");
        String fieldName = "";
        for (String word : words) {
            if (word == words[0]) {
                fieldName += word;
            } else {
                fieldName += capitalize(word);
            }
        }

        //if column name ends with Id, it shouldn't be there
        if (fieldName.endsWith("Id") && isForeignColumn) {
            return fieldName.substring(0, fieldName.length() - 2);
        }

        return fieldName;
    }

    public static String transferTableNameIntoClassName(String tableName) {
        String[] words = tableName.split("_");
        String className = "";
        for (String word : words) {
            className += capitalize(word);
        }

        return className;
    }

    public static String convertCamelToSnake(String input) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (Character.isUpperCase(currentChar)) {
                if (i > 0) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(currentChar));
            } else {
                result.append(currentChar);
            }
        }
        return result.toString();
    }

    public static String transferColumnNameIntoPluralFieldName(String columnName, boolean isForeignColumn) {
        return transferColumnNameIntoFieldName(columnName, isForeignColumn) + "s";
    }
}
