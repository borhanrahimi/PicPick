package edu.utsa.cs3443.picpick.model;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** App-private library. Call mutations on the main thread after any file import completes. */
public final class PhotoManager {
    private static final Map<String, Photo> photos = new LinkedHashMap<>();
    private static final List<Change> history = new ArrayList<>();
    private static boolean loaded;
    private static boolean samples = true;
    private static class Change {
        final String path;
        final Photo.Status before, after;
        Change(String path, Photo.Status before, Photo.Status after) {
            this.path = path; this.before = before; this.after = after;
        }
    }
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("photo_statuses", Context.MODE_PRIVATE);
    }
    public static void loadPhotos(Context c) {
        if (loaded) return;
        photos.clear(); history.clear();
        samples = c.getSharedPreferences("settings", Context.MODE_PRIVATE).getBoolean("samples", true);
        try {
            String[] files = c.getAssets().list("");
            if (files != null) for (String file : files) {
                if (file.matches("(?i).+\\.(png|jpg|jpeg|webp)")) {
                    photos.put(file, new Photo(photos.size(), file, "Sample " + (photos.size() + 1), Photo.Status.UNASSIGNED));
                }
            }
            JSONArray imported = new JSONArray(prefs(c).getString("imports", "[]"));
            for (int i = 0; i < imported.length(); i++) {
                JSONObject item = imported.getJSONObject(i);
                String path = item.getString("path");
                if (path.matches("imports/[a-f0-9-]+") && new File(c.getFilesDir(), path).isFile()) {
                    photos.put(path, new Photo(photos.size(), path, item.getString("label"), Photo.Status.UNASSIGNED));
                }
            }
        } catch (IOException | JSONException ignored) { }
        File[] recovered = new File(c.getFilesDir(), "imports").listFiles();
        if (recovered != null) for (File file : recovered) {
            String path = "imports/" + file.getName();
            if (file.isFile() && file.getName().matches("[a-f0-9]{64}") && !photos.containsKey(path)) {
                photos.put(path, new Photo(photos.size(), path, "Imported photo", Photo.Status.UNASSIGNED));
            }
        }
        loadSavedStatuses(c);
        try {
            JSONArray saved = new JSONArray(prefs(c).getString("history", "[]"));
            for (int i = Math.max(0, saved.length() - 100); i < saved.length(); i++) {
                JSONObject entry = saved.getJSONObject(i);
                if (photos.containsKey(entry.getString("path"))) history.add(new Change(entry.getString("path"),
                        Photo.Status.valueOf(entry.getString("before")), Photo.Status.valueOf(entry.getString("after"))));
            }
        } catch (JSONException | IllegalArgumentException ignored) { history.clear(); }
        loaded = true;
    }
    public static void forceReload(Context c) { loaded = false; loadPhotos(c); }
    private static boolean visible(Photo p) { return samples || p.getFilePath().startsWith("imports/"); }
    public static List<Photo> getAllPhotos() {
        List<Photo> result = new ArrayList<>();
        for (Photo p : photos.values()) if (visible(p)) result.add(p);
        return result;
    }
    public static List<Photo> getPhotosByStatus(Photo.Status status) {
        List<Photo> result = new ArrayList<>();
        for (Photo p : getAllPhotos()) if (p.getStatus() == status) result.add(p);
        return result;
    }
    public static List<Photo> getUnassignedPhotos() { return getPhotosByStatus(Photo.Status.UNASSIGNED); }
    public static Photo getPhotoByPath(String path) {
        Photo photo = photos.get(path);
        return photo != null && visible(photo) ? photo : null;
    }
    public static void addImported(Context c, List<Photo> imported) {
        loadPhotos(c);
        for (Photo p : imported) photos.put(p.getFilePath(), new Photo(photos.size(), p.getFilePath(), p.getLabel(), Photo.Status.UNASSIGNED));
        persist(c);
    }
    public static void setPhotoStatus(Photo p, Photo.Status status, Context c) {
        if (p == null || p.getStatus() == status) return;
        history.add(new Change(p.getFilePath(), p.getStatus(), status));
        if (history.size() > 100) history.remove(0);
        p.setStatus(status);
        persist(c);
    }
    public static boolean canUndo() {
        while (!history.isEmpty()) {
            Change last = history.get(history.size() - 1);
            Photo photo = getPhotoByPath(last.path);
            if (photo != null && photo.getStatus() == last.after) return true;
            history.remove(history.size() - 1);
        }
        return false;
    }
    public static String undo(Context c) {
        if (!canUndo()) return null;
        Change last = history.remove(history.size() - 1);
        photos.get(last.path).setStatus(last.before);
        persist(c);
        return last.path;
    }
    public static void resetAll(Context c) {
        for (Photo p : getAllPhotos()) p.setStatus(Photo.Status.UNASSIGNED);
        history.clear(); persist(c);
    }
    public static void moveSkippedToUnassigned(Context c) {
        for (Photo p : getPhotosByStatus(Photo.Status.SKIP)) p.setStatus(Photo.Status.UNASSIGNED);
        history.clear(); persist(c);
    }
    public static void setSamples(Context c, boolean show) {
        samples = show; history.clear();
        c.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("samples", show).apply();
        persist(c);
    }
    public static boolean showsSamples() { return samples; }
    public static void saveStatuses(Context c) {
        SharedPreferences.Editor editor = prefs(c).edit();
        for (Photo p : photos.values()) editor.putString(p.getFilePath(), p.getStatus().name());
        editor.apply();
    }
    public static void loadSavedStatuses(Context c) {
        for (Photo p : photos.values()) {
            try { p.setStatus(Photo.Status.valueOf(prefs(c).getString(p.getFilePath(), "UNASSIGNED"))); }
            catch (IllegalArgumentException ignored) { p.setStatus(Photo.Status.UNASSIGNED); }
        }
    }
    private static void persist(Context c) {
        saveStatuses(c);
        JSONArray imported = new JSONArray(), changes = new JSONArray();
        try {
            for (Photo p : photos.values()) if (p.getFilePath().startsWith("imports/")) {
                imported.put(new JSONObject().put("path", p.getFilePath()).put("label", p.getLabel()));
            }
            for (Change entry : history) changes.put(new JSONObject().put("path", entry.path)
                    .put("before", entry.before.name()).put("after", entry.after.name()));
        } catch (JSONException e) { throw new IllegalStateException(e); }
        prefs(c).edit().putString("imports", imported.toString()).putString("history", changes.toString()).apply();
        saveStatusesToCSV(c);
    }
    public static void saveStatusesToCSV(Context c) {
        try { writeCsv(c, new File(c.getFilesDir(), "photo_statuses.csv")); }
        catch (IOException e) { android.util.Log.w("PicPick", "Could not update CSV", e); }
    }
    public static void writeCsv(Context c, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("fileName,status\n");
            for (Photo p : getAllPhotos()) writer.write(Csv.field(p.getLabel()) + "," + p.getStatus().name() + "\n");
        }
    }
}
