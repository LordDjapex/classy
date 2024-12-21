package com.classy;

import com.classy.annotations.GenerateModel;
import com.classy.enums.SqlColumnTypes;
import com.classy.template.ForeignKeyMetaData;
import com.classy.utils.FileSystemUtils;
import com.classy.utils.QueryUtils;
import com.classy.utils.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static com.classy.mapper.SqlTypeMapper.mapSqlTypeToJavaType;
import static com.classy.utils.Constants.tableLowercasePlaceholder;
import static com.classy.utils.Constants.tablePlaceholder;

public class ClasslessProcessor extends AbstractProcessor {

    private static final String CLASSY_MODEL_PACKAGE = "classy.model";
    private static final String CLASSY_DTO_PACKAGE = "classy.dto";
    //   public static String[] exclude;
    String URL;
    String SCHEMA; //extract schema from URL
    String USERNAME;
    String PASSWORD;
    boolean annotateJPA;
    boolean generateDTOs;
    Connection connection = null;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "TEST WARNING");
        try {
            // Specify the file name and path in the source output location
            String packageName = "com.example.generated";
            String fileName = "GeneratedClass.java";

            // Create a source file in the SOURCE_OUTPUT location
            FileObject sourceFile = processingEnv.getFiler().createResource(
                    StandardLocation.SOURCE_OUTPUT, packageName, fileName);

            // Write the content of the generated class
            try (Writer writer = sourceFile.openWriter()) {
                writer.write("package " + packageName + ";\n\n");
                writer.write("public class GeneratedClass {\n");
                writer.write("    public static void sayHello() {\n");
                writer.write("        System.out.println(\"Hello from GeneratedClass!\");\n");
                writer.write("    }\n");
                writer.write("}\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (StandardLocation location : StandardLocation.values()) {

            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Name: " + location.getName());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Is Output Location: " + location.isOutputLocation());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Is Module-Oriented Location: " + location.isModuleOrientedLocation());
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "-------------------------");
        }
        if (!roundEnv.getElementsAnnotatedWith(GenerateModel.class).isEmpty()) {
            try {
                java.lang.Class.forName("com.mysql.cj.jdbc.Driver");
                Properties prop = new Properties();

                try (InputStream input = getClass().getClassLoader().getResourceAsStream("db_connect.properties")) {
                    prop.load(input);
                    URL = prop.getProperty("db.url");
                    SCHEMA = prop.getProperty("db.schema");
                    USERNAME = prop.getProperty("db.username");
                    PASSWORD = prop.getProperty("db.password");
                    annotateJPA = Boolean.parseBoolean(prop.getProperty("classy.annotateJPA"));
                    generateDTOs = Boolean.parseBoolean(prop.getProperty("classy.generateDTOs"));
                    //    exclude = annotation.exclude();
                } catch (Exception e) {
                    throw e;
                }

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Connecting database...");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Database connected!");

                PreparedStatement tableStatement = QueryUtils.extractTablesStatement(connection, SCHEMA);
                ResultSet tablesResultSet = tableStatement.executeQuery();

                List<String> templateSegments = FileSystemUtils.readAndSplitTemplate();

                while (tablesResultSet.next()) {
                    String tableName = tablesResultSet.getString("TABLE_NAME");
                    PreparedStatement preparedStatement = QueryUtils.extractTableColumns(connection, SCHEMA, tableName);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    createClass(tableName, resultSet);
                    for (String templateSegment: templateSegments) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Writing template:" + templateSegment);
                        writeTemplateSegment(tableName, resultSet, templateSegment);
                    }
                }

            } catch (Exception e) {
              //  processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,"Cannot connect the database!");
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,e.toString());
                return false;
            } finally {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Closing the connection.");
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException ignore) {

                    }
                }
            }
        }

            return true;
    }

    private void writeTemplateSegment(String name, ResultSet resultSet, String templateSegment) throws Exception {
        HashMap<String, String> placeholderMap = new HashMap<>();
        String className = StringUtils.transferTableNameIntoClassName(name);

        placeholderMap.put(tablePlaceholder, className);
        placeholderMap.put(tableLowercasePlaceholder, StringUtils.lowercaseFirstLetter(className));

        String packageName = templateSegment.split("\\R")[0].replace("package ", "");

        String classContent = templateSegment;
        for (Map.Entry<String, String> entry : placeholderMap.entrySet()) {
            classContent = classContent.replace(entry.getKey(), entry.getValue());
        }

        String packagePath = packageName.replace('.', '/').replace(";", "");
        String projectDir = System.getProperty("user.dir");
        File packageDir = new File(projectDir, "src/main/java/" + packagePath);

        // Create the directories if they don't exist
        if (!packageDir.exists()) {
            if (!packageDir.mkdirs()) {
                throw new IOException("Failed to create package directory: " + packageDir.getAbsolutePath());
            }
        }

        // Define the file path for the Java class
        File javaFile = new File(packageDir, className + ".java");


        // Write the class content to the file
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Writing file: " + javaFile.getPath());
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"User dir: " + System.getProperty("user.dir"));
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Writing file: " + javaFile.getAbsolutePath());
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write(classContent);
        }
    }
    private JavaFileObject createClass(String name, ResultSet resultSet) throws IOException, SQLException {
        JavaFileObject ourClass = null;
        BufferedWriter bufferedWriter = null;
        String className = StringUtils.transferTableNameIntoClassName(name);
        ourClass = processingEnv.getFiler().createSourceFile(className);

        List<List<String>> fields = extractFields(resultSet);

        List<String> otherFields = fields.get(0);
        List<String> primaryKeyField = fields.get(1);

        //package
        bufferedWriter = new BufferedWriter(ourClass.openWriter());
        bufferedWriter.append("package classy.model;");
        bufferedWriter.newLine();
        bufferedWriter.newLine();

        //imports and class annotations
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

        bufferedWriter.append("@Data");
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

        for (String field :primaryKeyField) {
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
        HashMap<String, List<ForeignKeyMetaData>> foreignKeys = extractForeignKeys(name);
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
                        bufferedWriter.append("private " + CLASSY_MODEL_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "ManyToOne":
                        if (annotateJPA) {
                            bufferedWriter.append("@ManyToOne");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\")");
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.append("private " + CLASSY_MODEL_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "OneToMany":
                        if (annotateJPA) {
                            bufferedWriter.append("@OneToMany(mappedBy =\"" + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), false) + "\")");
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.append("private List<" + CLASSY_MODEL_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getTableName()) + "> " +
                                StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "ManyToMany":
                        if (annotateJPA) {
                            bufferedWriter.append("@ManyToMany");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JoinTable(name = \"" + foreignKeyMetaData.getTableName() +
                                                "\", joinColumns = @JoinColumn(name = \"" +  foreignKeyMetaData.getColumnName() + "\"), " +
                                                " inverseJoinColumns = @JoinColumn(name = \"" + foreignKeyMetaData.getReferencedColumnName() + "\"))");
                            bufferedWriter.newLine();

                        }
                        bufferedWriter.append("private List<" + CLASSY_MODEL_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "> " +
                                                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                }

            }
        }

        bufferedWriter.append("}");
        bufferedWriter.close();

        if (generateDTOs) {
            createDTOClass(className, otherFields, primaryKeyField, foreignKeys);
        }

        return ourClass;
    }


    private List<List<String>> extractFields(ResultSet resultSet) throws SQLException {
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

    private String determineType(String columnType) throws SQLException {
        return mapSqlTypeToJavaType(SqlColumnTypes.valueOf(columnType));
    }

    private HashMap<String, List<ForeignKeyMetaData>> extractForeignKeys(String table) throws SQLException {

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

    private void createDTOClass(String className, List<String> fields,
                                                List<String> primaryKeyField,
                                                HashMap<String, List<ForeignKeyMetaData>> foreignKeys)
                                                throws IOException {

        JavaFileObject ourClass = null;
        BufferedWriter bufferedWriter = null;
        className = StringUtils.capitalize(className);
        ourClass = processingEnv.getFiler().createSourceFile(className + "DTO");

        //package
        bufferedWriter = new BufferedWriter(ourClass.openWriter());
        bufferedWriter.append("package classy.dto;");
        bufferedWriter.newLine();
        bufferedWriter.newLine();

        //imports and class annotations
        bufferedWriter.append("import java.util.List;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.*;");
        bufferedWriter.newLine();
        bufferedWriter.append("import java.sql.*;");
        bufferedWriter.newLine();
        bufferedWriter.append("import classy.model.*;");
        bufferedWriter.newLine();
        bufferedWriter.newLine();

        bufferedWriter.append("@Data");
        bufferedWriter.newLine();
        bufferedWriter.append("@Builder");
        bufferedWriter.newLine();
        bufferedWriter.append("@NoArgsConstructor");
        bufferedWriter.newLine();
        bufferedWriter.append("@AllArgsConstructor");
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
                        bufferedWriter.append("private " + CLASSY_DTO_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "OneToMany":
                    case "ManyToMany":
                        bufferedWriter.append("private List<" +CLASSY_DTO_PACKAGE + "." + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + "> " +
                                StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName(), true) + ";");
                        bufferedWriter.newLine();
                        break;
                }
            }
        }

        bufferedWriter.append("}");
        bufferedWriter.close();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList("com.classy.annotations.GenerateModel"));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
