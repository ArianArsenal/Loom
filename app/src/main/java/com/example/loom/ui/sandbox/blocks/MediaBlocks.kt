package com.example.loom.ui.sandbox.blocks

import kotlinx.coroutines.sync.withLock
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.loom.data.models.SandboxBlock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ImageBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    mediaLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Image", fontWeight = FontWeight.Bold)

            // --- FIX: Wrapped in Box for exact anchoring ---
            Box {
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        if (block.url.isEmpty()) {
            TextButton(onClick = { mediaLauncher.launch("image/*") }) { Text("Select Image") }
        } else {
            AsyncImage(
                model = block.url,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp))
            )
        }

        BasicTextField(
            value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = modifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
        )
    }
}

@Composable
fun VideoBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    videoLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Video", fontWeight = FontWeight.Bold)

            // --- FIX: Added Safe Deletion Menu ---
            Box {
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        if (block.url.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▶️", fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
                    TextButton(onClick = { videoLauncher.launch("video/*") }) { Text("Select Video") }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black)) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(block.url.toUri())
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            seekTo(1)
                        }
                    },
                    // Stop decoding when the block leaves the screen instead of leaking the player
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(onClick = { onUpdate(block.copy(url = "")) }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                    Icon(Icons.Default.Close, "Clear", tint = Color.White)
                }
            }
        }
        BasicTextField(
            value = textState, onValueChange = handleTextChange,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = modifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = placeholderUI
        )
    }
}

@Composable
fun AudioBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    mediaLauncher: ManagedActivityResultLauncher<String, Uri?>,
    audioLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onUpdate: (SandboxBlock) -> Unit,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }

    val fileName = remember(block.url) {
        var name = "Audio Recording"
        if (block.url.isNotEmpty()) {
            try {
                val uri = block.url.toUri()
                if (block.url.startsWith("content://")) {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) name = cursor.getString(nameIndex)
                    }
                } else name = uri.lastPathSegment ?: "Audio Recording"
            } catch (_: Exception) {}
        }
        name
    }

    var albumArt by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(block.url) {
        if (block.url.isNotEmpty()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val mmr = android.media.MediaMetadataRetriever()
                    try {
                        mmr.setDataSource(context, block.url.toUri())
                        val bytes = mmr.embeddedPicture
                        if (bytes != null) albumArt = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } finally {
                        mmr.release()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Audio", fontWeight = FontWeight.Bold)

            // --- FIX: Wrapped in Box for exact anchoring ---
            Box {
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options") }
                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        if (block.url.isEmpty()) {
            var isRecording by remember { mutableStateOf(false) }
            var recorder by remember { mutableStateOf<android.media.MediaRecorder?>(null) }
            var recordingFile by remember { mutableStateOf<File?>(null) }

            fun startRecording() {
                val file = File(context.filesDir, "media_${java.util.UUID.randomUUID()}.m4a")
                val r = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) android.media.MediaRecorder(context)
                        else @Suppress("DEPRECATION") android.media.MediaRecorder()
                try {
                    r.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
                    r.setOutputFormat(android.media.MediaRecorder.OutputFormat.MPEG_4)
                    r.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AAC)
                    r.setAudioEncodingBitRate(128_000)
                    r.setAudioSamplingRate(44_100)
                    r.setOutputFile(file.absolutePath)
                    r.prepare()
                    r.start()
                    recorder = r
                    recordingFile = file
                    isRecording = true
                } catch (e: Exception) {
                    r.release()
                    file.delete()
                    android.widget.Toast.makeText(context, "Couldn't start recording", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            // The shared launcher's result was ignored before, so "Record" never recorded
            val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) startRecording() }

            DisposableEffect(Unit) {
                onDispose { try { recorder?.stop() } catch (_: Exception) {}; recorder?.release() }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (isRecording) {
                    Button(onClick = {
                        try { recorder?.stop(); recorder?.release() } catch(_: Exception){}
                        recorder = null
                        isRecording = false
                        recordingFile?.let { onUpdate(block.copy(url = Uri.fromFile(it).toString())) }
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Stop Recording") }
                } else {
                    Button(onClick = {
                        val granted = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (granted) startRecording() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    }) { Text("🎤 Record") }
                    Button(onClick = { mediaLauncher.launch("audio/*") }) { Text("Upload") }
                }
            }
        } else {
            var isPlaying by remember { mutableStateOf(false) }
            var progress by remember { mutableFloatStateOf(0f) }
            var currentPosition by remember { mutableIntStateOf(0) }
            var duration by remember { mutableIntStateOf(0) }
            var playbackSpeed by remember { mutableFloatStateOf(1f) }
            val mediaPlayer = remember(block.url) { MediaPlayer() }

            DisposableEffect(block.url) {
                try {
                    mediaPlayer.setDataSource(context, block.url.toUri())
                    mediaPlayer.prepare()
                    duration = mediaPlayer.duration
                } catch (_: Exception) {}
                onDispose {
                    try {
                        if (mediaPlayer.isPlaying) mediaPlayer.stop()
                        mediaPlayer.release()
                    } catch (_: Exception) {}
                }
            }

            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    try {
                        currentPosition = mediaPlayer.currentPosition
                        if (duration > 0) progress = currentPosition.toFloat() / duration.toFloat()
                    } catch (_: Exception) {}
                    kotlinx.coroutines.delay(100.milliseconds)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (albumArt != null) {
                    Image(
                        bitmap = albumArt!!.asImageBitmap(),
                        contentDescription = "Cover",
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(fileName, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                try {
                                    if (isPlaying) {
                                        mediaPlayer.pause()
                                        isPlaying = false
                                    } else {
                                        if (progress >= 1f) mediaPlayer.seekTo(0)
                                        mediaPlayer.start()
                                        isPlaying = true
                                        mediaPlayer.setOnCompletionListener {
                                            isPlaying = false
                                            progress = 1f
                                        }
                                    }
                                } catch (_: Exception) { isPlaying = false }
                            },
                            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(20.dp)) }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(String.format(Locale.US, "%02d:%02d", currentPosition / 1000 / 60, currentPosition / 1000 % 60), style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = if (progress.isNaN()) 0f else progress,
                            onValueChange = {
                                progress = it
                                val seekTo = (it * duration).toInt()
                                mediaPlayer.seekTo(seekTo)
                                currentPosition = seekTo
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Text(String.format(Locale.US, "%02d:%02d", duration / 1000 / 60, duration / 1000 % 60), style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = {
                            playbackSpeed = when (playbackSpeed) { 1f -> 1.5f; 1.5f -> 2f; else -> 1f }
                            try { mediaPlayer.playbackParams = mediaPlayer.playbackParams.setSpeed(playbackSpeed) } catch (_: Exception){}
                        }, contentPadding = PaddingValues(0.dp), modifier = Modifier.width(40.dp)) { Text("${playbackSpeed}x", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }

        BasicTextField(
            value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = modifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
        )
    }
}

@Composable
fun FileBlockRenderer(
    block: SandboxBlock,
    textState: TextFieldValue,
    handleTextChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    placeholderUI: @Composable (@Composable () -> Unit) -> Unit,
    mediaLauncher: ManagedActivityResultLauncher<String, Uri?>,
    onDeleteBlock: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var showBlockMenu by remember { mutableStateOf(false) }
    var pdfBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isPdf by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val fileName = remember(block.url) {
        var name = "Attached File"
        if (block.url.isNotEmpty()) {
            try {
                val uri = block.url.toUri()
                if (block.url.startsWith("content://")) {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex != -1) name = cursor.getString(nameIndex)
                    }
                } else name = uri.lastPathSegment ?: "Attached File"
            } catch (_: Exception) {}
        }
        name
    }

    LaunchedEffect(block.url) {
        if (block.url.isNotEmpty()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val uri = block.url.toUri()
                    // use{} closes the descriptor even when PdfRenderer rejects a non-PDF file
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        android.graphics.pdf.PdfRenderer(pfd).use { renderer ->
                            if (renderer.pageCount > 0) {
                                isPdf = true
                                renderer.openPage(0).use { page ->
                                    val bitmap = createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    pdfBitmap = bitmap
                                }
                            }
                        }
                    }
                } catch (_: Exception) { isPdf = false }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("File Attachment", fontWeight = FontWeight.Bold)

            // --- FIX: Wrapped in Box for exact anchoring ---
            Box {
                IconButton(onClick = { showBlockMenu = true }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = Color.Gray) }
                DropdownMenu(expanded = showBlockMenu, onDismissRequest = { showBlockMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    DropdownMenuItem(
                        text = { Text("Open External") },
                        onClick = {
                            try {
                                val uri = block.url.toUri()
                                val finalUri = if (block.url.startsWith("file://")) {
                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", File(java.net.URI(block.url)))
                                } else { uri }
                                val resolvedMimeType = context.contentResolver.getType(finalUri) ?: "*/*"
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(finalUri, resolvedMimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Open File"))
                            } catch (_: Exception) {}
                            showBlockMenu = false
                        }
                    )
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { onDeleteBlock("", true) })
                }
            }
        }

        if (block.url.isEmpty()) {
            TextButton(onClick = { mediaLauncher.launch("*/*") }) { Text("Select File") }
        } else {
            if (isPdf && pdfBitmap != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)).clickable { showPdfDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Image(bitmap = pdfBitmap!!.asImageBitmap(), contentDescription = "PDF Preview", modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                    Box(modifier = Modifier.background(Color.Black.copy(alpha=0.6f), CircleShape).padding(8.dp)) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Expand", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.InsertDriveFile, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(fileName, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        BasicTextField(
            value = textState, onValueChange = handleTextChange, textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
            modifier = modifier.padding(top = 8.dp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = placeholderUI
        )
    }

    if (showPdfDialog) {
        Dialog(
            onDismissRequest = { showPdfDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showPdfDialog = false }) { Icon(Icons.Default.Close, "Close") }
                        Spacer(Modifier.width(16.dp))
                        Text(fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    var pageCount by remember { mutableIntStateOf(0) }
                    var pdfRenderer by remember { mutableStateOf<android.graphics.pdf.PdfRenderer?>(null) }
                    var pfd by remember { mutableStateOf<android.os.ParcelFileDescriptor?>(null) }
                    // PdfRenderer allows only one open page at a time; pages render from parallel coroutines
                    val renderLock = remember { kotlinx.coroutines.sync.Mutex() }

                    DisposableEffect(block.url) {
                        try {
                            val uri = block.url.toUri()
                            val fd = context.contentResolver.openFileDescriptor(uri, "r")
                            if (fd != null) {
                                pfd = fd
                                val renderer = try { android.graphics.pdf.PdfRenderer(fd) } catch (e: Exception) { fd.close(); pfd = null; throw e }
                                pdfRenderer = renderer
                                pageCount = renderer.pageCount
                            }
                        } catch (_: Exception) {}
                        onDispose {
                            val renderer = pdfRenderer
                            val descriptor = pfd
                            pdfRenderer = null
                            pageCount = 0
                            // Close only after any in-flight page render has finished
                            kotlinx.coroutines.runBlocking {
                                renderLock.withLock {
                                    try { renderer?.close() } catch (_: Exception) {}
                                    try { descriptor?.close() } catch (_: Exception) {}
                                }
                            }
                        }
                    }

                    if (pageCount > 0 && pdfRenderer != null) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(pageCount) { index ->
                                var pageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

                                LaunchedEffect(index) {
                                    withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        try {
                                            renderLock.withLock {
                                                val renderer = pdfRenderer ?: return@withLock
                                                renderer.openPage(index).use { page ->
                                                    val bitmap = createBitmap(page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888)
                                                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                    pageBitmap = bitmap
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }

                                if (pageBitmap != null) {
                                    Image(
                                        bitmap = pageBitmap!!.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clip(RoundedCornerShape(4.dp)).border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
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