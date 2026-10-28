package edu.utsa.cs3443.picpick.model;

/** CSV escaping, including spreadsheet formula protection for user-supplied filenames. */
public final class Csv {
    private Csv() { }
    public static String field(String value) {
        String trimmed = value.trim();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
