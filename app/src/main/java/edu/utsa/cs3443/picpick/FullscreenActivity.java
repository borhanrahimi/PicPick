package edu.utsa.cs3443.picpick;

import android.os.Bundle;
import android.widget.ImageView;

public class FullscreenActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_fullscreen);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        String path = getIntent().getStringExtra("imagePath");
        if (path != null) PhotoImages.load((ImageView) findViewById(R.id.fullImageView), path, 2048);
    }
}
