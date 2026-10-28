package edu.utsa.cs3443.picpick;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PhotoImportTest {
    @Test public void copiesDeduplicatesAndPersistsSelectedImages() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File exports = new File(context.getCacheDir(), "exports");
        assertTrue(exports.isDirectory() || exports.mkdirs());
        File source = new File(exports, "import-test.png");
        Bitmap bitmap = Bitmap.createBitmap(3, 3, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor((int) System.nanoTime());
        try (OutputStream output = new FileOutputStream(source)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", source);
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<int[]> report = new AtomicReference<>();
        AtomicReference<String> importedPath = new AtomicReference<>();
        AtomicReference<PhotoImportModel> model = new AtomicReference<>();
        Observer<int[]> observer = result -> { if (result != null) { report.set(result); done.countDown(); } };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            PhotoManager.loadPhotos(context);
            PhotoImportModel importer = new PhotoImportModel((Application) context.getApplicationContext());
            model.set(importer); importer.result.observeForever(observer);
            importer.importPhotos(Arrays.asList(uri, uri));
        });
        try {
            assertTrue("Import timed out", done.await(30, TimeUnit.SECONDS));
            assertArrayEquals(new int[]{1, 1, 0}, report.get());
            // The app-private copy remains readable even after its source is removed.
            assertTrue(source.delete());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                PhotoManager.forceReload(context);
                Photo imported = null;
                for (Photo p : PhotoManager.getAllPhotos()) if (p.getLabel().equals("import-test.png")) imported = p;
                assertNotNull(imported);
                importedPath.set(imported.getFilePath());
                assertTrue(new File(context.getFilesDir(), imported.getFilePath()).isFile());
            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> model.get().result.removeObserver(observer));
            if (importedPath.get() != null) new File(context.getFilesDir(), importedPath.get()).delete();
            source.delete();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                PhotoManager.forceReload(context);
                PhotoManager.resetAll(context);
            });
        }
    }
}
