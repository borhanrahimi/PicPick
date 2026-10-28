package edu.utsa.cs3443.picpick;

import edu.utsa.cs3443.picpick.model.Csv;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CsvTest {
    @Test public void preservesFilenamesContainingCommasQuotesAndNewlines() {
        assertEquals("\"trip, \"\"summer\"\"\n2026.jpg\"", Csv.field("trip, \"summer\"\n2026.jpg"));
    }
    @Test public void preventsImportedFilenameFromBecomingSpreadsheetFormula() {
        assertEquals("\"'=SUM(1,2).jpg\"", Csv.field("=SUM(1,2).jpg"));
        assertEquals("\"'  @example.jpg\"", Csv.field("  @example.jpg"));
    }
    @Test public void ordinaryNamesAndEmptyNamesRemainValidCsv() {
        assertEquals("\"holiday.jpg\"", Csv.field("holiday.jpg"));
        assertEquals("\"\"", Csv.field(""));
    }
}
