package csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV read/write helpers - no CSV/spreadsheet library on this project's classpath,
 * and business data (names, addresses) routinely contains commas, so a naive split(",")
 * would silently corrupt rows. This handles RFC4180-style quoted fields - a field containing
 * a comma, quote, or newline is wrapped in quotes with internal quotes doubled - both when
 * writing and when reading back a file exported by Excel/Google Sheets/this same app.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** One CSV row (CRLF-terminated, per RFC4180) with each field quoted only if it needs to be. */
    public static String writeRow(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuoting = field.indexOf(',') >= 0 || field.indexOf('"') >= 0
                || field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;
        if (!needsQuoting) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /**
     * Parses an entire CSV document into rows of fields. Treats any quote character
     * encountered outside an already-open quoted field as the start of one (rather than
     * requiring it to be the very first character of the field) - simpler than full RFC4180,
     * but matches every real-world spreadsheet export, which never partially quotes a field.
     */
    public static List<String[]> parse(String content) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int len = content.length();
        while (i < len) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i += 2;
                        continue;
                    }
                    inQuotes = false;
                    i++;
                    continue;
                }
                field.append(c);
                i++;
            } else if (c == '"') {
                inQuotes = true;
                i++;
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
                i++;
            } else if (c == '\r') {
                i++; // ignore - a paired '\n', if any, is handled on the next iteration
            } else if (c == '\n') {
                row.add(field.toString());
                field.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
                i++;
            } else {
                field.append(c);
                i++;
            }
        }
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }

    /** True if every field in the row is null/blank - callers typically skip these (e.g. a trailing blank line). */
    public static boolean isBlankRow(String[] row) {
        for (String field : row) {
            if (field != null && !field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Field at index, or "" if the row is shorter than expected - so a short row doesn't throw. */
    public static String field(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index] : "";
    }
}
