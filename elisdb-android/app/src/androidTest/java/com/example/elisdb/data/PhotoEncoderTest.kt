package com.example.elisdb.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PhotoEncoderTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val encoder = PhotoEncoder(context)
  private lateinit var outDir: File

  @Before
  fun setUp() {
    outDir = File(context.cacheDir, "photoencoder_test").apply { deleteRecursively(); mkdirs() }
  }

  @After
  fun tearDown() {
    outDir.deleteRecursively()
  }

  private fun writeJpeg(width: Int, height: Int): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.RED)
    val file = File(outDir, "src_${width}x$height.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    bitmap.recycle()
    return file
  }

  private fun decodeDims(file: File): Pair<Int, Int> {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, opts)
    return opts.outWidth to opts.outHeight
  }

  @Test
  fun largeImage_isDownscaledToMaxDim() {
    val source = writeJpeg(4000, 2000)
    val dest = encoder.encodeToWebp(Uri.fromFile(source), outDir)

    assertEquals(1800 to 900, decodeDims(dest))
  }

  @Test
  fun smallImage_isNotUpscaled() {
    val source = writeJpeg(800, 600)
    val dest = encoder.encodeToWebp(Uri.fromFile(source), outDir)

    assertEquals(800 to 600, decodeDims(dest))
  }

  @Test
  fun output_isWebp() {
    val source = writeJpeg(200, 200)
    val dest = encoder.encodeToWebp(Uri.fromFile(source), outDir)
    val bytes = dest.readBytes()

    assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
    assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
  }

  @Test
  fun exifRotation_isAppliedToPixels() {
    val source = writeJpeg(200, 100)
    ExifInterface(source.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
      saveAttributes()
    }

    val dest = encoder.encodeToWebp(Uri.fromFile(source), outDir)

    assertEquals(100 to 200, decodeDims(dest))
  }

  private fun writeTwoColorJpeg(width: Int = 200, height: Int = 100): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (y in 0 until height) {
      for (x in 0 until width) {
        bitmap.setPixel(x, y, if (x < width / 2) Color.RED else Color.BLUE)
      }
    }
    val file = File(outDir, "two_${width}x$height.jpg")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    bitmap.recycle()
    return file
  }

  private fun firstPixel(file: File): Int {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
    val pixel = bitmap.getPixel(0, 0)
    bitmap.recycle()
    return pixel
  }

  @Test
  fun allEightExifOrientations_areApplied() {
    val expectedDims =
      mapOf(
        ExifInterface.ORIENTATION_NORMAL to (200 to 100),
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL to (200 to 100),
        ExifInterface.ORIENTATION_ROTATE_180 to (200 to 100),
        ExifInterface.ORIENTATION_FLIP_VERTICAL to (200 to 100),
        ExifInterface.ORIENTATION_TRANSPOSE to (100 to 200),
        ExifInterface.ORIENTATION_ROTATE_90 to (100 to 200),
        ExifInterface.ORIENTATION_TRANSVERSE to (100 to 200),
        ExifInterface.ORIENTATION_ROTATE_270 to (100 to 200),
      )
    expectedDims.forEach { (orientation, dims) ->
      val source = writeTwoColorJpeg()
      ExifInterface(source.absolutePath).apply {
        setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        saveAttributes()
      }
      val dest = encoder.encodeToWebp(Uri.fromFile(source), outDir)
      assertEquals("orientation $orientation dimensions", dims, decodeDims(dest))
    }

    val flipped = writeTwoColorJpeg()
    ExifInterface(flipped.absolutePath).apply {
      setAttribute(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL.toString(),
      )
      saveAttributes()
    }
    val flippedOut = encoder.encodeToWebp(Uri.fromFile(flipped), outDir)
    val pixel = firstPixel(flippedOut)
    assertTrue("flip horizontal moves blue to the left", Color.blue(pixel) > Color.red(pixel))
  }

  @Test
  fun missingSource_throwsAndLeavesNoBrokenFile() {
    val missing = File(outDir, "nao-existe.jpg")

    assertThrows(IllegalStateException::class.java) {
      encoder.encodeToWebp(Uri.fromFile(missing), outDir)
    }
    assertTrue(outDir.listFiles()?.none { it.name.endsWith(".webp") } ?: true)
  }
}
