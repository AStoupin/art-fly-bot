package ru.artfly.bot.booking;

import java.util.ArrayList;
import java.util.List;

/** Минимальный CSV-кодек (RFC 4180): поля с запятыми и кавычками берутся в кавычки. */
final class Csv {

    private Csv() {
    }

    static String formatLine(String... fields) {
        var sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    static List<String> parseLine(String line) {
        var fields = new ArrayList<String>();
        var current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }

    private static String escape(String value) {
        String v = value.replace('\r', ' ').replace('\n', ' ');
        if (v.contains(",") || v.contains("\"")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
