package edu.utsa.cs3443.picpick;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

/** Uses app test data; resets classifications before and after each test. */
@RunWith(AndroidJUnit4.class)
public class PhotoManagerTest {
    private Context context;

    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        PhotoManager.loadPhotos(context);
        PhotoManager.setSamples(context, true);
        PhotoManager.resetAll(context);
    }

    @After public void cleanup() { PhotoManager.resetAll(context); }

    @Test public void sortingSurvivesReloadAndResetRestoresQueue() {
        Photo photo = PhotoManager.getUnassignedPhotos().get(0);
        String path = photo.getFilePath();
        int total = PhotoManager.getAllPhotos().size();
        PhotoManager.setPhotoStatus(photo, Photo.Status.KEEP, context);
        PhotoManager.forceReload(context);
        assertEquals(Photo.Status.KEEP, PhotoManager.getPhotoByPath(path).getStatus());
        assertEquals(total - 1, PhotoManager.getUnassignedPhotos().size());
        assertEquals(1, PhotoManager.getPhotosByStatus(Photo.Status.KEEP).size());
        PhotoManager.resetAll(context);
        assertEquals(total, PhotoManager.getUnassignedPhotos().size());
        assertTrue(PhotoManager.getPhotosByStatus(Photo.Status.KEEP).isEmpty());
    }

    @Test public void revisitingSkippedUpdatesPersistenceAndCsv() throws Exception {
        Photo photo = PhotoManager.getUnassignedPhotos().get(0);
        String path = photo.getFilePath();
        PhotoManager.setPhotoStatus(photo, Photo.Status.SKIP, context);
        PhotoManager.moveSkippedToUnassigned(context);
        String csv = new String(Files.readAllBytes(new File(context.getFilesDir(), "photo_statuses.csv").toPath()), StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"" + photo.getLabel() + "\",UNASSIGNED\n"));
        assertFalse(csv.contains("\"" + photo.getLabel() + "\",SKIP\n"));
        PhotoManager.forceReload(context);
        assertEquals(Photo.Status.UNASSIGNED, PhotoManager.getPhotoByPath(path).getStatus());
        assertTrue(PhotoManager.getPhotosByStatus(Photo.Status.SKIP).isEmpty());
    }
    @Test public void undoRestoresMultipleDecisionsAfterReload() {
        Photo first = PhotoManager.getUnassignedPhotos().get(0);
        Photo second = PhotoManager.getUnassignedPhotos().get(1);
        PhotoManager.setPhotoStatus(first, Photo.Status.KEEP, context);
        PhotoManager.setPhotoStatus(second, Photo.Status.TRASH, context);
        PhotoManager.forceReload(context);
        assertTrue(PhotoManager.canUndo());
        assertEquals(second.getFilePath(), PhotoManager.undo(context));
        assertEquals(Photo.Status.UNASSIGNED, PhotoManager.getPhotoByPath(second.getFilePath()).getStatus());
        assertEquals(first.getFilePath(), PhotoManager.undo(context));
        assertFalse(PhotoManager.canUndo());
    }

    @Test public void sampleTogglePreservesHiddenChoicesAndResetClearsUndo() {
        Photo photo = PhotoManager.getUnassignedPhotos().get(0);
        PhotoManager.setPhotoStatus(photo, Photo.Status.KEEP, context);
        PhotoManager.setSamples(context, false);
        assertNull(PhotoManager.getPhotoByPath(photo.getFilePath()));
        PhotoManager.setSamples(context, true);
        assertEquals(Photo.Status.KEEP, PhotoManager.getPhotoByPath(photo.getFilePath()).getStatus());
        PhotoManager.resetAll(context);
        assertFalse(PhotoManager.canUndo());
    }
}
