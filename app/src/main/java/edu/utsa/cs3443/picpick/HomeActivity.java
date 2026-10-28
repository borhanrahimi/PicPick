package edu.utsa.cs3443.picpick;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import java.util.List;

public class HomeActivity extends BaseActivity {
    private String currentPath;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_home);
        currentPath = state == null ? getIntent().getStringExtra("imagePath") : state.getString("currentPath");
        findViewById(R.id.btnKeep).setOnClickListener(v -> choose(Photo.Status.KEEP));
        findViewById(R.id.btnTrash).setOnClickListener(v -> choose(Photo.Status.TRASH));
        findViewById(R.id.btnSkip).setOnClickListener(v -> choose(Photo.Status.SKIP));
        findViewById(R.id.btnUndo).setOnClickListener(v -> {
            currentPath = PhotoManager.undo(this); render();
        });
        findViewById(R.id.btnReviewSkipped).setOnClickListener(v -> {
            PhotoManager.moveSkippedToUnassigned(this); render();
        });
    }
    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intent.hasExtra("imagePath")) currentPath = intent.getStringExtra("imagePath");
        render();
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onLibraryChanged() { render(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putString("currentPath", currentPath);
    }
    private void choose(Photo.Status status) {
        Photo photo = PhotoManager.getPhotoByPath(currentPath);
        if (photo == null || photo.getStatus() != Photo.Status.UNASSIGNED) return;
        PhotoManager.setPhotoStatus(photo, status, this);
        currentPath = null; render();
    }
    private void render() {
        List<Photo> queue = PhotoManager.getUnassignedPhotos();
        int total = PhotoManager.getAllPhotos().size();
        int skipped = PhotoManager.getPhotosByStatus(Photo.Status.SKIP).size();
        int sorted = total - queue.size() - skipped;
        ProgressBar progress = findViewById(R.id.reviewProgressBar);
        progress.setMax(Math.max(1, total)); progress.setProgress(sorted);
        ((TextView) findViewById(R.id.reviewProgress)).setText(getString(R.string.review_progress, sorted, total, skipped));
        Photo current = PhotoManager.getPhotoByPath(currentPath);
        if (current == null || current.getStatus() != Photo.Status.UNASSIGNED) current = queue.isEmpty() ? null : queue.get(0);
        currentPath = current == null ? null : current.getFilePath();
        boolean hasPhoto = current != null;
        ImageView image = findViewById(R.id.imageViewMain);
        image.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        findViewById(R.id.reviewEmpty).setVisibility(hasPhoto ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.reviewEmpty)).setText(total == 0 ? R.string.no_photos : skipped > 0 ? R.string.review_paused : R.string.review_complete);
        ((TextView) findViewById(R.id.currentPhotoLabel)).setText(hasPhoto ? current.getLabel() : "");
        findViewById(R.id.btnReviewSkipped).setVisibility(!hasPhoto && skipped > 0 ? View.VISIBLE : View.GONE);
        for (int id : new int[]{R.id.btnKeep, R.id.btnTrash, R.id.btnSkip}) {
            findViewById(id).setEnabled(hasPhoto); findViewById(id).setAlpha(hasPhoto ? 1f : .35f);
        }
        boolean undo = PhotoManager.canUndo();
        findViewById(R.id.btnUndo).setEnabled(undo); findViewById(R.id.btnUndo).setAlpha(undo ? 1f : .35f);
        if (!hasPhoto) { image.setTag(null); image.setImageDrawable(null); return; }
        image.setContentDescription(getString(R.string.review_photo, current.getLabel()));
        PhotoImages.load(image, currentPath, 1600);
    }
}
