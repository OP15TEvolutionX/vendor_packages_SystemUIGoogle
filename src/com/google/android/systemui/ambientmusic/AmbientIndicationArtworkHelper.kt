package com.google.android.systemui.ambientmusic

import android.app.WallpaperColors
import android.content.Context
import android.content.theming.ThemeStyle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.android.systemui.graphics.ImageLoader
import com.android.systemui.monet.ColorScheme
import com.android.systemui.util.getColorWithAlpha
import com.google.android.systemui.res.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

object AmbientIndicationArtworkHelper {

    private const val TAG = "AmbientArtwork"
    private const val ART_HTTP_CONNECT_MS = 5000
    private const val ART_HTTP_READ_MS = 5000

    @JvmField @Volatile var lastArtworkResult: ArtworkResult? = null

    data class ArtworkResult(
        @JvmField val albumArtUri: Uri,
        @JvmField val artwork: Drawable,
        @JvmField val colorScheme: ColorScheme,
        @JvmField val smallIcon: Drawable,
    )

    fun processArtwork(
        context: Context,
        imageLoader: ImageLoader,
        albumArtUri: Uri?,
        targetWidth: Int,
        targetHeight: Int,
        mainHandler: Handler,
        callback:
            (
                artwork: LayerDrawable?,
                colorScheme: ColorScheme?,
                albumArtUri: Uri?,
                smallIcon: Drawable?,
            ) -> Unit,
    ) {
        if (albumArtUri == null) {
            mainHandler.post { callback(null, null, null, null) }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap =
                if (albumArtUri.scheme == "http" || albumArtUri.scheme == "https") {
                    loadRemoteBitmap(albumArtUri)
                } else {
                    imageLoader.loadBitmapSync(
                        ImageLoader.Uri(albumArtUri),
                        targetWidth,
                        targetWidth,
                        android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE,
                    )
                }

            if (bitmap == null) {
                mainHandler.post { callback(null, null, albumArtUri, null) }
                return@launch
            }

            processBitmap(
                context,
                bitmap,
                albumArtUri,
                targetWidth,
                targetHeight,
                mainHandler,
                callback,
            )
        }
    }

    /**
     * ImageLoader only decodes through ContentResolver, so it cannot open the https cover URL that
     * online recognition returns. Fetch it directly, the same way the dynamic bar's Now Playing
     * pill does for its fallback cover lookup.
     */
    private fun loadRemoteBitmap(uri: Uri): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            connection =
                (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                    connectTimeout = ART_HTTP_CONNECT_MS
                    readTimeout = ART_HTTP_READ_MS
                    requestMethod = "GET"
                }
            if (connection.responseCode !in 200..299) return null
            val bytes = connection.inputStream.use { it.readBytes() }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.w(TAG, "Remote album art failed for $uri", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun processBitmap(
        context: Context,
        bitmap: Bitmap,
        albumArtUri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        mainHandler: Handler,
        callback:
            (
                artwork: LayerDrawable?,
                colorScheme: ColorScheme?,
                albumArtUri: Uri?,
                smallIcon: Drawable?,
            ) -> Unit,
    ) {
        val wallpaperColors = WallpaperColors.fromBitmap(bitmap)
        if (wallpaperColors == null) {
            mainHandler.post { callback(null, null, albumArtUri, null) }
            return
        }

        val colorScheme = ColorScheme(wallpaperColors, false, ThemeStyle.CONTENT)

        val iconSizePx = (48f * context.resources.displayMetrics.density).toInt()
        val smallIconBitmap = Bitmap.createScaledBitmap(bitmap, iconSizePx, iconSizePx, true)
        val smallIcon = BitmapDrawable(context.resources, smallIconBitmap)

        val scale =
            Math.max(targetWidth, targetHeight).toFloat() /
                Math.max(bitmap.width, bitmap.height).toFloat()
        val scaledWidth = (bitmap.width * scale).toInt()
        val scaledHeight = (bitmap.height * scale).toInt()

        val artworkBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(artworkBitmap)
        val matrix =
            Matrix().apply {
                postScale(scale, scale)
                postTranslate((targetWidth - scaledWidth) / 2f, (targetHeight - scaledHeight) / 2f)
            }
        canvas.drawBitmap(bitmap, matrix, null)

        val artworkDrawable = BitmapDrawable(context.resources, artworkBitmap)

        val drawable = context.getDrawable(com.android.systemui.res.R.drawable.qs_media_scrim)
        val scrimDrawable = (if (drawable != null) drawable.mutate() else null) as GradientDrawable
        val secondaryTone20 = colorScheme.materialScheme.secondaryPalette.tone(20)
        scrimDrawable.setColors(
            intArrayOf(
                getColorWithAlpha(secondaryTone20, 0.65f),
                getColorWithAlpha(secondaryTone20, 0.75f),
            )
        )

        val layeredArtwork = LayerDrawable(arrayOf(artworkDrawable, scrimDrawable))

        val result = ArtworkResult(albumArtUri, layeredArtwork, colorScheme, smallIcon)
        lastArtworkResult = result

        bitmap.recycle()

        mainHandler.post { callback(layeredArtwork, colorScheme, albumArtUri, smallIcon) }
    }
}
