package edu.utsa.cs3443.picpick;

import android.content.Context;
import android.view.*;
import android.widget.*;
import edu.utsa.cs3443.picpick.model.Photo;
import java.util.List;

public class ImageAdapter extends BaseAdapter {
    private final Context context;
    private final List<Photo> photos;
    public ImageAdapter(Context context, List<Photo> photos) { this.context = context; this.photos = photos; }
    @Override public int getCount() { return photos.size(); }
    @Override public Photo getItem(int position) { return photos.get(position); }
    @Override public long getItemId(int position) { return photos.get(position).getId(); }
    @Override public View getView(int position, View recycled, ViewGroup parent) {
        ImageView image = recycled instanceof ImageView ? (ImageView) recycled : new ImageView(context);
        int width = parent instanceof GridView ? ((GridView) parent).getColumnWidth() : 0;
        int size = width > 0 ? width : Math.round(112 * context.getResources().getDisplayMetrics().density);
        image.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setBackgroundResource(R.drawable.bg_photo_card);
        image.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 16 * context.getResources().getDisplayMetrics().density);
            }
        });
        image.setClipToOutline(true);
        Photo photo = getItem(position);
        image.setContentDescription(photo.getLabel());
        PhotoImages.load(image, photo.getFilePath(), 512);
        return image;
    }
}
