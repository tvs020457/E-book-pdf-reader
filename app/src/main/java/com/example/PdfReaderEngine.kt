package com.example

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfReaderEngine(private val file: File) {
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    suspend fun open() = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPageCount(): Int = pdfRenderer?.pageCount ?: 0

    suspend fun renderPage(pageIndex: Int, width: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
        if (width <= 0) return@withContext null

        try {
            val page = renderer.openPage(pageIndex)
            
            // Calculate scale to fit width while maintaining aspect ratio
            val pageWidth = page.width.toFloat()
            val pageHeight = page.height.toFloat()
            val scale = width / pageWidth
            val scaledHeight = (pageHeight * scale).toInt()
            
            // Recreate bitmap with exact scaled dimensions
            val finalBitmap = Bitmap.createBitmap(width, maxOf(1, scaledHeight), Bitmap.Config.ARGB_8888)
            finalBitmap.eraseColor(android.graphics.Color.WHITE)
            
            page.render(finalBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return@withContext finalBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
