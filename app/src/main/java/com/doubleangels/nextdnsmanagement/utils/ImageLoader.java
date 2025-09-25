package com.doubleangels.nextdnsmanagement.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.lang.ref.WeakReference;

/**
 * Optimized image loading utility with memory management and caching.
 * Provides efficient image loading with proper memory cleanup and recycling.
 */
public class ImageLoader {

    private static final int MAX_IMAGE_SIZE = 1024; // Maximum image size in pixels
    private static final int CACHE_SIZE = 50; // Maximum number of cached images

    /**
     * Loads a drawable resource into an ImageView with memory optimization.
     * Uses weak references to prevent memory leaks.
     *
     * @param context       The context for resource access.
     * @param imageView     The ImageView to load the image into.
     * @param drawableResId The drawable resource ID to load.
     */
    public static void loadDrawable(@NonNull Context context, @NonNull ImageView imageView, int drawableResId) {
        try {
            // Use weak reference to prevent memory leaks
            WeakReference<ImageView> imageViewRef = new WeakReference<>(imageView);
            WeakReference<Context> contextRef = new WeakReference<>(context);

            // Load drawable asynchronously to avoid blocking UI thread
            new AsyncTask<Void, Void, Drawable>() {
                @Override
                protected Drawable doInBackground(Void... voids) {
                    Context ctx = contextRef.get();
                    if (ctx == null)
                        return null;

                    try {
                        return ContextCompat.getDrawable(ctx, drawableResId);
                    } catch (Exception e) {
                        SentryManager.captureStaticException(e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Drawable drawable) {
                    ImageView iv = imageViewRef.get();
                    if (iv != null && drawable != null) {
                        iv.setImageDrawable(drawable);
                    }
                }
            }.execute();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Loads a bitmap with memory optimization and size constraints.
     * Automatically scales large images to prevent OutOfMemoryError.
     *
     * @param context     The context for resource access.
     * @param imageView   The ImageView to load the bitmap into.
     * @param bitmapResId The bitmap resource ID to load.
     */
    public static void loadBitmap(@NonNull Context context, @NonNull ImageView imageView, int bitmapResId) {
        try {
            WeakReference<ImageView> imageViewRef = new WeakReference<>(imageView);
            WeakReference<Context> contextRef = new WeakReference<>(context);

            new AsyncTask<Void, Void, Bitmap>() {
                @Override
                protected Bitmap doInBackground(Void... voids) {
                    Context ctx = contextRef.get();
                    if (ctx == null)
                        return null;

                    try {
                        // Load bitmap with size optimization
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeResource(ctx.getResources(), bitmapResId, options);

                        // Calculate sample size to prevent memory issues
                        options.inSampleSize = calculateInSampleSize(options, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE);
                        options.inJustDecodeBounds = false;
                        options.inPreferredConfig = Bitmap.Config.RGB_565;
                        options.inPurgeable = true;
                        options.inInputShareable = true;

                        return BitmapFactory.decodeResource(ctx.getResources(), bitmapResId, options);
                    } catch (Exception e) {
                        SentryManager.captureStaticException(e);
                        return null;
                    }
                }

                @Override
                protected void onPostExecute(Bitmap bitmap) {
                    ImageView iv = imageViewRef.get();
                    if (iv != null && bitmap != null) {
                        iv.setImageBitmap(bitmap);
                    }
                }
            }.execute();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Calculates the appropriate sample size for bitmap loading to prevent memory
     * issues.
     *
     * @param options   The BitmapFactory.Options containing image dimensions.
     * @param reqWidth  The requested width.
     * @param reqHeight The requested height.
     * @return The calculated sample size.
     */
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Clears image from ImageView and releases memory.
     * Should be called when views are no longer needed.
     *
     * @param imageView The ImageView to clear.
     */
    public static void clearImage(@NonNull ImageView imageView) {
        try {
            imageView.setImageDrawable(null);
            imageView.setImageBitmap(null);
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }
}
