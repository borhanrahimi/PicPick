package edu.utsa.cs3443.picpick;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import java.util.List;

public abstract class PhotoGridActivity extends BaseActivity {
    protected abstract Photo.Status category();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_gallery);
        int title = category() == Photo.Status.KEEP ? R.string.keep_action : category() == Photo.Status.TRASH ? R.string.trash_action
                : category() == Photo.Status.SKIP ? R.string.skip_action : R.string.gallery_title;
        ((TextView) findViewById(R.id.galleryTitle)).setText(title);
        ((GridView) findViewById(R.id.gridGallery)).setEmptyView(findViewById(R.id.gridEmpty));
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onLibraryChanged() { render(); }
    private void render() {
        List<Photo> photos = PhotoManager.getPhotosByStatus(category());
        ((TextView) findViewById(R.id.photoCount)).setText(getString(R.string.photo_count, photos.size()));
        GridView grid = findViewById(R.id.gridGallery);
        grid.setAdapter(new ImageAdapter(this, photos));
        grid.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, category() == Photo.Status.UNASSIGNED ? HomeActivity.class : FullscreenActivity.class);
            intent.putExtra("imagePath", photos.get(position).getFilePath());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }
}
