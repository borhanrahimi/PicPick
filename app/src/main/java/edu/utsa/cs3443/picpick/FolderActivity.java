package edu.utsa.cs3443.picpick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;

public class FolderActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_folder);
        findViewById(R.id.cardKeep).setOnClickListener(v -> startActivity(new Intent(this, KeepActivity.class)));
        findViewById(R.id.cardTrash).setOnClickListener(v -> startActivity(new Intent(this, TrashActivity.class)));
        findViewById(R.id.cardSkip).setOnClickListener(v -> startActivity(new Intent(this, SkipActivity.class)));
        findViewById(R.id.btnReset).setOnClickListener(v -> confirmReset());
        findViewById(R.id.btnExport).setOnClickListener(v -> shareCsv());
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onLibraryChanged() { render(); }
    private void render() {
        int keep = PhotoManager.getPhotosByStatus(Photo.Status.KEEP).size();
        int trash = PhotoManager.getPhotosByStatus(Photo.Status.TRASH).size();
        int skip = PhotoManager.getPhotosByStatus(Photo.Status.SKIP).size();
        int total = PhotoManager.getAllPhotos().size();
        ((TextView) findViewById(R.id.keepCount)).setText(String.valueOf(keep));
        ((TextView) findViewById(R.id.trashCount)).setText(String.valueOf(trash));
        ((TextView) findViewById(R.id.skipCount)).setText(String.valueOf(skip));
        ((TextView) findViewById(R.id.librarySummary)).setText(getString(R.string.library_summary, total, keep + trash, total - keep - trash));
    }
}
