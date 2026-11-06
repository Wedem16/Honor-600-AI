package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedPhotoResult(
    val uri: Uri,
    val localFile: File,
    val displayName: String,
    val width: Int,
    val height: Int,
    val exifSummary: String
)

class MediaStoreManager(private val context: Context) {

    private val dateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val exifDateFormatter = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)

    /**
     * Saves an original captured photo scaled/formatted to the selected target resolution
     * (defaulting to 4K UHD 3840x2160) to both local app storage and Android MediaStore
     * under Pictures/<customFolder> with complete EXIF metadata.
     */
    fun saveOriginalPhoto(
        jpegBytes: ByteArray,
        targetWidth: Int,
        targetHeight: Int,
        customFolder: String,
        lensLabel: String,
        aperture: Float,
        focalLengthMm: Float,
        iso: Int,
        shutterSpeedNs: Long,
        whiteBalanceLabel: String
    ): SavedPhotoResult {
        val now = Date()
        val stamp = dateFormatter.format(now)
        val fileName = "IMG_4K_${stamp}.jpg"

        val localDir = File(context.filesDir, "captures/$customFolder").apply { mkdirs() }
        val localFile = File(localDir, fileName)

        val decoded = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        if (decoded != null) {
            val finalBitmap = if (decoded.width != targetWidth || decoded.height != targetHeight) {
                Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
            } else {
                decoded
            }
            FileOutputStream(localFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 96, out)
            }
        } else {
            FileOutputStream(localFile).use { it.write(jpegBytes) }
        }

        val shutterSeconds = shutterSpeedNs / 1_000_000_000.0
        val shutterFormatted = if (shutterSeconds >= 1.0) {
            String.format(Locale.US, "%.1fs", shutterSeconds)
        } else {
            val denom = (1.0 / shutterSeconds).toInt().coerceAtLeast(1)
            "1/${denom}s"
        }

        writeExifMetadata(
            file = localFile,
            date = now,
            aperture = aperture,
            focalLengthMm = focalLengthMm,
            iso = iso,
            shutterSeconds = shutterSeconds,
            userComment = "HONOR 600 4K Original • $lensLabel • WB $whiteBalanceLabel"
        )

        val mediaUri = exportFileToMediaStore(
            sourceFile = localFile,
            displayName = fileName,
            mimeType = "image/jpeg",
            relativeFolder = "${Environment.DIRECTORY_PICTURES}/$customFolder"
        ) ?: Uri.fromFile(localFile)

        val exifSummary = "4K (${targetWidth}×${targetHeight}) • ISO $iso • $shutterFormatted • f/$aperture"
        return SavedPhotoResult(
            uri = mediaUri,
            localFile = localFile,
            displayName = fileName,
            width = targetWidth,
            height = targetHeight,
            exifSummary = exifSummary
        )
    }

    /**
     * Saves the On-Device Auto AI enhanced photo as a companion _AI.jpg file in MediaStore,
     * preserving EXIF tags from the original capture.
     */
    fun saveAiEnhancedPhoto(
        enhancedBytes: ByteArray,
        originalDisplayName: String,
        customFolder: String,
        sceneSummary: String
    ): SavedPhotoResult {
        val baseName = originalDisplayName.removeSuffix(".jpg").removeSuffix(".png")
        val aiFileName = "${baseName}_AI.jpg"

        val localDir = File(context.filesDir, "captures/$customFolder").apply { mkdirs() }
        val aiLocalFile = File(localDir, aiFileName)

        FileOutputStream(aiLocalFile).use { out ->
            out.write(enhancedBytes)
        }

        val originalFile = File(localDir, originalDisplayName)
        copyExifAndAnnotateAi(
            originalFile = if (originalFile.exists()) originalFile else null,
            targetFile = aiLocalFile,
            sceneSummary = sceneSummary
        )

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(aiLocalFile.absolutePath, bounds)

        val mediaUri = exportFileToMediaStore(
            sourceFile = aiLocalFile,
            displayName = aiFileName,
            mimeType = "image/jpeg",
            relativeFolder = "${Environment.DIRECTORY_PICTURES}/$customFolder"
        ) ?: Uri.fromFile(aiLocalFile)

        val w = if (bounds.outWidth > 0) bounds.outWidth else 3840
        val h = if (bounds.outHeight > 0) bounds.outHeight else 2160

        return SavedPhotoResult(
            uri = mediaUri,
            localFile = aiLocalFile,
            displayName = aiFileName,
            width = w,
            height = h,
            exifSummary = "4K Auto AI ($sceneSummary)"
        )
    }

    fun createOpticalCalibrationJpeg(): ByteArray {
        val bitmap = try {
            val opts = BitmapFactory.Options().apply { inMutable = true }
            BitmapFactory.decodeResource(
                context.resources,
                R.drawable.img_optics_studio_banner_1793975946933,
                opts
            )
        } catch (_: Exception) {
            null
        } ?: Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#08090C"),
                    Color.parseColor("#182238"),
                    Color.parseColor("#3D2B00")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        val outStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
        return outStream.toByteArray()
    }

    fun resolveLocalFileForCapture(displayName: String, customFolder: String): File? {
        val file = File(context.filesDir, "captures/$customFolder/$displayName")
        return if (file.exists()) file else null
    }

    private fun writeExifMetadata(
        file: File,
        date: Date,
        aperture: Float,
        focalLengthMm: Float,
        iso: Int,
        shutterSeconds: Double,
        userComment: String
    ) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val formattedDate = exifDateFormatter.format(date)
            exif.setAttribute(ExifInterface.TAG_MAKE, "HONOR")
            exif.setAttribute(ExifInterface.TAG_MODEL, "HONOR 600")
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Lumina 600 Auto AI Camera")
            exif.setAttribute(ExifInterface.TAG_DATETIME, formattedDate)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, formattedDate)
            exif.setAttribute(ExifInterface.TAG_F_NUMBER, aperture.toString())
            exif.setAttribute(ExifInterface.TAG_FOCAL_LENGTH, "${(focalLengthMm * 100).toInt()}/100")
            exif.setAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, iso.toString())
            exif.setAttribute(ExifInterface.TAG_EXPOSURE_TIME, String.format(Locale.US, "%.6f", shutterSeconds))
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, userComment)
            exif.saveAttributes()
        } catch (_: Exception) {
        }
    }

    private fun copyExifAndAnnotateAi(
        originalFile: File?,
        targetFile: File,
        sceneSummary: String
    ) {
        try {
            val targetExif = ExifInterface(targetFile.absolutePath)
            if (originalFile != null && originalFile.exists()) {
                val srcExif = ExifInterface(originalFile.absolutePath)
                val tagsToCopy = listOf(
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_WHITE_BALANCE
                )
                for (tag in tagsToCopy) {
                    val value = srcExif.getAttribute(tag)
                    if (value != null) {
                        targetExif.setAttribute(tag, value)
                    }
                }
            } else {
                targetExif.setAttribute(ExifInterface.TAG_MAKE, "HONOR")
                targetExif.setAttribute(ExifInterface.TAG_MODEL, "HONOR 600")
            }
            targetExif.setAttribute(
                ExifInterface.TAG_SOFTWARE,
                "Lumina 600 On-Device Auto AI ISP"
            )
            targetExif.setAttribute(
                ExifInterface.TAG_USER_COMMENT,
                "On-Device Auto AI Enhanced | $sceneSummary"
            )
            targetExif.saveAttributes()
        } catch (_: Exception) {
        }
    }

    private fun exportFileToMediaStore(
        sourceFile: File,
        displayName: String,
        mimeType: String,
        relativeFolder: String
    ): Uri? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val itemUri = resolver.insert(collection, values) ?: return null
            resolver.openOutputStream(itemUri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(itemUri, completeValues, null, null)
            }
            itemUri
        } catch (_: Exception) {
            null
        }
    }
}
