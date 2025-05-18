package com.example.purrytify.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.purrytify.R
import java.io.File

object ImageUtils {
    private const val TAG = "ImageUtils"

    fun loadImage(context: Context, uri: String, imageView: ImageView?): Int {
        val defaultResourceId = R.drawable.logo

        if (imageView == null) return defaultResourceId

        try {
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

            if (uri.startsWith("file://")) {
                val filePath = uri.substring(7) // Remove "file://"
                val file = File(filePath)

                if (file.exists() && imageView != null) {
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    imageView.post { imageView.setImageBitmap(bitmap) }
                }
                return defaultResourceId
            }

            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                Glide.with(context)
                    .load(uri)
                    .placeholder(defaultResourceId)
                    .error(defaultResourceId)
                    .into(imageView)
                return defaultResourceId
            }

            val resourceName = uri.substringAfterLast("/")
            val resId = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )

            if (resId != 0) {
                imageView.setImageResource(resId)
                return resId
            } else {
                imageView.setImageResource(defaultResourceId)
                return defaultResourceId
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading image from uri: $uri", e)
            imageView.setImageResource(defaultResourceId)
            return defaultResourceId
        }
    }
}