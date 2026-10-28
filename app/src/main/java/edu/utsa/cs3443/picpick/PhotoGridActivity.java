package edu.utsa.cs3443.picpick;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.widget.*;
import com.google.android.material.chip.ChipGroup;
import edu.utsa.cs3443.picpick.model.*;
import java.util.List;

public abstract class PhotoGridActivity extends BaseActivity {
    private String query = "";
    private Photo.Status status;
    private int source;
    protected abstract Photo.Status category();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_gallery);
        if (state != null) {
            query = state.getString("query", ""); source = state.getInt("source", 0);
            String filter = state.getString("filter");
            if (filter != null) status = Photo.Status.valueOf(filter);
        } else if (getIntent().getBooleanExtra("importedOnly", false)) source = 1;
        int title = category() == Photo.Status.KEEP ? R.string.keep_action : category() == Photo.Status.TRASH ? R.string.trash_action
                : category() == Photo.Status.SKIP ? R.string.skip_action : R.string.gallery_title;
        ((TextView) findViewById(R.id.galleryTitle)).setText(title);
        ((GridView) findViewById(R.id.gridGallery)).setEmptyView(findViewById(R.id.gridEmpty));
        EditText search = findViewById(R.id.photoSearch);
        search.setText(query);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { query = s.toString(); render(); }
            public void afterTextChanged(Editable editable) { }
        });
        Spinner sources = findViewById(R.id.sourceFilter);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.photo_sources, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); sources.setAdapter(adapter);
        sources.setSelection(source);
        sources.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { source = position; render(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        ChipGroup filters = findViewById(R.id.statusFilters);
        filters.check(status == Photo.Status.KEEP ? R.id.filterKeep : status == Photo.Status.TRASH ? R.id.filterTrash
                : status == Photo.Status.SKIP ? R.id.filterSkip : status == Photo.Status.UNASSIGNED ? R.id.filterReview : R.id.filterAll);
        findViewById(R.id.statusFilterScroll).setVisibility(category() == Photo.Status.UNASSIGNED ? View.VISIBLE : View.GONE);
        filters.setOnCheckedStateChangeListener((group, checked) -> {
            int id = checked.isEmpty() ? R.id.filterAll : checked.get(0);
            status = id == R.id.filterKeep ? Photo.Status.KEEP : id == R.id.filterTrash ? Photo.Status.TRASH
                    : id == R.id.filterSkip ? Photo.Status.SKIP : id == R.id.filterReview ? Photo.Status.UNASSIGNED : null;
            render();
        });
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intent.getBooleanExtra("importedOnly", false)) {
            ((EditText) findViewById(R.id.photoSearch)).setText("");
            ((ChipGroup) findViewById(R.id.statusFilters)).check(R.id.filterAll);
            ((Spinner) findViewById(R.id.sourceFilter)).setSelection(1);
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("query", query); state.putInt("source", source);
        if (status != null) state.putString("filter", status.name());
    }
    @Override protected void onResume() { super.onResume(); render(); }
    @Override protected void onLibraryChanged() { render(); }
    private void render() {
        List<Photo> photos = PhotoFilters.apply(PhotoManager.getAllPhotos(), query, category() == Photo.Status.UNASSIGNED ? status : category(), source);
        ((TextView) findViewById(R.id.photoCount)).setText(getString(R.string.photo_count, photos.size()));
        GridView grid = findViewById(R.id.gridGallery); grid.setAdapter(new ImageAdapter(this, photos));
        grid.setOnItemClickListener((parent, view, position, id) -> {
            Photo photo = photos.get(position);
            Intent intent = new Intent(this, photo.getStatus() == Photo.Status.UNASSIGNED ? HomeActivity.class : FullscreenActivity.class);
            intent.putExtra("imagePath", photo.getFilePath());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); startActivity(intent);
        });
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            Photo photo = photos.get(position);
            if (!photo.getFilePath().startsWith("imports/")) {
                Toast.makeText(this, R.string.sample_cannot_remove, Toast.LENGTH_SHORT).show(); return true;
            }
            confirmRemove(photo.getFilePath(), this::render); return true;
        });
    }
}
