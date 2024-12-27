package com.classy.writer;

import com.classy.template.ForeignKeyMetaData;
import com.classy.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.classy.writer.GetterSetterWriter.writeGetterSetter;
import static com.classy.writer.GetterSetterWriter.writeGettersSetters;

public class DTOWriter {

    public static void createDTOClass(String className, List<String> fields, List<String> primaryKeyField,
                                HashMap<String, List<ForeignKeyMetaData>> foreignKeys, String projectDirectory,
                                      String sourceDirectory) throws IOException {

        className = StringUtils.capitalize(className);
        String packageName = "com.classy.dto";
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
        File javaFile = new File(packageDir, className + "DTO.java");

        // Skip if the file already exists
        if (javaFile.exists()) {
            System.out.println("Skipping " + className + ".java (already exists).");
            return;
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(javaFile))) {
            //package
            bufferedWriter.append("package ");
            bufferedWriter.append(packageName);
            bufferedWriter.append(";");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            //imports and class annotations
            bufferedWriter.append("import java.util.List;");
            bufferedWriter.newLine();
            bufferedWriter.append("import java.sql.*;");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            //class and fields
            bufferedWriter.append("public class ");
            bufferedWriter.append(className + "DTO");
            bufferedWriter.append("{");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            for (String field : primaryKeyField) {
                bufferedWriter.append("private " + StringUtils.transferColumnNameIntoFieldName(field, false) + ";");
                bufferedWriter.newLine();
            }

            for (String field : fields) {
                bufferedWriter.append("private " + StringUtils.transferColumnNameIntoFieldName(field, false) + ";");
                bufferedWriter.newLine();
            }

            for (Map.Entry<String, List<ForeignKeyMetaData>> entry : foreignKeys.entrySet()) {
                String relationshipType = entry.getKey();
                List<ForeignKeyMetaData> foreignFields = entry.getValue();
                for (ForeignKeyMetaData foreignKeyMetaData : foreignFields) {
                    switch (relationshipType) {
                        case "OneToOne":
                        case "ManyToOne":
                            bufferedWriter.append("private " + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO",
                                    StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true));
                            bufferedWriter.newLine();
                            break;
                        case "OneToMany":
                        case "ManyToMany":
                            bufferedWriter.append("private List<"   + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + "> " +
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, "List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO>",
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true));
                            bufferedWriter.newLine();
                            break;
                    }
                }
            }

            writeGettersSetters(bufferedWriter, primaryKeyField);
            writeGettersSetters(bufferedWriter, fields);

            bufferedWriter.append("}");
        }
    }
}
