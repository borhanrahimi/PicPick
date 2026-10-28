package edu.utsa.cs3443.picpick;

import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import edu.utsa.cs3443.picpick.model.PhotoManager;

public class SettingsActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_settings);
        for (int id : new int[]{R.id.credit_borhan, R.id.credit_nadibur, R.id.credit_simon, R.id.credit_levi}) {
            ((android.widget.TextView) findViewById(id)).setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        }
        RadioGroup themes = findViewById(R.id.themeGroup);
        int mode = getSharedPreferences("settings", MODE_PRIVATE).getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        themes.check(mode == AppCompatDelegate.MODE_NIGHT_YES ? R.id.themeDark : mode == AppCompatDelegate.MODE_NIGHT_NO ? R.id.themeLight : R.id.themeSystem);
        themes.setOnCheckedChangeListener((group, checked) -> {
            int theme = checked == R.id.themeDark ? AppCompatDelegate.MODE_NIGHT_YES
                    : checked == R.id.themeLight ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("theme", theme).apply();
            AppCompatDelegate.setDefaultNightMode(theme);
        });
        SwitchMaterial samples = findViewById(R.id.switchSamples);
        samples.setChecked(PhotoManager.showsSamples());
        samples.setOnCheckedChangeListener((button, show) -> PhotoManager.setSamples(this, show));
        findViewById(R.id.btnManageStorage).setOnClickListener(v -> startActivity(new android.content.Intent(this, GalleryActivity.class)
                .putExtra("importedOnly", true).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)));
        findViewById(R.id.btnWalkthrough).setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.welcome_title).setMessage(R.string.welcome_message).setPositiveButton(android.R.string.ok, null).show());
        findViewById(R.id.btnExport).setOnClickListener(v -> shareCsv());
        findViewById(R.id.btnReset).setOnClickListener(v -> confirmReset());
    }
    @Override protected void onResume() { super.onResume(); updateStorage(); }
    @Override protected void onLibraryChanged() { updateStorage(); }
    private void updateStorage() {
        ((android.widget.TextView) findViewById(R.id.storageSummary)).setText(getString(R.string.storage_summary,
                PhotoManager.importedCount(), android.text.format.Formatter.formatShortFileSize(this, PhotoManager.importedBytes(this))));
    }
}
