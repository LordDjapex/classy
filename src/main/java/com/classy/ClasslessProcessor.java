package com.classy;

import com.classy.annotations.GenerateModel;
import com.classy.utils.FileSystemUtils;
import com.classy.utils.QueryUtils;
import com.classy.writer.ModelWriter;
import com.classy.writer.TemplateSegmentWriter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClasslessProcessor extends AbstractProcessor {

    String URL;
    String SCHEMA; //extract schema from URL
    String USERNAME;
    String PASSWORD;
    boolean annotateJPA;
    boolean generateDTOs;
    String projectDirectory;
    String sourceDirectory;
    Connection connection = null;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "TEST WARNING");

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
                    projectDirectory = prop.getProperty("classy.projectDirectory");
                    sourceDirectory = prop.getProperty("classy.sourceDirectory");
                } catch (Exception e) {
                    throw e;
                }

                ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Connecting database...");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,"Database connected!");

                PreparedStatement tableStatement = QueryUtils.extractTablesStatement(connection, SCHEMA);
                ResultSet tablesResultSet = tableStatement.executeQuery();

                List<String> templateSegments = FileSystemUtils.readAndSplitTemplate();
                List<Future<?>> futures = new ArrayList<>();

                while (tablesResultSet.next()) {
                    String tableName = tablesResultSet.getString("TABLE_NAME");

                    Future<?> future = executor.submit(() -> {
                        try {
                            PreparedStatement preparedStatement = QueryUtils.extractTableColumns(connection, SCHEMA, tableName);
                            ResultSet resultSet = preparedStatement.executeQuery();
                            ModelWriter.createModelClass(tableName, resultSet, projectDirectory, sourceDirectory, annotateJPA, generateDTOs, connection);
                            for (String templateSegment : templateSegments) {
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Writing template:" + templateSegment);
                                TemplateSegmentWriter.writeTemplateSegment(tableName, templateSegment, projectDirectory, sourceDirectory);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    futures.add(future);
                }

                // Wait for all tasks to finish
                for (Future<?> future : futures) {
                    future.get();  // Ensures all tasks are completed before moving on
                }

                executor.shutdown();

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

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Arrays.asList("com.classy.annotations.GenerateModel"));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
