package edu.utsa.cs3443.picpick.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PhotoFilters {
    private PhotoFilters() { }
    /** Source: 0 = all, 1 = imported, 2 = bundled samples. */
    public static List<Photo> apply(List<Photo> library, String query, Photo.Status status, int source) {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<Photo> result = new ArrayList<>();
        for (Photo photo : library) {
            boolean imported = photo.getFilePath().startsWith("imports/");
            if (status != null && photo.getStatus() != status) continue;
            if (source == 1 && !imported || source == 2 && imported) continue;
            if (!photo.getLabel().toLowerCase(Locale.ROOT).contains(needle)) continue;
            result.add(photo);
        }
        return result;
    }
}
