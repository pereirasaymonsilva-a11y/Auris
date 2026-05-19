package com.goldensystem.auris.presentation.screens

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.goldensystem.auris.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PiracyDialog(
    downloadUrl: String,
    officialPackage: String,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }

    // Todas as strings são obtidas AQUI, no escopo @Composable
    val dialogTitle = stringResource(R.string.piracy_dialog_title)
    val dialogMessage = stringResource(R.string.piracy_dialog_message)
    val downloadButtonText = stringResource(R.string.piracy_button_download)
    val exitButtonText = stringResource(R.string.piracy_button_exit)
    val downloadTitle = stringResource(R.string.piracy_download_title)
    val downloadDescription = stringResource(R.string.piracy_download_description)
    val downloadInterrupted = stringResource(R.string.piracy_download_interrupted)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDownload(context, downloadUrl, downloadTitle, downloadDescription) { id ->
                downloadId = id
                isDownloading = true
            }
        }
    }

    // Monitoramento do progresso do download
    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        while (true) {
            val done = withContext(Dispatchers.IO) {
                val query = DownloadManager.Query().setFilterById(id)
                manager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val downloaded = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val total = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    Triple(downloaded, total, status)
                }
            }

            if (done == null) break

            val (downloaded, total, status) = done

            if (total > 0) {
                progress = (downloaded * 100L / total).toInt()
            }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    isDownloading = false
                    val filePath = withContext(Dispatchers.IO) {
                        val query = DownloadManager.Query().setFilterById(id)
                        manager.query(query).use { cursor ->
                            if (cursor.moveToFirst()) {
                                val localUri = cursor.getString(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                                )
                                if (localUri != null) {
                                    if (localUri.startsWith("file://")) Uri.parse(localUri).path
                                    else manager.getUriForDownloadedFile(id)?.path ?: localUri
                                } else null
                            } else null
                        }
                    }
                    if (filePath != null) {
                        installApk(context, filePath)
                        onExit()
                    }
                    break
                }
                DownloadManager.STATUS_FAILED,
                DownloadManager.STATUS_PAUSED -> {
                    isDownloading = false
                    // Usamos o contexto para mostrar o Toast, não stringResource
                    Toast.makeText(context, downloadInterrupted, Toast.LENGTH_SHORT).show()
                    break
                }
            }

            delay(500)
        }
    }

    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier.padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dialogTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))

                if (isDownloading) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("$progress%", style = MaterialTheme.typography.labelMedium)
                } else {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                startDownload(context, downloadUrl, downloadTitle, downloadDescription) { id ->
                                    downloadId = id
                                    isDownloading = true
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(downloadButtonText)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(exitButtonText)
                    }
                }
            }
        }
    }
}

private fun startDownload(
    context: Context,
    downloadUrl: String,
    title: String,
    description: String,
    onIdReceived: (Long) -> Unit
) {
    val fileName = "auris_official_${System.currentTimeMillis()}.apk"
    val request = DownloadManager.Request(Uri.parse(downloadUrl))
        .setTitle(title)
        .setDescription(description)
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setAllowedOverMetered(true)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val id = manager.enqueue(request)
    onIdReceived(id)
}

private fun installApk(context: Context, filePath: String) {
    try {
        Toast.makeText(context, context.getString(R.string.piracy_download_completed), Toast.LENGTH_SHORT).show()

        val uri = Uri.parse(filePath)
        val apkUri = if (uri.scheme == "content") {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "auris_official_temp.apk")
            inputStream?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            inputStream?.close()
            FileProvider.getUriForFile(context, "${context.packageName}.provider", tempFile)
        } else {
            val file = File(uri.path ?: return)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        }

        context.grantUriPermission(
            "com.android.packageinstaller",
            apkUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.piracy_install_error), Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.piracy_apk_install_error, e.message), Toast.LENGTH_LONG).show()
    }
}