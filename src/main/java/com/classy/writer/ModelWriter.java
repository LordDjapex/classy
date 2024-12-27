package com.classy.writer;

import com.classy.enums.SqlColumnTypes;
import com.classy.template.ForeignKeyMetaData;
import com.classy.utils.QueryUtils;
import com.classy.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.classy.mapper.SqlTypeMapper.mapSqlTypeToJavaType;
import static com.classy.writer.GetterSetterWriter.writeGetterSetter;
import static com.classy.writer.GetterSetterWriter.writeGettersSetters;
//import static com.classy.writer.ModelToDtoConversionWriter.writeToDTO;

public class ModelWriter {

    public static void createModelClass(String name, ResultSet resultSet, String projectDirectory, String sourceDirectory,
                                  boolean annotateJPA, boolean generateDTOs, Connection connection) throws IOException, SQLException {
        String className = StringUtils.transferTableNameIntoClassName(name);
        String packageName = "com.classy.model";
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
        File javaFile = new File(packageDir, className + ".java");

        // Skip if the file already exists
        if (javaFile.exists()) {
            System.out.println("Skipping " + className + ".java (already exists).");
            return;
        }

        List<List<String>> fields = extractFields(resultSet);
        List<String> otherFields = fields.get(0);
        List<String> primaryKeyField = fields.get(1);

        //package
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(javaFile))) {
            bufferedWriter.append("package ");
            bufferedWriter.append(packageName);
            bufferedWriter.append(";");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            //imports and class annotations
            bufferedWriter.append("import com.classy.dto.*;");
            bufferedWriter.newLine();
            bufferedWriter.append("import java.util.List;");
            bufferedWriter.newLine();
            bufferedWriter.append("import lombok.Data;");
            bufferedWriter.newLine();
            bufferedWriter.append("import java.sql.*;");
            bufferedWriter.newLine();
            if (annotateJPA) {
                bufferedWriter.append("import jakarta.persistence.*;");
                bufferedWriter.newLine();
            }

            bufferedWriter.append("import com.fasterxml.jackson.annotation.*;");
            bufferedWriter.newLine();

            //adds additional line so generated class is more readable
            bufferedWriter.newLine();

            if (annotateJPA) {
                bufferedWriter.append("@Entity");
                bufferedWriter.newLine();
                bufferedWriter.append("@Table(name = \"" + name + "\")");
                bufferedWriter.newLine();
            }

            //class and fields
            bufferedWriter.append("public class ");
            bufferedWriter.append(className);
            bufferedWriter.append("{");
            bufferedWriter.newLine();
            bufferedWriter.newLine();

            for (String field : primaryKeyField) {
                if (annotateJPA) {
                    bufferedWriter.append("@Id");
                    bufferedWriter.newLine();
                    bufferedWriter.append("@GeneratedValue(strategy = GenerationType.IDENTITY)");
                    bufferedWriter.newLine();
                    bufferedWriter.append("@Column(name = \"" + StringUtils.convertCamelToSnake(field.split(" ")[1]) + "\")");
                    bufferedWriter.newLine();
                }
                bufferedWriter.append("private " + StringUtils.transferColumnNameIntoFieldName(field, false) + ";");
                bufferedWriter.newLine();
            }

            //adding fields
            for (String field : otherFields) {
                if (annotateJPA) {
                    bufferedWriter.append("@Column(name = \"" + StringUtils.convertCamelToSnake(field.split(" ")[1]) + "\")");
                    bufferedWriter.newLine();
                }
                bufferedWriter.append("private " + StringUtils.transferColumnNameIntoFieldName(field, false) + ";");
                bufferedWriter.newLine();
            }

            //adding foreign key fields
        /*
            TableName refers to table that references another table
            In case of OneToMany, we need to have tableName in order to actually know which table we should have in our List
         */
            HashMap<String, List<ForeignKeyMetaData>> foreignKeys = extractForeignKeys(name, connection);
            for (Map.Entry<String, List<ForeignKeyMetaData>> entry : foreignKeys.entrySet()) {
                String relationshipType = entry.getKey();
                List<ForeignKeyMetaData> foreignFields = entry.getValue();
                for (ForeignKeyMetaData foreignKeyMetaData : foreignFields) {
                    switch (relationshipType) {
                        case "OneToOne":
                            if (annotateJPA) {
                                bufferedWriter.append("@OneToOne");
                                bufferedWriter.newLine();
                                bufferedWriter.append("@JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\")");
                                bufferedWriter.newLine();
                                bufferedWriter.append("@JsonIgnore");
                                bufferedWriter.newLine();
                            }
                            bufferedWriter.append("private " + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()),
                                    StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true));
                            bufferedWriter.newLine();
                            break;
                        case "ManyToOne":
                            if (annotateJPA) {
                                bufferedWriter.append("@ManyToOne");
                                bufferedWriter.newLine();
                                bufferedWriter.append("@JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\")");
                                bufferedWriter.newLine();
                            }
                            bufferedWriter.append("private "  + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()),
                                    StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true));
                            bufferedWriter.newLine();
                            break;
                        case "OneToMany":
                            if (annotateJPA) {
                                bufferedWriter.append("@OneToMany(mappedBy =\"" + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), false) + "\")");
                                bufferedWriter.newLine();
                            }
                            bufferedWriter.append("private List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getTableName()) + "> " +
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, "List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getTableName()) + ">",
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true));
                            bufferedWriter.newLine();
                            break;
                        case "ManyToMany":
                            if (annotateJPA) {
                                bufferedWriter.append("@ManyToMany");
                                bufferedWriter.newLine();
                                bufferedWriter.append("@JoinTable(name = \"" + foreignKeyMetaData.getTableName() +
                                        "\", joinColumns = @JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\"), " +
                                        " inverseJoinColumns = @JoinColumn(name = \"" + foreignKeyMetaData.getReferencedColumnName() + "\"))");
                                bufferedWriter.newLine();

                            }
                            bufferedWriter.append("private List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "> " +
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                            bufferedWriter.newLine();
                            writeGetterSetter(bufferedWriter, "List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + ">",
                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true));
                            bufferedWriter.newLine();
                            break;
                    }
                }
            }

            writeGettersSetters(bufferedWriter, primaryKeyField);
            writeGettersSetters(bufferedWriter, otherFields);

            ModelToDtoConversionWriter.writeToDTO(className, primaryKeyField, otherFields, foreignKeys, bufferedWriter);

            bufferedWriter.append("}");
            bufferedWriter.close();

            if (generateDTOs) {
                DTOWriter.createDTOClass(className, otherFields, primaryKeyField, foreignKeys, projectDirectory, sourceDirectory);
            }

        }
    }

    private static List<List<String>> extractFields(ResultSet resultSet) throws SQLException {
        List<List<String>> ultimateList = new ArrayList<>();
        List<String> primaryKeyField = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("DATA_TYPE").toUpperCase();
            String columnKey = resultSet.getString("COLUMN_KEY");

            //  processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Name: " + columnName + ", Type: " + dataType);

            String columnJavaType = determineType(dataType);
            if (columnKey.equals("PRI")) {
                primaryKeyField.add(columnJavaType + " " + columnName);
            } else {
                fields.add(columnJavaType + " " + columnName);
            }
        }

        ultimateList.add(fields);
        ultimateList.add(primaryKeyField);

        return ultimateList;
    }

    private static HashMap<String, List<ForeignKeyMetaData>> extractForeignKeys(String table, Connection connection) throws SQLException {

        String schema = "classless";

        HashMap<String, List<ForeignKeyMetaData>> foreignKeyMappings = new HashMap<>();

        foreignKeyMappings.put("ManyToOne", new ArrayList<>());
        foreignKeyMappings.put("OneToOne", new ArrayList<>());
        foreignKeyMappings.put("OneToMany", new ArrayList<>());
        foreignKeyMappings.put("ManyToMany", new ArrayList<>());


        PreparedStatement preparedStatement = QueryUtils.extractForeignConstraints(connection, schema, table);
        ResultSet rs = preparedStatement.executeQuery();
        while (rs.next()) {
            ForeignKeyMetaData relationship = new ForeignKeyMetaData(rs.getString("REFERENCED_TABLE_NAME"),
                    rs.getString("REFERENCED_COLUMN_NAME"),
                    rs.getString("COLUMN_NAME"),
                    rs.getString("TABLE_NAME"));

            foreignKeyMappings.get(rs.getString("relationship_type")).add(relationship);
        }

        return foreignKeyMappings;

    }

    private static String determineType(String columnType) throws SQLException {
        return mapSqlTypeToJavaType(SqlColumnTypes.valueOf(columnType));
    }
}
