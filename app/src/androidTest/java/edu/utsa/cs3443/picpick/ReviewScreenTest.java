package edu.utsa.cs3443.picpick;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import org.junit.*;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReviewScreenTest {
    private Context context;
    @Before public void prepare() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("onboarding_complete", true).apply();
            PhotoManager.loadPhotos(context);
            PhotoManager.setSamples(context, true);
            PhotoManager.resetAll(context);
        });
    }
    @After public void cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> PhotoManager.resetAll(context));
    }
    @Test public void twoChoicesCanBeUndoneAfterRotation() {
        try (ActivityScenario<HomeActivity> screen = ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.btnKeep)).perform(click());
            onView(withId(R.id.btnTrash)).perform(click());
            screen.recreate();
            onView(withId(R.id.btnUndo)).perform(click());
            onView(withId(R.id.btnUndo)).perform(click());
            onView(withId(R.id.btnUndo)).check(matches(not(isEnabled())));
            screen.onActivity(activity -> assertEquals(PhotoManager.getAllPhotos().size(), PhotoManager.getUnassignedPhotos().size()));
        }
    }
    @Test public void completionDisablesChoicesAndUndoRestoresReview() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Photo p : PhotoManager.getAllPhotos()) PhotoManager.setPhotoStatus(p, Photo.Status.KEEP, context);
        });
        try (ActivityScenario<HomeActivity> screen = ActivityScenario.launch(HomeActivity.class)) {
            onView(withId(R.id.reviewEmpty)).check(matches(withText(R.string.review_complete)));
            onView(withId(R.id.btnKeep)).check(matches(not(isEnabled())));
            onView(withId(R.id.btnUndo)).perform(click());
            onView(withId(R.id.btnKeep)).check(matches(isEnabled()));
        }
    }
    @Test public void resetRequiresConfirmationAndCancelPreservesChoices() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                PhotoManager.setPhotoStatus(PhotoManager.getAllPhotos().get(0), Photo.Status.KEEP, context));
        try (ActivityScenario<SettingsActivity> screen = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.btnReset)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click());
            onView(withText(android.R.string.cancel)).perform(click());
            screen.onActivity(activity -> assertEquals(1, PhotoManager.getPhotosByStatus(Photo.Status.KEEP).size()));
            onView(withId(R.id.btnReset)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            screen.onActivity(activity -> assertTrue(PhotoManager.getPhotosByStatus(Photo.Status.KEEP).isEmpty()));
        }
    }
}
