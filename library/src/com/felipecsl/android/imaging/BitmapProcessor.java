package com.felipecsl.android.imaging;

import java.io.InputStream;
import java.net.ResponseCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;

public class BitmapProcessor {

    private final Context context;

    public BitmapProcessor(final Context context) {
        this.context = context;
        ResponseCache.setDefault(new ImageResponseCache(context.getCacheDir()));
    }

    public BitmapDrawable getRoundedCorners(final BitmapDrawable drawable, final int radius) {
        final Bitmap bitmap = drawable.getBitmap();
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        final Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        canvas.drawBitmap(bitmap, rect, rect, paint);

        return new BitmapDrawable(context.getResources(), output);
    }

    // Took from:
    // http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
    private static int calculateInSampleSize(final BitmapFactory.Options options, final int reqWidth, final int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if ((height > reqHeight) || (width > reqWidth)) {

            // Calculate ratios of height and width to requested height and
            // width
            final int heightRatio = Math.round((float)height / (float)reqHeight);
            final int widthRatio = Math.round((float)width / (float)reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will
            // guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);
        }

        return inSampleSize;
    }

    public static Bitmap decodeStream(final InputStream stream) {
        try {
            return BitmapFactory.decodeStream(stream);
        } catch (final OutOfMemoryError e) {
            Log.e("BitmapProcessor", "Out of memory in decodeStream()");
            return null;
        }
    }

    public static Bitmap decodeStream(final InputStream stream, final BitmapFactory.Options options) {
        try {
            return BitmapFactory.decodeStream(stream, null, options);
        } catch (final OutOfMemoryError e) {
            Log.e("BitmapProcessor", "Out of memory in decodeStream()");
            return null;
        }
    }

    /**
     * Decodes a sampled Bitmap from the provided url in the requested width and height
     * 
     * @param urlString URL to download the bitmap from
     * @param reqWidth Requested width
     * @param reqHeight Requested height
     * @return Decoded bitmap
     */
    public Bitmap decodeSampledBitmapFromUrl(final String urlString, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPurgeable = true;
        options.inDither = false;
        options.inInputShareable = true;

        final BitmapConnection bitmapConnection = new BitmapConnection();
        bitmapConnection.readStream(urlString, new BitmapConnection.Runnable<Void>() {
            @Override
            public Void run(final InputStream stream) {
                decodeStream(stream, options);
                return null;
            }
        });

        if (reqWidth == 0) {
            reqWidth = options.outWidth;
        }
        if (reqHeight == 0) {
            reqHeight = options.outHeight;
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return bitmapConnection.readStream(urlString, new BitmapConnection.Runnable<Bitmap>() {
            @Override
            public Bitmap run(final InputStream stream) {
                return decodeStream(stream, options);
            }
        });
    }
}
