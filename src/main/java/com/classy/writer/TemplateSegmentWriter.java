package com.classy.writer;

import com.classy.utils.StringUtils;
import com.classy.utils.TagParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.classy.utils.Constants.*;

public class TemplateSegmentWriter {

    public static void writeTemplateSegment(String name, String templateSegment, String projectDirectory,
                                     String sourceDirectory) throws Exception {
        HashMap<String, String> placeholderMap = new HashMap<>();
        String tableName = StringUtils.transferTableNameIntoClassName(name);

        placeholderMap.put(tablePlaceholder, tableName);
        placeholderMap.put(tableLowercasePlaceholder, StringUtils.lowercaseFirstLetter(tableName));
        placeholderMap.put(tablePluralPlaceholder, StringUtils.transferColumnNameIntoPluralFieldName(tableName, false));

        String packageName = templateSegment.split("\\R")[0].replace("package ", "");

        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            templateSegment = templateSegment.replace(entry.getKey(), entry.getValue());
        }

        //Removes #className tag and returns you className at 0 and changed file at 1
        String[] fileNameAndUpdatedSegment = TagParser.parseClassName(templateSegment);
        String fileName = fileNameAndUpdatedSegment[0];
        String updateTemplate = fileNameAndUpdatedSegment[1];

        templateSegment = updateTemplate;

        String packagePath = packageName.replace('.', '/').replace(";", "");
        Path path = Paths.get(projectDirectory, sourceDirectory, packagePath);
        File packageDir = path.toFile();

        // Create the directories if they don't exist
        if (!packageDir.exists()) {
            if (!packageDir.mkdirs()) {
                throw new IOException("Failed to create package directory: " + packageDir.getAbsolutePath());
            }
        }

        // Define the file path for the Java class
        File javaFile = new File(packageDir, fileName + ".java");

        // Skip if the file already exists
        if (javaFile.exists()) {
            System.out.println("Skipping " + fileName + ".java (already exists).");
            return;
        }

        // Write the class content to the file
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(templateSegment);
        }
    }
}
