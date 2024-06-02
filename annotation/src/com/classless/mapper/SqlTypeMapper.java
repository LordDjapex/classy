package com.classless.mapper;

import com.classless.enums.SqlColumnTypes;

import java.sql.SQLException;

public class SqlTypeMapper {

    public static String mapSqlTypeToJavaType(SqlColumnTypes sqlType) throws SQLException {
        switch (sqlType) {
            // Numeric types
            case TINYINT:
                return "Byte";
            case SMALLINT:
                return "Short";
            case MEDIUMINT:
            case INT:
                return "Integer";
            case BIGINT:
                return "Long";
            case FLOAT:
                return "Float";
            case DOUBLE:
                return "Double";
            case DECIMAL:
                return "java.math.BigDecimal";

            // Date and time types
            case DATE:
                return "java.sql.Date";
            case TIME:
                return "java.sql.Time";
            case DATETIME:
            case TIMESTAMP:
                return "java.sql.Timestamp";
            case YEAR:
                return "Short"; // Assuming MySQL YEAR is mapped to Java Short

            // String types
            case CHAR:
            case VARCHAR:
            case TINYTEXT:
            case TEXT:
            case MEDIUMTEXT:
            case LONGTEXT:
                return "String";
            case BINARY:
            case VARBINARY:
            case TINYBLOB:
            case BLOB:
            case MEDIUMBLOB:
            case LONGBLOB:
                return "byte[]";

            // Enumeration types
            case ENUM:
            case SET:
                return "String"; // Assuming MySQL ENUM and SET are mapped to Java String

            // Other types
            default:
                throw new SQLException("Unknown SQL Type: " + sqlType);
        }
    }
}
