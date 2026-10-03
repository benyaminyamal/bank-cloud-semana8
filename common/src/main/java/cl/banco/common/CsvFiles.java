package cl.banco.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CsvFiles {

    private CsvFiles() {
    }

    public static List<Map<String, String>> read(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            List<String> headers = parseLine(headerLine);
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> values = parseLine(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String header = LegacyTexts.clean(headers.get(i));
                    row.put(header, i < values.size() ? values.get(i) : "");
                }
                rows.add(row);
            }
            return List.copyOf(rows);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el CSV legacy", ex);
        }
    }

    static List<String> parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
