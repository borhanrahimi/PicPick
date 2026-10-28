package edu.utsa.cs3443.picpick;

import android.app.Application;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import edu.utsa.cs3443.picpick.model.Photo;
import edu.utsa.cs3443.picpick.model.PhotoManager;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;

/** Retains import work through rotation; copies only the images selected by the user. */
public class PhotoImportModel extends AndroidViewModel {
    private static final java.util.concurrent.ExecutorService WORKER = Executors.newSingleThreadExecutor();
    public final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    public final MutableLiveData<int[]> result = new MutableLiveData<>();
    public PhotoImportModel(@NonNull Application app) { super(app); }

    public void importPhotos(List<Uri> uris) {
        if (Boolean.TRUE.equals(busy.getValue()) || uris.isEmpty()) return;
        busy.setValue(true);
        Application app = getApplication();
        WORKER.execute(() -> {
            List<Photo> added = new ArrayList<>();
            int failed = 0, duplicates = 0;
            File dir = new File(app.getFilesDir(), "imports");
            for (Uri uri : uris.subList(0, Math.min(30, uris.size()))) {
                File temporary = null;
                try {
                    if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Cannot create import folder");
                    temporary = File.createTempFile("import-", ".tmp", dir);
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    try (InputStream input = app.getContentResolver().openInputStream(uri);
                         OutputStream output = new FileOutputStream(temporary)) {
                        if (input == null) throw new IOException("No image data");
                        byte[] bytes = new byte[16384];
                        int read; long size = 0;
                        while ((read = input.read(bytes)) != -1) {
                            size += read;
                            if (size > 25L * 1024 * 1024) throw new IOException("Image exceeds 25 MB");
                            digest.update(bytes, 0, read); output.write(bytes, 0, read);
                        }
                    }
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(temporary.getPath(), bounds);
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || (long) bounds.outWidth * bounds.outHeight > 100_000_000L) {
                        throw new IOException("Unsupported image");
                    }
                    StringBuilder hash = new StringBuilder();
                    for (byte value : digest.digest()) hash.append(String.format(Locale.ROOT, "%02x", value & 255));
                    File destination = new File(dir, hash.toString());
                    if (destination.isFile()) { duplicates++; continue; }
                    if (!temporary.renameTo(destination)) throw new IOException("Cannot store image");
                    String label = "Imported photo";
                    try (Cursor cursor = app.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) label = cursor.getString(0);
                    } catch (RuntimeException ignored) { }
                    if (label.length() > 200) label = label.substring(0, 200);
                    added.add(new Photo(0, "imports/" + destination.getName(), label, Photo.Status.UNASSIGNED));
                } catch (Exception e) { failed++; }
                finally { if (temporary != null && temporary.exists()) temporary.delete(); }
            }
            int errors = failed + Math.max(0, uris.size() - 30), repeated = duplicates;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                PhotoManager.addImported(app, added);
                busy.setValue(false);
                result.setValue(new int[]{added.size(), repeated, errors});
            });
        });
    }
}
