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
    return downscale(applyOrientation(decoded, exifOrientation(source)))
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

  private fun exifOrientation(source: Uri): Int {
    return runCatching {
      context.contentResolver.openInputStream(source)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
  }

  /**
   * Applies all eight EXIF orientations, including the mirrored transpose and
   * transverse variants that a rotation-only transform would leave flipped.
   */
  private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> {
        matrix.setRotate(90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
      ExifInterface.ORIENTATION_TRANSVERSE -> {
        matrix.setRotate(-90f)
        matrix.postScale(-1f, 1f)
      }
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
      else -> return bitmap
    }
    val transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (transformed != bitmap) bitmap.recycle()
    return transformed
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
