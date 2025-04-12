package com.example.purrytify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.example.purrytify.R
import java.io.File

/**
 * Helper class for handling image loading from different sources
 */
object ImageUtils {
    private const val TAG = "ImageUtils"

    /**
     * Enhanced version of getDrawableResourceFromUri that handles both resource images
     * and file-based images. For file-based images, it sets the bitmap directly on the ImageView.
     *
     * @param context The context
     * @param uri The URI string (can be drawable reference, custom:filename, or file:// path)
     * @param imageView The ImageView to set the image on for file-based images
     * @return Resource ID (for drawable resources) or default resource ID (for file-based images)
     */
    fun loadImage(context: Context, uri: String, imageView: ImageView?): Int {
        val defaultResourceId = R.drawable.logo

        try {
            // Case 1: Custom artwork reference (custom_artwork:timestamp)
            if (uri.startsWith("custom_artwork:")) {
                val timestamp = uri.substringAfter("custom_artwork:")
                val filename = "${timestamp}.jpg"
                val file = File(context.filesDir, filename)

                if (file.exists() && imageView != null) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    imageView.post { imageView.setImageBitmap(bitmap) }
                    return defaultResourceId // Return default, but the bitmap is already set
                }
                return defaultResourceId
            }

            // Case 2: File URI (file://....)
            if (uri.startsWith("file://")) {
                val filePath = uri.substring(7) // Remove "file://"
                val file = File(filePath)

                if (file.exists() && imageView != null) {
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    imageView.post { imageView.setImageBitmap(bitmap) }
                }
                return defaultResourceId
            }

            // Case 3: Drawable resource reference (drawable/image_name)
            val resourceName = uri.substringAfterLast("/")
            val resId = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )

            return if (resId != 0) resId else defaultResourceId

        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from uri: $uri", e)
            return defaultResourceId
        }
    }
}