package com.hakim.clinical.engine

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** نسخ ملف النموذج (GGUF) من الجهاز لمجلد التطبيق الداخلي. */
class ModelManager(private val context: Context) {

    data class ImportResult(val file: File, val displayName: String, val sizeBytes: Long)

    private val modelsDir: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    suspend fun copyIn(
        uri: Uri,
        onProgress: (fraction: Float, copied: Long, total: Long) -> Unit,
    ): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var name = "model.gguf"
        var size = -1L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0) c.getString(ni)?.let { name = it }
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }

        // تأكد إنه ملف GGUF فعلاً قبل نسخ غيغات
        val magicOk = resolver.openInputStream(uri)?.use { ins ->
            val magic = ByteArray(4)
            ins.read(magic) == 4 && String(magic, Charsets.US_ASCII) == "GGUF"
        } ?: throw IOException("ما قدرت افتح الملف.")
        if (!magicOk) throw IOException("هاد الملف مو نموذج GGUF. اختار ملف ينتهي بـ .gguf")

        if (size > 0 && modelsDir.usableSpace < size + SPARE_BYTES) {
            throw IOException("ما في مساحة كافية على الجهاز لنسخ النموذج.")
        }

        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            .let { if (it.endsWith(".gguf", ignoreCase = true)) it else "$it.gguf" }
        val tmp = File(modelsDir, "$safeName.part")
        val dest = File(modelsDir, safeName)

        val input = resolver.openInputStream(uri) ?: throw IOException("ما قدرت افتح الملف.")
        input.use { ins ->
            FileOutputStream(tmp).use { out ->
                val buf = ByteArray(1 shl 20)
                var copied = 0L
                var lastReport = 0L
                while (true) {
                    val r = ins.read(buf)
                    if (r < 0) break
                    out.write(buf, 0, r)
                    copied += r
                    if (copied - lastReport >= REPORT_STEP) {
                        lastReport = copied
                        onProgress(if (size > 0) copied.toFloat() / size else -1f, copied, size)
                    }
                    ensureActive()
                }
                out.fd.sync()
                onProgress(1f, copied, if (size > 0) size else copied)
            }
        }

        // احذف النماذج القديمة لنوفّر مساحة
        modelsDir.listFiles()?.forEach { f ->
            if (f.name != tmp.name) f.delete()
        }
        if (!tmp.renameTo(dest)) throw IOException("فشل حفظ ملف النموذج.")
        ImportResult(dest, name, dest.length())
    }

    companion object {
        private const val REPORT_STEP = 16L * 1024 * 1024
        private const val SPARE_BYTES = 300L * 1024 * 1024
    }
}
