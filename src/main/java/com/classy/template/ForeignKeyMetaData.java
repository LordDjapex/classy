package com.classy.template;

public class ForeignKeyMetaData {

    public ForeignKeyMetaData(String referencedTableName, String referencedColumnName, String columnName, String tableName) {
        this.referencedTableName = referencedTableName;
        this.referencedColumnName = referencedColumnName;
        this.columnName = columnName;
        this.tableName = tableName;
    }

    private String relationshipType;
    private String referencedTableName;
    private String referencedColumnName;

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public void setReferencedColumnName(String referencedColumnName) {
        this.referencedColumnName = referencedColumnName;
    }

    private String columnName;
    private String tableName;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(String relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public void setReferencedTableName(String referencedTableName) {
        this.referencedTableName = referencedTableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
