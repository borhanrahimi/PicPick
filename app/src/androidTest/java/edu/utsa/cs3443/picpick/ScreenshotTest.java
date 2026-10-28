package edu.utsa.cs3443.picpick;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import edu.utsa.cs3443.picpick.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Captures actual emulator screens for README; use a disposable test installation. */
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    @Test public void captureProductScreens() throws Exception {
        Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_NO).putBoolean("onboarding_complete", true).apply();
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            PhotoManager.loadPhotos(c); PhotoManager.setSamples(c, true); PhotoManager.resetAll(c);
        });
        try {
            try (ActivityScenario<HomeActivity> screen = ActivityScenario.launch(HomeActivity.class)) {
                FeatureScreenTest.waitForImage(screen, R.id.imageViewMain);
                onView(withId(R.id.btnKeep)).check(matches(isDisplayed())); capture(c, "review");
            }
            try (ActivityScenario<GalleryActivity> screen = ActivityScenario.launch(GalleryActivity.class)) {
                onView(withId(R.id.photoSearch)).check(matches(isDisplayed()));
                SystemClock.sleep(500); capture(c, "gallery");
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(0), Photo.Status.KEEP, c);
                PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(1), Photo.Status.KEEP, c);
                PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(2), Photo.Status.TRASH, c);
                PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(3), Photo.Status.SKIP, c);
            });
            try (ActivityScenario<FolderActivity> screen = ActivityScenario.launch(FolderActivity.class)) {
                onView(withId(R.id.librarySummary)).check(matches(isDisplayed())); capture(c, "folders");
            }
            try (ActivityScenario<SettingsActivity> screen = ActivityScenario.launch(SettingsActivity.class)) {
                onView(withId(R.id.themeGroup)).check(matches(isDisplayed())); capture(c, "settings");
                onView(withText(R.string.about_app)).perform(scrollTo()); capture(c, "credits");
            }
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_YES).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            });
            try (ActivityScenario<HomeActivity> screen = ActivityScenario.launch(HomeActivity.class)) {
                FeatureScreenTest.waitForImage(screen, R.id.imageViewMain); capture(c, "review-dark");
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                PhotoManager.resetAll(c);
                c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM).apply();
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            });
        }
    }
    private void capture(Context context, String name) throws IOException {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(200);
        // Shell-owned output survives Gradle uninstalling the disposable test APK.
        shell("mkdir -p /data/local/tmp/picpick-screenshots");
        shell("screencap -p /data/local/tmp/picpick-screenshots/" + name + ".png");
        String size = shell("wc -c /data/local/tmp/picpick-screenshots/" + name + ".png").trim().split("\\s+")[0];
        assertTrue(Long.parseLong(size) > 0);
    }
    private String shell(String command) throws IOException {
        android.os.ParcelFileDescriptor descriptor = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        try (InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[1024]; int count;
            while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
            return output.toString("UTF-8");
        }
    }
}
