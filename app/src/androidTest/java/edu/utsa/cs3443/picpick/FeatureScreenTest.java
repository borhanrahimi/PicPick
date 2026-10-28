package edu.utsa.cs3443.picpick;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import edu.utsa.cs3443.picpick.model.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FeatureScreenTest {
    private Context context;
    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply();
            PhotoManager.loadPhotos(context); PhotoManager.setSamples(context, true); PhotoManager.resetAll(context);
        });
    }
    @After public void cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> PhotoManager.resetAll(context));
    }
    @Test public void swipesClassifyAndButtonsRemainVisibleAfterImagesLoad() {
        try (ActivityScenario<HomeActivity> screen = ActivityScenario.launch(HomeActivity.class)) {
            waitForImage(screen, R.id.imageViewMain);
            onView(withId(R.id.imageViewMain)).perform(swipeRight());
            waitForImage(screen, R.id.imageViewMain);
            onView(withId(R.id.imageViewMain)).perform(swipeLeft());
            screen.onActivity(activity -> {
                assertEquals(1, PhotoManager.getPhotosByStatus(Photo.Status.KEEP).size());
                assertEquals(1, PhotoManager.getPhotosByStatus(Photo.Status.TRASH).size());
            });
            onView(withId(R.id.btnUndo)).perform(click());
            onView(withId(R.id.btnKeep)).check(matches(isDisplayed()));
        }
    }
    @Test public void gallerySearchAndStatusFilterSurviveRecreation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(0), Photo.Status.KEEP, context));
        try (ActivityScenario<GalleryActivity> screen = ActivityScenario.launch(GalleryActivity.class)) {
            onView(withId(R.id.filterKeep)).perform(click());
            onView(withId(R.id.photoSearch)).perform(replaceText("Sample 1"), closeSoftKeyboard());
            onView(withId(R.id.photoCount)).check(matches(withText(context.getString(R.string.photo_count, 1))));
            screen.recreate();
            onView(withId(R.id.photoSearch)).check(matches(withText("Sample 1")));
            onView(withId(R.id.photoCount)).check(matches(withText(context.getString(R.string.photo_count, 1))));
        }
    }
    @Test public void fullPhotoDoubleTapZoomsAndResetRestoresFit() {
        Intent intent = new Intent(context, FullscreenActivity.class).putExtra("imagePath", PhotoManager.getAllPhotos().get(0).getFilePath());
        try (ActivityScenario<FullscreenActivity> screen = ActivityScenario.launch(intent)) {
            waitForImage(screen, R.id.fullImageView);
            float[] fitted = new float[9];
            screen.onActivity(activity -> ((ZoomImageView) activity.findViewById(R.id.fullImageView)).getImageMatrix().getValues(fitted));
            onView(withId(R.id.fullImageView)).perform(doubleClick());
            screen.onActivity(activity -> {
                float[] zoomed = new float[9]; ((ZoomImageView) activity.findViewById(R.id.fullImageView)).getImageMatrix().getValues(zoomed);
                assertTrue(zoomed[Matrix.MSCALE_X] > fitted[Matrix.MSCALE_X] * 2);
            });
            onView(withId(R.id.btnResetZoom)).perform(click());
            screen.onActivity(activity -> {
                float[] reset = new float[9]; ((ZoomImageView) activity.findViewById(R.id.fullImageView)).getImageMatrix().getValues(reset);
                assertEquals(fitted[Matrix.MSCALE_X], reset[Matrix.MSCALE_X], .001);
            });
        }
    }
    @Test public void settingsCreditsHaveAllFourLinksAndGuideCanBeReplayed() {
        try (ActivityScenario<SettingsActivity> screen = ActivityScenario.launch(SettingsActivity.class)) {
            screen.onActivity(activity -> {
                for (int id : new int[]{R.id.credit_borhan, R.id.credit_nadibur, R.id.credit_simon, R.id.credit_levi}) {
                    assertEquals(1, ((TextView) activity.findViewById(id)).getUrls().length);
                }
            });
            onView(withId(R.id.btnWalkthrough)).perform(scrollTo(), click());
            onView(withText(R.string.welcome_title)).check(matches(isDisplayed()));
            onView(withText(android.R.string.ok)).perform(click());
        }
    }
    @Test public void removingAnImportedCopyRequiresConfirmationAndPreservesSource() throws Exception {
        String path = "imports/" + String.join("", Collections.nCopies(64, "b"));
        File copy = new File(context.getFilesDir(), path);
        File source = new File(context.getCacheDir(), "original-photo-test");
        copy.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(copy)) { out.write(new byte[]{1,2,3}); }
        try (OutputStream out = new FileOutputStream(source)) { out.write(new byte[]{1,2,3}); }
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> PhotoManager.addImported(context,
                Collections.singletonList(new Photo(0, path, "Removal test", Photo.Status.UNASSIGNED))));
        Intent intent = new Intent(context, FullscreenActivity.class).putExtra("imagePath", path);
        try (ActivityScenario<FullscreenActivity> screen = ActivityScenario.launch(intent)) {
            onView(withId(R.id.btnRemoveCopy)).perform(click());
            onView(withText(android.R.string.cancel)).perform(click()); assertTrue(copy.exists());
            onView(withId(R.id.btnRemoveCopy)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            assertFalse(copy.exists()); assertTrue(source.exists());
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> PhotoManager.removeImported(context, path)); source.delete();
        }
    }
    static <T extends androidx.appcompat.app.AppCompatActivity> void waitForImage(ActivityScenario<T> screen, int id) {
        long deadline = SystemClock.uptimeMillis() + 10000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < deadline) {
            screen.onActivity(activity -> ready.set(((ImageView) activity.findViewById(id)).getDrawable() instanceof android.graphics.drawable.BitmapDrawable));
            if (ready.get()) return;
            SystemClock.sleep(50);
        }
        fail("Photo did not load");
    }
}
