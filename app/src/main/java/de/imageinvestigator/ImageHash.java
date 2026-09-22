package de.imageinvestigator;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import java.io.InputStream;

/** Perceptual hash for near-duplicate images. It does not identify people. */
final class ImageHash {
    private ImageHash() { }

    static long compute(ContentResolver resolver, Uri uri) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(input, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IllegalArgumentException("Invalid image");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = Math.max(1, Math.max(bounds.outWidth, bounds.outHeight) / 512);
        Bitmap bitmap;
        try (InputStream input = resolver.openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(input, null, options);
        }
        if (bitmap == null) throw new IllegalArgumentException("Cannot decode image");
        Bitmap small = Bitmap.createBitmap(9, 8, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(small);
        canvas.drawBitmap(bitmap, null, new android.graphics.Rect(0, 0, 9, 8), null);
        long hash = 0;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) {
            int a = small.getPixel(x, y), b = small.getPixel(x + 1, y);
            int left = ((a >> 16) & 255) * 299 + ((a >> 8) & 255) * 587 + (a & 255) * 114;
            int right = ((b >> 16) & 255) * 299 + ((b >> 8) & 255) * 587 + (b & 255) * 114;
            hash = (hash << 1) | (left > right ? 1 : 0);
        }
        small.recycle(); bitmap.recycle();
        return hash;
    }
}
