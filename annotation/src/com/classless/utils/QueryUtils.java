package com.classless.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class QueryUtils {

    public static PreparedStatement extractTablesStatement(Connection connection, String schema) throws SQLException {
            //extracts all tables that don't have composite primary key
            PreparedStatement tableStatement = connection.prepareStatement("SELECT table_name\n" +
                                            "FROM information_schema.tables\n" +
                                            "WHERE table_schema = ? AND TABLE_TYPE = 'BASE TABLE' \n" +
                                            "  AND table_name NOT IN (\n" +
                                            "    SELECT tc.table_name\n" +
                                            "    FROM information_schema.table_constraints tc\n" +
                                            "    JOIN information_schema.key_column_usage kcu\n" +
                                            "      ON tc.constraint_name = kcu.constraint_name\n" +
                                            "     AND tc.table_schema = kcu.table_schema\n" +
                                            "     AND tc.table_name = kcu.table_name\n" +
                                            "    WHERE tc.table_schema = ?\n" +
                                            "      AND tc.constraint_type = 'PRIMARY KEY'\n" +
                                            "    GROUP BY tc.table_name\n" +
                                            "    HAVING COUNT(*) >= 2\n" +
                                            "  );");

            tableStatement.setString(1, schema);
            tableStatement.setString(2, schema);

            return tableStatement;
    }


    // Get the ResultSet containing column metadata
    //extracts columns that are not foreign keys and not manytomany tables
    public static PreparedStatement extractTableColumns(Connection connection, String schema, String tableName) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT DISTINCT c.table_name, c.column_name, c.data_type, c.column_key \n" +
                "FROM INFORMATION_SCHEMA.COLUMNS c\n" +
                "LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu on c.column_name = kcu.column_name\n" +
                "AND c.table_schema = kcu.table_schema\n" +
                "AND c.table_name = kcu.table_name\n" +
                " where c.table_schema = ? \n" +
                " and c.table_name = ? \n" +
                " AND kcu.REFERENCED_COLUMN_NAME IS NULL\n" +
                " AND kcu.REFERENCED_TABLE_NAME IS NULL\n" +
                "AND NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE  a\n" +
                " where a.table_schema = ?\n" +
                " and a.table_name = ?\n" +
                " and a.column_name = c.column_name\n" +
                " AND a.REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                " AND a.REFERENCED_TABLE_NAME IS NOT NULL);");


        preparedStatement.setString(1, schema);
        preparedStatement.setString(2, tableName);
        preparedStatement.setString(3, schema);
        preparedStatement.setString(4, tableName);

        return preparedStatement;
    }

    public static PreparedStatement extractForeignConstraints(Connection connection, String schema, String tableName) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT 'ManyToOne' AS relationship_type, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME, kcu.COLUMN_NAME, kcu.TABLE_NAME\n" +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n" +
                    "WHERE kcu.TABLE_SCHEMA = ? \n" +
                    "    AND kcu.TABLE_NAME = ? \n" +
                    "    AND kcu.REFERENCED_TABLE_NAME IS NOT NULL \n" +
                    "    AND kcu.REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                    "    AND NOT EXISTS (\n" +
                    "        SELECT 1 \n" +
                    "        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE \n" +
                    "        WHERE TABLE_SCHEMA = kcu.TABLE_SCHEMA \n" +
                    "            AND REFERENCED_TABLE_NAME = kcu.TABLE_NAME\n" +
                    "            AND TABLE_NAME = kcu.REFERENCED_TABLE_NAME\n" +
                    "    )\n" +
                    "    UNION\n" +
                    "      SELECT 'OneToOne' AS relationship_type, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME, kcu.COLUMN_NAME, kcu.TABLE_NAME\n" +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n" +
                    "WHERE kcu.TABLE_SCHEMA = ? \n" +
                    "    AND kcu.TABLE_NAME = ? \n" +
                    "    AND kcu.REFERENCED_TABLE_NAME IS NOT NULL \n" +
                    "    AND kcu.REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                    "    AND EXISTS (\n" +
                    "        SELECT 1 \n" +
                    "        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE \n" +
                    "        WHERE TABLE_SCHEMA = kcu.TABLE_SCHEMA \n" +
                    "            AND REFERENCED_TABLE_NAME = kcu.TABLE_NAME\n" +
                    "    )\n" +
                    "UNION\n" +
                    "    SELECT 'OneToMany' AS relationship_type, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME, kcu.COLUMN_NAME, kcu.TABLE_NAME\n" +
                    "    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n" +
                    "WHERE kcu.TABLE_SCHEMA = ? \n" +
                    "AND kcu.TABLE_NAME != ? \n" +
                    "AND kcu.REFERENCED_TABLE_NAME = ? \n" +
                    "AND kcu.REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                    "AND NOT EXISTS (\n" +
                    "        SELECT 1 \n" +
                    "        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE a\n" +
                    "        WHERE a.TABLE_SCHEMA = ? \n" +
                    "            AND kcu.REFERENCED_TABLE_NAME = a.TABLE_NAME\n" +
                    "    ) AND \n" +
                    "   table_name NOT IN (\n" +
                    "    SELECT tc.table_name\n" +
                    "    FROM information_schema.table_constraints tc\n" +
                    "    JOIN information_schema.key_column_usage kcu\n" +
                    "      ON tc.constraint_name = kcu.constraint_name\n" +
                    "     AND tc.table_schema = kcu.table_schema\n" +
                    "     AND tc.table_name = kcu.table_name\n" +
                    "    WHERE tc.table_schema = 'classless'\n" +
                    "      AND tc.constraint_type = 'PRIMARY KEY'\n" +
                    "    GROUP BY tc.table_name\n" +
                    "    HAVING COUNT(*) >= 2\n" +
                    "  )" +
                    "UNION\n" +
                    "SELECT 'ManyToMany' AS relationship_type, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME, kcu.COLUMN_NAME, kcu.TABLE_NAME\n" +
                    "    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu\n" +
                    "WHERE kcu.TABLE_SCHEMA = ? \n" +
                    "        AND kcu.TABLE_NAME != ? \n" +
                    "        AND kcu.REFERENCED_TABLE_NAME != ? \n" +
                    "        AND kcu.REFERENCED_COLUMN_NAME IS NOT NULL\n" +
                    "AND EXISTS (\n" +
                    "        SELECT 1 \n" +
                    "        FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE \n" +
                    "        WHERE TABLE_SCHEMA = kcu.TABLE_SCHEMA \n" +
                    "and TABLE_NAME = kcu.TABLE_NAME\n" +
                    "            AND REFERENCED_TABLE_NAME = ? \n" +
                    "            AND REFERENCED_COLUMN_NAME = (SELECT COLUMN_NAME\n" +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE\n" +
                    "WHERE TABLE_SCHEMA = ? \n" +
                    "AND TABLE_NAME = ? \n" +
                    "AND CONSTRAINT_NAME = 'PRIMARY'\n" +
                    "LIMIT 1 \n" +
                    ")\n" +
                    ");");

        preparedStatement.setString(1, schema);
        preparedStatement.setString(2, tableName);
        preparedStatement.setString(3, schema);
        preparedStatement.setString(4, tableName);
        preparedStatement.setString(5, schema);
        preparedStatement.setString(6, tableName);
        preparedStatement.setString(7, tableName);
        preparedStatement.setString(8, schema);
        preparedStatement.setString(9, schema);
        preparedStatement.setString(10, tableName);
        preparedStatement.setString(11, tableName);
        preparedStatement.setString(12, tableName);
        preparedStatement.setString(13, schema);
        preparedStatement.setString(14, tableName);

        return preparedStatement;
    }
}
