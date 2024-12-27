package com.classy.writer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import com.classy.utils.StringUtils;

public class GetterSetterWriter {

    // Method to handle list of fields
    public static void writeGettersSetters(BufferedWriter bw, List<String> fields) throws IOException {
        for (String field : fields) {
            String[] parts = field.split(" ");
            String type = parts[0];
            String name = parts[1];

            writeGetterSetter(bw, type, name);
        }
    }

    public static void writeGetterSetter(BufferedWriter bw, String type, String name) throws IOException {
        String capitalized = StringUtils.capitalize(name);

        // Getter
        bw.newLine();
        bw.append("    public " + type + " get" + capitalized + "() {");
        bw.newLine();
        bw.append("        return " + name + ";");
        bw.newLine();
        bw.append("    }");
        bw.newLine();

        // Setter
        bw.newLine();
        bw.append("    public void set" + capitalized + "(" + type + " " + name + ") {");
        bw.newLine();
        bw.append("        this." + name + " = " + name + ";");
        bw.newLine();
        bw.append("    }");
        bw.newLine();
    }
}
