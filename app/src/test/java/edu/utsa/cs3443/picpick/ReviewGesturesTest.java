package edu.utsa.cs3443.picpick;
import edu.utsa.cs3443.picpick.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReviewGesturesTest {
    @Test public void leftTrashesAndRightKeeps() {
        assertEquals(Photo.Status.TRASH, ReviewGestures.classify(-120, 10, 88));
        assertEquals(Photo.Status.KEEP, ReviewGestures.classify(120, -10, 88));
    }
    @Test public void verticalAndDiagonalScrollingDoesNotClassify() {
        assertNull(ReviewGestures.classify(120, 200, 88));
        assertNull(ReviewGestures.classify(120, 100, 88));
    }
    @Test public void shortDragsAndTapsDoNotClassify() {
        assertNull(ReviewGestures.classify(20, 0, 88));
        assertNull(ReviewGestures.classify(0, 0, 88));
    }
}
