package dev.hive.mobile

// Photos and files picked with + in a thread. A photo is shrunk to a JPEG of at most 1568 px on its long side (what
// Claude reads at full detail anyway); any other file goes as it is. Everything sent at once stays under BUDGET bytes,
// which keeps the sealed message under the relay's 2 MB frame.
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

object Attachments {
    const val BUDGET = 1_000_000
    private const val EDGE = 1568

    // the picked files, read; the second value says what was left out and why ("" when nothing was)
    suspend fun read(ctx: Context, uris: List<Uri>, already: List<Attachment>): Pair<List<Attachment>, String> = withContext(Dispatchers.IO) {
        var used = already.sumOf { it.data.length * 3 / 4 }
        val got = mutableListOf<Attachment>()
        val skipped = mutableListOf<String>()
        for (uri in uris) {
            val name = nameOf(ctx, uri)
            val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
            val bytes = try {
                if (mime.startsWith("image/")) photo(ctx, uri) else ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) { null }
            if (bytes == null) { skipped += "$name could not be read"; continue }
            if (used + bytes.size > BUDGET) { skipped += "$name is too big (up to ${BUDGET / 1_000_000} MB per message)"; continue }
            used += bytes.size
            val asPhoto = mime.startsWith("image/")
            got += Attachment(if (asPhoto && !name.endsWith(".jpg", true)) name.substringBeforeLast('.') + ".jpg" else name, if (asPhoto) "image/jpeg" else mime, Base64.encodeToString(bytes, Base64.NO_WRAP))
        }
        got to skipped.joinToString(". ")
    }

    fun json(files: List<Attachment>): JSONArray = JSONArray(files.map { JSONObject().put("name", it.name).put("mime", it.mime).put("data", it.data) })

    // decoded at about the size it is sent at, then scaled to fit EDGE and saved as a JPEG
    private fun photo(ctx: Context, uri: Uri): ByteArray? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val long = maxOf(bounds.outWidth, bounds.outHeight)
        if (long <= 0) return null
        var sample = 1
        while (long / (sample * 2) >= EDGE) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val scale = minOf(1f, EDGE.toFloat() / maxOf(bmp.width, bmp.height))
        // the camera's own turn of the picture (EXIF), kept, so a portrait photo does not arrive on its side
        val turn = ctx.contentResolver.openInputStream(uri)?.use {
            when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) { 6 -> 90f; 3 -> 180f; 8 -> 270f; else -> 0f }
        } ?: 0f
        val m = android.graphics.Matrix().apply { if (scale < 1f) postScale(scale, scale); if (turn != 0f) postRotate(turn) }
        val fit = if (scale < 1f || turn != 0f) Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true) else bmp
        return ByteArrayOutputStream().also { fit.compress(Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
    }

    private fun nameOf(ctx: Context, uri: Uri): String =
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            ?: uri.lastPathSegment?.substringAfterLast('/') ?: "file"
}
