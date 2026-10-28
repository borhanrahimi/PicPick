package edu.utsa.cs3443.picpick.model;

public final class ReviewGestures {
    private ReviewGestures() { }
    public static Photo.Status classify(float dx, float dy, float threshold) {
        if (Math.abs(dx) < threshold || Math.abs(dx) < Math.abs(dy) * 1.5f) return null;
        return dx > 0 ? Photo.Status.KEEP : Photo.Status.TRASH;
    }
}
