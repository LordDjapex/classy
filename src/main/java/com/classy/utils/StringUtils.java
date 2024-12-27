package com.classy.utils;

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
        String singular = transferColumnNameIntoFieldName(columnName, isForeignColumn);

        // Basic pluralization without regex
        if (singular.endsWith("s") || singular.endsWith("x") || singular.endsWith("z") ||
                singular.endsWith("ch") || singular.endsWith("sh")) {
            return singular + "es";
        } else if (singular.endsWith("y") && isConsonantBeforeY(singular)) {
            return singular.substring(0, singular.length() - 1) + "ies";
        } else {
            return singular + "s";
        }
    }

    // Helper method to check if 'y' has a consonant before it
    private static boolean isConsonantBeforeY(String word) {
        if (word.length() < 2) return false;
        char beforeY = word.charAt(word.length() - 2);
        return !"aeiou".contains(String.valueOf(beforeY).toLowerCase());
    }

    public static String lowercaseFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str; // Return the original string if it's null or empty
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }
}
