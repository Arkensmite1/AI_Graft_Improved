package com.aigraft.farmer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoProcessor {

    suspend fun prepareFromUri(
        context: Context,
        uri: Uri,
        mode: ScanMode,
        batchId: String?,
        scansDir: File,
    ): PendingScan = withContext(Dispatchers.IO) {
        val rotation = readExifRotation(context, uri)
        val raw = decodeSampledBitmap(context, uri, targetMaxEdge = 1280)
            ?: throw IllegalStateException("Could not decode selected photo.")
        val oriented = rotateIfNeeded(raw, rotation)
        if (oriented !== raw) raw.recycle()
        buildPendingScan(oriented, mode, batchId, scansDir)
    }

    suspend fun prepareFromFile(
        file: File,
        mode: ScanMode,
        batchId: String?,
        scansDir: File,
    ): PendingScan = withContext(Dispatchers.IO) {
        val rotation = readExifRotation(file)
        val raw = decodeSampledBitmap(file, targetMaxEdge = 1280)
            ?: throw IllegalStateException("Could not decode captured photo.")
        val oriented = rotateIfNeeded(raw, rotation)
        if (oriented !== raw) raw.recycle()
        buildPendingScan(oriented, mode, batchId, scansDir)
    }

    private fun buildPendingScan(
        source: Bitmap,
        mode: ScanMode,
        batchId: String?,
        scansDir: File,
    ): PendingScan {
        val id = UUID.randomUUID().toString()
        val analysisBitmap = scaleDown(source, maxEdge = 1024)
        val displayBitmap = scaleDown(analysisBitmap, maxEdge = 640)
        val thumbBitmap = scaleDown(displayBitmap, maxEdge = 240)

        if (source !== analysisBitmap) source.recycle()

        val photoFile = File(scansDir, "photo_$id.jpg")
        val thumbFile = File(scansDir, "thumb_$id.jpg")

        saveJpeg(displayBitmap, photoFile, quality = 78)
        saveJpeg(thumbBitmap, thumbFile, quality = 70)

        val aspect = if (analysisBitmap.height > 0) {
            analysisBitmap.width.toFloat() / analysisBitmap.height.toFloat()
        } else {
            4f / 3.4f
        }

        if (displayBitmap !== analysisBitmap) displayBitmap.recycle()
        if (thumbBitmap !== displayBitmap && thumbBitmap !== analysisBitmap) thumbBitmap.recycle()

        return PendingScan(
            id = id,
            mode = mode,
            photoPath = photoFile.absolutePath,
            thumbPath = thumbFile.absolutePath,
            aspectRatio = aspect,
            analysisBitmap = analysisBitmap,
            batchId = batchId,
        )
    }

    private fun saveJpeg(bitmap: Bitmap, outFile: File, quality: Int) {
        FileOutputStream(outFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
    }

    private fun scaleDown(src: Bitmap, maxEdge: Int): Bitmap {
        val largest = max(src.width, src.height)
        if (largest <= maxEdge) return src
        val ratio = maxEdge.toFloat() / largest.toFloat()
        val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, targetMaxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        val opts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, targetMaxEdge)
        }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun decodeSampledBitmap(file: File, targetMaxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, targetMaxEdge)
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun computeSampleSize(width: Int, height: Int, targetMaxEdge: Int): Int {
        var sample = 1
        val largest = max(width, height)
        if (largest > targetMaxEdge) {
            var half = largest / 2
            while (half / sample >= targetMaxEdge) {
                sample *= 2
            }
        }
        return sample
    }

    private fun readExifRotation(context: Context, uri: Uri): Int = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            exifDegrees(ExifInterface(stream))
        } ?: 0
    }.getOrDefault(0)

    private fun readExifRotation(file: File): Int = runCatching {
        exifDegrees(ExifInterface(file.absolutePath))
    }.getOrDefault(0)

    private fun exifDegrees(exif: ExifInterface): Int =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

    private fun rotateIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
