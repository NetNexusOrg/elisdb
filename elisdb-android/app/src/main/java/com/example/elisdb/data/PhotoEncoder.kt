package com.example.elisdb.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Normalizes photos before storage: decodes with sampling to bound memory,
 * applies EXIF orientation, downscales to a maximum side, and encodes WebP
 * lossy. Used by both the gallery and camera paths so stored photos are small
 * and consistent regardless of source.
 */
class PhotoEncoder(private val context: Context) {

  /**
   * Encodes [source] to a new WebP file inside [dir]. Never modifies [source];
   * on failure the partially-written destination is deleted and an exception is
   * thrown so no broken photo is attached.
   */
  fun encodeToWebp(source: Uri, dir: File): File {
    dir.mkdirs()
    val dest = File(dir, "foto_${UUID.randomUUID()}.webp")
    val bitmap = decode(source) ?: run { dest.delete(); error("Não foi possível ler a imagem.") }
    try {
      FileOutputStream(dest).use { out ->
        if (!bitmap.compress(webpFormat(), WEBP_QUALITY, out)) {
          dest.delete()
          error("Não foi possível codificar a imagem.")
        }
      }
    } finally {
      bitmap.recycle()
    }
    return dest
  }

  /**
   * Lossy WebP format. `Bitmap.CompressFormat.WEBP` is deprecated in favour of
   * `WEBP_LOSSY` from API 30; the old constant behaves as lossy, so this keeps
   * the same output on all supported API levels.
   */
  @Suppress("DEPRECATION")
  private fun webpFormat(): Bitmap.CompressFormat =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Bitmap.CompressFormat.WEBP_LOSSY
    } else {
      Bitmap.CompressFormat.WEBP
    }

  private fun decode(source: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openStream(resolver, source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply { inSampleSize = computeSampleSize(bounds) }
    val decoded =
      openStream(resolver, source)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: return null
    return downscale(rotate(decoded, exifRotationDegrees(source)))
  }

  private fun openStream(resolver: android.content.ContentResolver, source: Uri): java.io.InputStream? =
    try {
      resolver.openInputStream(source)
    } catch (e: java.io.IOException) {
      null
    }

  private fun computeSampleSize(bounds: BitmapFactory.Options): Int {
    var sample = 1
    while (bounds.outWidth / sample > MAX_DIM * 2 || bounds.outHeight / sample > MAX_DIM * 2) {
      sample *= 2
    }
    return sample
  }

  private fun exifRotationDegrees(source: Uri): Float {
    val orientation =
      runCatching {
        context.contentResolver.openInputStream(source)?.use {
          ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
      }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
    return when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> 90f
      ExifInterface.ORIENTATION_ROTATE_180 -> 180f
      ExifInterface.ORIENTATION_ROTATE_270 -> 270f
      else -> 0f
    }
  }

  private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (rotated != bitmap) bitmap.recycle()
    return rotated
  }

  private fun downscale(bitmap: Bitmap): Bitmap {
    val largest = max(bitmap.width, bitmap.height)
    if (largest <= MAX_DIM) return bitmap
    val ratio = MAX_DIM.toFloat() / largest
    val scaled = bitmap.scale((bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    if (scaled != bitmap) bitmap.recycle()
    return scaled
  }

  companion object {
    const val MAX_DIM = 1800
    const val WEBP_QUALITY = 82
  }
}
