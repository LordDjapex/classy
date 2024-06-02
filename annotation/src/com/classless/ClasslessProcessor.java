package com.classless;

import com.classless.annotations.GenerateModel;
import com.classless.annotations.GenerateDTO;
import com.classless.annotations.JPAAnnotations;
import com.classless.enums.SqlColumnTypes;
import com.classless.template.ForeignKeyMetaData;
import com.classless.utils.QueryUtils;
import com.classless.utils.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
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
import java.util.Set;

import static com.classless.mapper.SqlTypeMapper.mapSqlTypeToJavaType;

public class ClasslessProcessor extends AbstractProcessor {

    //next step, check onetoone, onetomany, manytoone, manytomany

    public static final String URL = "jdbc:mysql://localhost:3306/classless";
    public static final String SCHEMA = URL.split("/")[URL.split("/").length - 1]; //extract schema from URL
    public static final String USERNAME = "root";
    public static final String PASSWORD = "password";
    public static Connection connection = null;

    public static final String LOMBOK_DATA_IMPORT = "import lombok.Data;";
    public static final String LOMBOK_NOARGSCONSTRUCTOR_IMPORT = "import lombok.NoArgsConstructor;";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "TEST WARNING");

        try {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Connecting database...");
            java.lang.Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Database connected!");

            for (Element element : roundEnv.getElementsAnnotatedWith(GenerateModel.class)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, element.getSimpleName().toString().toLowerCase());
                DatabaseMetaData metaData = connection.getMetaData();

                PreparedStatement tableStatement = QueryUtils.extractTablesStatement(connection, SCHEMA);

                ResultSet tablesResultSet = tableStatement.executeQuery();

                while (tablesResultSet.next()) {
                    String tableName = tablesResultSet.getString("TABLE_NAME");
                    PreparedStatement preparedStatement = QueryUtils.extractTableColumns(connection, SCHEMA, tableName);
                    ResultSet resultSet = preparedStatement.executeQuery();
                    createClass(tableName, resultSet,  element);
                }
            }


        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,"Cannot connect the database!");
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,e.toString());
        } finally {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Closing the connection.");
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignore) {

                }
            }
        }

            return true;
    }

    private JavaFileObject createClass(String name, ResultSet resultSet, Element element) throws IOException, SQLException {
        JavaFileObject ourClass = null;
        BufferedWriter bufferedWriter = null;
       // PackageElement packageElement = (PackageElement)element.getEnclosingElement();
        String className = StringUtils.transferTableNameIntoClassName(name);
        ourClass = processingEnv.getFiler().createSourceFile(className);

        List<List<String>> fieldsAndImports = extractFieldsAndImports(resultSet);

        List<String> neededImports = fieldsAndImports.get(0);
        List<String> fields = fieldsAndImports.get(1);
        List<String> primaryKeyField = fieldsAndImports.get(2);

        //package
        bufferedWriter = new BufferedWriter(ourClass.openWriter());
        bufferedWriter.append("package ");
        bufferedWriter.append("com.experiment");
        bufferedWriter.append(";");
        bufferedWriter.newLine();

        //imports and class annotations
        bufferedWriter.append("import java.util.List;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.Data;");
        bufferedWriter.newLine();
        if (element.getAnnotation(JPAAnnotations.class) != null) {
            bufferedWriter.append("import jakarta.persistence.*;");
            bufferedWriter.newLine();
        }
        for (String neededImport : neededImports) {
            bufferedWriter.append("import " + neededImport + ";");
            bufferedWriter.newLine();
        }
        //experimental
        bufferedWriter.append("import com.fasterxml.jackson.annotation.*;");
        bufferedWriter.newLine();

        //adds additional line so generated class is more readable
        bufferedWriter.newLine();

        bufferedWriter.append("@Data");
        bufferedWriter.newLine();
        if (element.getAnnotation(JPAAnnotations.class) != null) {
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
            if (element.getAnnotation(JPAAnnotations.class) != null) {
                bufferedWriter.append("@Id");
                bufferedWriter.newLine();
                bufferedWriter.append("@GeneratedValue(strategy = GenerationType.IDENTITY)");
                bufferedWriter.newLine();
                bufferedWriter.append("@Column(name = \"" + StringUtils.convertCamelToSnake(field.split(" ")[1]) + "\")");
                bufferedWriter.newLine();
            }
            bufferedWriter.append("private " + field + ";");
            bufferedWriter.newLine();
        }

        //adding fields
        for (String field : fields) {
            if (element.getAnnotation(JPAAnnotations.class) != null) {
                bufferedWriter.append("@Column(name = \"" + StringUtils.convertCamelToSnake(field.split(" ")[1]) + "\")");
                bufferedWriter.newLine();
            }
            bufferedWriter.append("private " + field + ";");
            bufferedWriter.newLine();
        }
        //adding foreign key fields

        /*
            TableName refers to table that references another table
            In case of OneToMany, we need to have tableName in order to actually know which table we having in our List

         */
        HashMap<String, List<ForeignKeyMetaData>> foreignKeys = extractForeignKeys(name);
        for (Map.Entry<String, List<ForeignKeyMetaData>> entry : foreignKeys.entrySet()) {
            String relationshipType = entry.getKey();
            List<ForeignKeyMetaData> foreignFields = entry.getValue();
            for (ForeignKeyMetaData foreignKeyMetaData : foreignFields) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,foreignKeyMetaData.toString());
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "" + element.getAnnotation(JPAAnnotations.class));
                switch (relationshipType) {
                    case "OneToOne":
                        if (element.getAnnotation(JPAAnnotations.class) != null) {
                            bufferedWriter.append("@OneToOne");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\")");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JsonIgnore");
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.append("private " + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName()) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "ManyToOne":
                        if (element.getAnnotation(JPAAnnotations.class) != null) {
                            bufferedWriter.append("@ManyToOne");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JoinColumn(name = \"" + foreignKeyMetaData.getColumnName() + "\")");
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.append("private " + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName()) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "OneToMany":
                        if (element.getAnnotation(JPAAnnotations.class) != null) {
                            bufferedWriter.append("@OneToMany(mappedBy =\"" + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName()) + "\")");
                            bufferedWriter.newLine();
                        }
                        bufferedWriter.append("private List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getTableName()) + "> " +
                                StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName()) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "ManyToMany":
                        if (element.getAnnotation(JPAAnnotations.class) != null) {
                            bufferedWriter.append("@ManyToMany");
                            bufferedWriter.newLine();
                            bufferedWriter.append("@JoinTable(name = \"" + foreignKeyMetaData.getTableName() +
                                                "\", joinColumns = @JoinColumn(name = \"" +  foreignKeyMetaData.getColumnName() + "\"), " +
                                                " inverseJoinColumns = @JoinColumn(name = \"" + foreignKeyMetaData.getReferencedColumnName() + "\"))");
                            bufferedWriter.newLine();

                        }
                        bufferedWriter.append("private List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "> " +
                                                                    StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName()) + ";");
                        bufferedWriter.newLine();
                        break;
                }

            }
        }



        bufferedWriter.append("}");
        bufferedWriter.close();

        if (element.getAnnotation(GenerateDTO.class) != null) {
            createDTOClass(className, neededImports, fields, primaryKeyField, foreignKeys);
        }

        return ourClass;
    }


    private List<List<String>> extractFieldsAndImports(ResultSet resultSet) throws SQLException {
        List<List<String>> ultimateList = new ArrayList<>();
        List<String> primaryKeyField = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        while (resultSet.next()) {
            String columnName = resultSet.getString("COLUMN_NAME");
            String dataType = resultSet.getString("DATA_TYPE").toUpperCase();
            String columnKey = resultSet.getString("COLUMN_KEY");

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Name: " + columnName + ", Type: " + dataType);

            String columnJavaType = determineType(dataType);
            if (columnJavaType.contains(".")) {
                imports.add(columnJavaType);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, columnJavaType.toString());
                if (columnKey.equals("PRI")) {
                    primaryKeyField.add(columnJavaType.split("\\.")[columnJavaType.split("\\.").length - 1] + " " + columnName);
                } else {
                    fields.add(columnJavaType.split("\\.")[columnJavaType.split("\\.").length - 1] + " " + columnName); //adds field type and field name
                }
            } else {
                if (columnKey.equals("PRI")) {
                    primaryKeyField.add(columnJavaType + " " + columnName);
                } else {
                    fields.add(columnJavaType + " " + columnName);
                }
            }
        }

        ultimateList.add(imports);
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

    private void createDTOClass(String className, List<String> neededImports, List<String> fields,
                                                List<String> primaryKeyField,
                                                HashMap<String, List<ForeignKeyMetaData>> foreignKeys)
                                                throws IOException {

        JavaFileObject ourClass = null;
        BufferedWriter bufferedWriter = null;
        // PackageElement packageElement = (PackageElement)element.getEnclosingElement();
        className = StringUtils.capitalize(className);
        ourClass = processingEnv.getFiler().createSourceFile(className + "DTO");

        //package
        bufferedWriter = new BufferedWriter(ourClass.openWriter());
        bufferedWriter.append("package ");
        bufferedWriter.append("com.experiment.dto;");
        bufferedWriter.newLine();

        //imports and class annotations
        bufferedWriter.append("import java.util.List;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.Data;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.Builder;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.NoArgsConstructor;");
        bufferedWriter.newLine();
        bufferedWriter.append("import lombok.AllArgsConstructor;");
        bufferedWriter.newLine();
        bufferedWriter.append("import java.sql.*;");
        bufferedWriter.newLine();
        bufferedWriter.append("import com.experiment.*;");
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
            bufferedWriter.append("private " + field + ";");
            bufferedWriter.newLine();
        }

        for (String field : fields) {
            bufferedWriter.append("private " + field + ";");
            bufferedWriter.newLine();
        }

        for (Map.Entry<String, List<ForeignKeyMetaData>> entry : foreignKeys.entrySet()) {
            String relationshipType = entry.getKey();
            List<ForeignKeyMetaData> foreignFields = entry.getValue();
            for (ForeignKeyMetaData foreignKeyMetaData : foreignFields) {
                switch (relationshipType) {
                    case "OneToOne":
                    case "ManyToOne":
                        bufferedWriter.append("private " + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + " " + StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName()) + ";");
                        bufferedWriter.newLine();
                        break;
                    case "OneToMany":
                    case "ManyToMany":
                        bufferedWriter.append("private List<" + StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName()) + "DTO" + "> " +
                                StringUtils.transferColumnNameIntoPluralFieldName(foreignKeyMetaData.getTableName()) + ";");
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
        return new HashSet<>(Arrays.asList("com.classless.annotations.Class",
                                            "com.classless.annotations.JPAAnnotations",
                                            "com.classless.annotations.GenerateDTO"));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
