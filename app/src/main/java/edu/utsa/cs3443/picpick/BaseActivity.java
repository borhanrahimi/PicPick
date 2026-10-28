package edu.utsa.cs3443.picpick;

import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import java.io.*;

public abstract class BaseActivity extends AppCompatActivity {
    private PhotoImportModel importer;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(30), uris -> importer.importPhotos(uris));

    @Override protected void onCreate(Bundle state) {
        int mode = getSharedPreferences("settings", MODE_PRIVATE).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (AppCompatDelegate.getDefaultNightMode() != mode) AppCompatDelegate.setDefaultNightMode(mode);
        super.onCreate(state);
        PhotoManager.loadPhotos(this);
        importer = new ViewModelProvider(this).get(PhotoImportModel.class);
        importer.result.observe(this, result -> {
            if (result == null) return;
            importer.result.setValue(null);
            Toast.makeText(this, getString(R.string.import_result, result[0], result[1], result[2]), Toast.LENGTH_LONG).show();
            onLibraryChanged();
        });
    }
    @Override public void setContentView(int layout) {
        super.setContentView(layout);
        View root = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
        boolean light = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) != android.content.res.Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), root);
        controller.setAppearanceLightStatusBars(light);
        controller.setAppearanceLightNavigationBars(light);
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        if (nav != null) {
            int selected = this instanceof SettingsActivity ? R.id.nav_settings : this instanceof HomeActivity ? R.id.nav_review
                    : this instanceof GalleryActivity ? R.id.nav_gallery : R.id.nav_folders;
            nav.setSelectedItemId(selected);
            nav.setOnItemSelectedListener(item -> {
                boolean inFolderDetail = this instanceof PhotoGridActivity && !(this instanceof GalleryActivity);
                if (item.getItemId() == selected && !inFolderDetail) return true;
                Class<?> screen = item.getItemId() == R.id.nav_review ? HomeActivity.class
                        : item.getItemId() == R.id.nav_gallery ? GalleryActivity.class
                        : item.getItemId() == R.id.nav_settings ? SettingsActivity.class : FolderActivity.class;
                startActivity(new Intent(this, screen).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                return false;
            });
        }
        View add = findViewById(R.id.btnImport);
        if (add != null) {
            add.setOnClickListener(v -> pickPhotos());
            importer.busy.observe(this, busy -> {
                boolean working = Boolean.TRUE.equals(busy);
                add.setEnabled(!working);
                if (add instanceof android.widget.TextView) ((android.widget.TextView) add).setText(working ? R.string.importing : R.string.import_photos);
            });
        }
    }
    public void pickPhotos() {
        if (Boolean.TRUE.equals(importer.busy.getValue())) return;
        try {
            picker.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        } catch (ActivityNotFoundException e) { Toast.makeText(this, R.string.no_picker, Toast.LENGTH_LONG).show(); }
    }
    protected void onLibraryChanged() { }
    protected void confirmReset() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reset_title).setMessage(R.string.reset_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.reset_confirm, (dialog, which) -> {
                    PhotoManager.resetAll(this); onLibraryChanged();
                    Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show();
                }).show();
    }
    protected void shareCsv() {
        try {
            File dir = new File(getCacheDir(), "exports");
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException();
            File file = new File(dir, "picpick-" + System.currentTimeMillis() + ".csv");
            PhotoManager.writeCsv(this, file);
            android.net.Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
            Intent share = new Intent(Intent.ACTION_SEND).setType("text/csv")
                    .putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setClipData(ClipData.newRawUri("PicPick CSV", uri));
            startActivity(Intent.createChooser(share, getString(R.string.share_csv)));
        } catch (IOException | ActivityNotFoundException e) {
            Toast.makeText(this, R.string.export_error, Toast.LENGTH_LONG).show();
        }
    }
}
