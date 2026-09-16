package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeView(
    onNavigateToReader: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { PdfRepository(context) }
    var pdfs by remember { mutableStateOf(emptyList<File>()) }

    LaunchedEffect(Unit) {
        repository.addSamplePdfIfNeeded()
        pdfs = repository.getLocalPdfs()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            repository.savePdfFromUri(it)
            pdfs = repository.getLocalPdfs()
        }
    }

    val neuColors = LocalNeuColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(neuColors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Custom Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Library",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = neuColors.text
                )
            }

            if (pdfs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = neuColors.text.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No books yet",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = neuColors.text
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to upload a PDF",
                            fontSize = 14.sp,
                            color = neuColors.text.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(pdfs, key = { it.name }) { file ->
                        PdfItem(
                            modifier = Modifier.animateItem(),
                            file = file,
                            onClick = { onNavigateToReader(file.name) },
                            onDelete = {
                                repository.deletePdf(file)
                                pdfs = repository.getLocalPdfs()
                            }
                        )
                    }
                }
            }
        }

        // FAB
        NeuButton(
            onClick = { launcher.launch("application/pdf") },
            isCircle = true,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add PDF", tint = neuColors.accent, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun PdfItem(
    modifier: Modifier = Modifier,
    file: File,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val neuColors = LocalNeuColors.current
    NeuButton(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = neuColors.accent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = neuColors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${file.length() / 1024} KB",
                    fontSize = 12.sp,
                    color = neuColors.text.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            NeuButton(
                onClick = onDelete,
                isCircle = true,
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = neuColors.red,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
