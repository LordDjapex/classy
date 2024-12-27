package com.classy.writer;

import com.classy.template.ForeignKeyMetaData;
import com.classy.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ModelToDtoConversionWriter {

    public static void writeToDTO(String className, List<String> primaryKeyField, List<String> fields, Map<String, List<ForeignKeyMetaData>> foreignKeys,
                                  BufferedWriter writer) throws IOException {
        writer.append("public ").append(className).append("DTO toDTO() {");
        writer.newLine();
        writer.append("    ").append(className).append("DTO dto = new ").append(className).append("DTO();");
        writer.newLine();

        for (String field : primaryKeyField) {
            String fieldName = field.split(" ")[1];
            writer.append("    dto.set").append(StringUtils.capitalize(fieldName))
                    .append("(this.get").append(StringUtils.capitalize(fieldName)).append("());");
            writer.newLine();
        }

        for (String field : fields) {
            String fieldName = field.split(" ")[1];
            writer.append("    dto.set").append(StringUtils.capitalize(fieldName))
                    .append("(this.get").append(StringUtils.capitalize(fieldName)).append("());");
            writer.newLine();
        }
        for (Map.Entry<String, List<ForeignKeyMetaData>> entry : foreignKeys.entrySet()) {
            List<ForeignKeyMetaData> foreignFields = entry.getValue();
            for (ForeignKeyMetaData foreignKeyMetaData : foreignFields) {
                String referencedClass = StringUtils.transferTableNameIntoClassName(foreignKeyMetaData.getReferencedTableName());
                String fieldName = StringUtils.transferColumnNameIntoFieldName(foreignKeyMetaData.getColumnName(), true);

                switch (entry.getKey()) {
                    case "ManyToOne":
                    case "OneToOne":
                        writer.append("    if (this.").append(fieldName).append(" != null) {");
                        writer.newLine();
                        writer.append("        dto.set").append(StringUtils.capitalize(fieldName))
                                .append("(this.").append(fieldName).append(".toDTO());");
                        writer.newLine();
                        writer.append("    }");
                        writer.newLine();
                        break;
                    case "OneToMany":
                    case "ManyToMany":
                        writer.append("    if (this.").append(fieldName).append(" != null) {");
                        writer.newLine();
                        writer.append("        dto.set").append(StringUtils.capitalize(fieldName))
                                .append("(this.").append(fieldName)
                                .append(".stream().map(").append(referencedClass).append("::toDTO).collect(Collectors.toList()));");
                        writer.newLine();
                        writer.append("    }");
                        writer.newLine();
                        break;
                }
            }
        }
        writer.append("    return dto;");
        writer.newLine();
        writer.append("}");
    }
}
