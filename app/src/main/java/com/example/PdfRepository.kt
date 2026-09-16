package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

class PdfRepository(private val context: Context) {
    private val pdfsDir = File(context.filesDir, "pdfs").apply { mkdirs() }

    fun addSamplePdfIfNeeded() {
        val sampleFile = File(pdfsDir, "Welcome_to_PDF_Reader.pdf")
        if (!sampleFile.exists()) {
            val document = PdfDocument()
            
            // Page 1
            var pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Welcome to PDF Reader!", 200f, 100f, paint)
            paint.textSize = 16f
            canvas.drawText("Swipe left or right to turn pages.", 200f, 150f, paint)
            canvas.drawText("This is a sample reference book.", 200f, 200f, paint)
            document.finishPage(page)
            
            // Page 2
            pageInfo = PdfDocument.PageInfo.Builder(400, 600, 2).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawText("Bookmarks", 200f, 100f, paint)
            canvas.drawText("You can bookmark any page", 200f, 150f, paint)
            canvas.drawText("using the icon in the top right.", 200f, 200f, paint)
            document.finishPage(page)
            
            // Page 3
            pageInfo = PdfDocument.PageInfo.Builder(400, 600, 3).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawText("Neumorphic UI", 200f, 100f, paint)
            canvas.drawText("Enjoy the soft, modern interface!", 200f, 150f, paint)
            document.finishPage(page)

            try {
                FileOutputStream(sampleFile).use { out ->
                    document.writeTo(out)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                document.close()
            }
        }
    }

    fun getLocalPdfs(): List<File> {
        return pdfsDir.listFiles()?.toList()?.filter { it.extension == "pdf" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun savePdfFromUri(uri: Uri): File? {
        val fileName = getFileName(uri) ?: "document_${System.currentTimeMillis()}.pdf"
        // Ensure valid filename
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
        val destFile = File(pdfsDir, safeFileName)
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return destFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun deletePdf(file: File) {
        if (file.exists()) {
            file.delete()
        }
        val prefs = context.getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)
        prefs.edit().remove(file.name).apply()
        val pagesPrefs = context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE)
        pagesPrefs.edit().remove(file.name).apply()
    }

    fun getBookmarks(fileName: String): Set<Int> {
        val prefs = context.getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)
        return prefs.getStringSet(fileName, emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    fun toggleBookmark(fileName: String, page: Int) {
        val prefs = context.getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)
        val bookmarks = getBookmarks(fileName).toMutableSet()
        if (bookmarks.contains(page)) {
            bookmarks.remove(page)
        } else {
            bookmarks.add(page)
        }
        prefs.edit().putStringSet(fileName, bookmarks.map { it.toString() }.toSet()).apply()
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }
}
