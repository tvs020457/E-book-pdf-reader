package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ReaderView(
    fileName: String,
    onBack: () -> Unit,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    val neuColors = LocalNeuColors.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val pdfsDir = File(context.filesDir, "pdfs")
    val file = File(pdfsDir, fileName)
    
    val engine = remember { PdfReaderEngine(file) }
    var pageCount by remember { mutableIntStateOf(0) }
    
    val sharedPrefs = remember { context.getSharedPreferences("pdf_prefs", Context.MODE_PRIVATE) }
    var currentPage by remember { mutableIntStateOf(sharedPrefs.getInt(fileName, 0)) }
    
    val repository = remember { PdfRepository(context) }
    var bookmarks by remember { mutableStateOf(repository.getBookmarks(fileName)) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var viewWidth by remember { mutableIntStateOf(0) }
    
    var direction by remember { mutableIntStateOf(1) } // 1 for next, -1 for prev
    var isReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        engine.open()
        pageCount = engine.getPageCount()
        if (currentPage >= pageCount) {
            currentPage = maxOf(0, pageCount - 1)
        }
        isReady = true
    }

    DisposableEffect(Unit) {
        onDispose {
            sharedPrefs.edit().putInt(fileName, currentPage).apply()
            coroutineScope.launch { engine.close() }
        }
    }

    LaunchedEffect(currentPage) {
        sharedPrefs.edit().putInt(fileName, currentPage).apply()
    }

    LaunchedEffect(currentPage, viewWidth, isReady) {
        if (isReady && viewWidth > 0 && pageCount > 0) {
            val bmp = engine.renderPage(currentPage, viewWidth)
            currentBitmap = bmp
        }
    }

    val isBookmarked = bookmarks.contains(currentPage)

    val pdfColorFilter = remember(currentTheme) {
        when (currentTheme) {
            "dark" -> ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                -1f,  0f,  0f,  0f, 255f,
                 0f, -1f,  0f,  0f, 255f,
                 0f,  0f, -1f,  0f, 255f,
                 0f,  0f,  0f,  1f,   0f
            )))
            "sepia" -> {
                val lumR = 0.299f
                val lumG = 0.587f
                val lumB = 0.114f

                val dr = (244f - 91f) / 255f
                val dg = (236f - 70f) / 255f
                val db = (216f - 54f) / 255f

                ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                    dr * lumR, dr * lumG, dr * lumB, 0f, 91f,
                    dg * lumR, dg * lumG, dg * lumB, 0f, 70f,
                    db * lumR, db * lumG, db * lumB, 0f, 54f,
                    0f, 0f, 0f, 1f, 0f
                )))
            }
            else -> null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(neuColors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NeuButton(
                    onClick = onBack,
                    isCircle = true,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = neuColors.text)
                }
                
                Text(
                    text = fileName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = neuColors.text,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    maxLines = 1
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeuButton(
                        onClick = {
                            val nextTheme = when (currentTheme) {
                                "light" -> "dark"
                                "dark" -> "sepia"
                                else -> "light"
                            }
                            onThemeChange(nextTheme)
                        },
                        isCircle = true,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Change Theme", tint = neuColors.text)
                    }
                    
                    NeuButton(
                        onClick = { showBookmarksDialog = true },
                        isCircle = true,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "Bookmarks List", tint = neuColors.text)
                    }
                    
                    NeuButton(
                        onClick = {
                            repository.toggleBookmark(fileName, currentPage)
                            bookmarks = repository.getBookmarks(fileName)
                        },
                        isCircle = true,
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Toggle Bookmark",
                            tint = if (isBookmarked) neuColors.accent else neuColors.text
                        )
                    }
                }
            }

            // Subdued Progress Bar
            val progress = if (pageCount > 0) (currentPage + 1).toFloat() / pageCount.toFloat() else 0f
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(4.dp),
                color = neuColors.accent,
                trackColor = neuColors.darkShadow.copy(alpha = 0.3f)
            )

            // Document Viewer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onSizeChanged { size -> viewWidth = size.width }
                    .pointerInput(Unit) {
                        var dragSum = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = { dragSum = 0f },
                            onDragCancel = { dragSum = 0f }
                        ) { change, dragAmount ->
                            change.consume()
                            dragSum += dragAmount
                            if (dragSum < -200f && currentPage < pageCount - 1) {
                                direction = 1
                                currentPage++
                                dragSum = 0f
                            } else if (dragSum > 200f && currentPage > 0) {
                                direction = -1
                                currentPage--
                                dragSum = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (currentBitmap != null) {
                    AnimatedContent(
                        targetState = currentPage to currentBitmap!!,
                        transitionSpec = {
                            if (direction > 0) {
                                slideInHorizontally(
                                    animationSpec = tween(300),
                                    initialOffsetX = { fullWidth -> fullWidth }
                                ) togetherWith slideOutHorizontally(
                                    animationSpec = tween(300),
                                    targetOffsetX = { fullWidth -> -fullWidth }
                                )
                            } else {
                                slideInHorizontally(
                                    animationSpec = tween(300),
                                    initialOffsetX = { fullWidth -> -fullWidth }
                                ) togetherWith slideOutHorizontally(
                                    animationSpec = tween(300),
                                    targetOffsetX = { fullWidth -> fullWidth }
                                )
                            }
                        },
                        label = "PageTransition"
                    ) { (_, bmp) ->
                        NeuCard(
                            cornerRadius = 8.dp,
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Page ${currentPage + 1}",
                                modifier = Modifier.fillMaxWidth(),
                                colorFilter = pdfColorFilter
                            )
                        }
                    }
                } else if (pageCount > 0) {
                    CircularProgressIndicator(color = neuColors.accent)
                } else {
                    Text("Loading document...", color = neuColors.text)
                }
            }

            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuButton(
                    onClick = {
                        if (currentPage > 0) {
                            direction = -1
                            currentPage--
                        }
                    },
                    isCircle = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Previous", tint = neuColors.text)
                }
                
                NeuCard(
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        "${currentPage + 1} / ${maxOf(1, pageCount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = neuColors.text
                    )
                }
                
                NeuButton(
                    onClick = {
                        if (currentPage < pageCount - 1) {
                            direction = 1
                            currentPage++
                        }
                    },
                    isCircle = true,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = neuColors.text)
                }
            }
        }

        if (showBookmarksDialog) {
            Dialog(onDismissRequest = { showBookmarksDialog = false }) {
                NeuCard(
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(24.dp)
                    ) {
                        Text(
                            "Bookmarks",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = neuColors.text,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (bookmarks.isEmpty()) {
                            Text("No bookmarks yet.", color = neuColors.text.copy(alpha = 0.7f))
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(bookmarks.sorted()) { page ->
                                    NeuButton(
                                        onClick = {
                                            direction = if (page > currentPage) 1 else -1
                                            currentPage = page
                                            showBookmarksDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Page ${page + 1}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = neuColors.text
                                            )
                                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = neuColors.accent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
