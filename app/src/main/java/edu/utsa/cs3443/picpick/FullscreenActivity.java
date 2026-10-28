package edu.utsa.cs3443.picpick;

import android.os.Bundle;
import android.view.View;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;

public class FullscreenActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_fullscreen);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ZoomImageView image = findViewById(R.id.fullImageView);
        findViewById(R.id.btnResetZoom).setOnClickListener(v -> image.resetZoom());
        String path = getIntent().getStringExtra("imagePath");
        Photo photo = PhotoManager.getPhotoByPath(path);
        if (photo == null) { finish(); return; }
        image.setContentDescription(photo.getLabel());
        PhotoImages.load(image, path, 2048);
        boolean imported = path.startsWith("imports/");
        findViewById(R.id.btnRemoveCopy).setVisibility(imported ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnRemoveCopy).setOnClickListener(v -> confirmRemove(path, this::finish));
    }
}
