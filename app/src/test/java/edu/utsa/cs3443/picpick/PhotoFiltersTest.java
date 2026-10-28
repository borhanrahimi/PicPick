package edu.utsa.cs3443.picpick;

import edu.utsa.cs3443.picpick.model.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PhotoFiltersTest {
    private final List<Photo> library = Arrays.asList(
        new Photo(0, "sample.png", "Beach Sunset", Photo.Status.KEEP),
        new Photo(1, "imports/abc", "beach trip.jpg", Photo.Status.UNASSIGNED),
        new Photo(2, "imports/def", "Receipt.png", Photo.Status.TRASH));
    @Test public void combinesCaseInsensitiveSearchStatusAndSource() {
        assertEquals(1, PhotoFilters.apply(library, " BEACH ", Photo.Status.UNASSIGNED, 1).size());
        assertEquals("imports/abc", PhotoFilters.apply(library, "beach", null, 1).get(0).getFilePath());
        assertTrue(PhotoFilters.apply(library, "beach", Photo.Status.TRASH, 0).isEmpty());
    }
    @Test public void allFiltersReturnsLibraryAndDoesNotMutateIt() {
        assertEquals(3, PhotoFilters.apply(library, "", null, 0).size());
        assertEquals(1, PhotoFilters.apply(library, "", null, 2).size());
        assertEquals(3, library.size());
    }
    @Test public void emptyLibraryAndNoMatchesAreHandled() {
        assertTrue(PhotoFilters.apply(Collections.emptyList(), "", null, 0).isEmpty());
        assertTrue(PhotoFilters.apply(library, "missing", null, 0).isEmpty());
    }
}
