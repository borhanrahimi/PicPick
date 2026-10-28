package edu.utsa.cs3443.picpick;

import android.os.Bundle;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import edu.utsa.cs3443.picpick.model.PhotoManager;

public class SettingsActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_settings);
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
        findViewById(R.id.btnExport).setOnClickListener(v -> shareCsv());
        findViewById(R.id.btnReset).setOnClickListener(v -> confirmReset());
    }
}
