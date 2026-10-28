package edu.utsa.cs3443.picpick;

import android.content.Context;
import android.graphics.*;
import androidx.exifinterface.media.ExifInterface;
import android.os.*;
import android.widget.ImageView;
import android.util.LruCache;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

public final class PhotoImages {
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(16 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2);
    private static InputStream open(Context c, String path) throws IOException {
        return path.startsWith("imports/") ? new FileInputStream(new File(c.getFilesDir(), path)) : c.getAssets().open(path);
    }
    public static void load(ImageView image, String path, int size) {
        image.setTag(path); image.setImageDrawable(null);
        String key = path + ":" + size;
        Bitmap cached = CACHE.get(key);
        if (cached != null) { image.setImageBitmap(cached); return; }
        Context context = image.getContext().getApplicationContext();
        WeakReference<ImageView> target = new WeakReference<>(image);
        WORKERS.execute(() -> {
            Bitmap bitmap = CACHE.get(key);
            if (bitmap == null) {
                try {
                    BitmapFactory.Options bounds = new BitmapFactory.Options();
                    bounds.inJustDecodeBounds = true;
                    try (InputStream stream = open(context, path)) { BitmapFactory.decodeStream(stream, null, bounds); }
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 1;
                    while (bounds.outWidth / options.inSampleSize > size || bounds.outHeight / options.inSampleSize > size) options.inSampleSize *= 2;
                    try (InputStream stream = open(context, path)) { bitmap = BitmapFactory.decodeStream(stream, null, options); }
                    if (bitmap != null) {
                        int orientation = ExifInterface.ORIENTATION_NORMAL;
                        try (InputStream stream = open(context, path)) {
                            orientation = new ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        } catch (IOException ignored) { }
                        Matrix matrix = new Matrix();
                        switch (orientation) {
                            case 2: matrix.setScale(-1, 1); break;
                            case 3: matrix.setRotate(180); break;
                            case 4: matrix.setScale(1, -1); break;
                            case 5: matrix.setRotate(90); matrix.postScale(-1, 1); break;
                            case 6: matrix.setRotate(90); break;
                            case 7: matrix.setRotate(-90); matrix.postScale(-1, 1); break;
                            case 8: matrix.setRotate(-90); break;
                            default: break;
                        }
                        if (!matrix.isIdentity()) bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        CACHE.put(key, bitmap);
                    }
                } catch (IOException | RuntimeException ignored) { }
            }
            Bitmap result = bitmap;
            new Handler(Looper.getMainLooper()).post(() -> {
                ImageView view = target.get();
                if (view != null && path.equals(view.getTag())) {
                    if (result == null) view.setImageResource(R.drawable.ic_image_placeholder);
                    else view.setImageBitmap(result);
                }
            });
        });
    }
}
