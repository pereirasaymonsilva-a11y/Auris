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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.goldensystem.auris.BuildConfig
import com.goldensystem.auris.data.model.AppVersionInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    updateInfo: AppVersionInfo,
    onCancelClick: () -> Unit,
    onRemindLaterClick: () -> Unit
) {
    val context = LocalContext.current
    var downloadId by remember { mutableStateOf<Long?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var isDownloading by remember { mutableStateOf(false) }
    var showDownloadOptions by remember { mutableStateOf(true) }
    var installationCancelled by remember { mutableStateOf(false) }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            showDownloadOptions = true
        }
    }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        while (true) {
            val result = withContext(Dispatchers.IO) {
                val query = DownloadManager.Query().setFilterById(id)
                manager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    
                    val downloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    
                    arrayOf(downloaded, total, status, localUri)
                }
            }
            
            if (result == null) break
            
            val downloaded = result[0] as Int
            val total = result[1] as Int
            val status = result[2] as Int
            val localUri = result[3] as String?

            if (total > 0) {
                progress = (downloaded * 100L / total).toInt()
            }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    isDownloading = false
                    val filePath = if (!localUri.isNullOrEmpty()) {
                        if (localUri.startsWith("file://")) Uri.parse(localUri).path else localUri
                    } else {
                        manager.getUriForDownloadedFile(id)?.path
                    }
                    if (filePath != null) {
                        downloadedFilePath = filePath
                        installationCancelled = false
                        installApk(context, filePath)
                    }
                    break
                }
                DownloadManager.STATUS_FAILED, DownloadManager.STATUS_PAUSED -> {
                    isDownloading = false
                    Toast.makeText(context, "Download interrompido", Toast.LENGTH_SHORT).show()
                    break
                }
            }
            delay(500)
        }
    }

    Dialog(onDismissRequest = { if (!updateInfo.isRequired && !installationCancelled) onCancelClick() }) {
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
                    when {
                        installationCancelled -> "Instalação cancelada ⚠️"
                        isDownloading -> "Baixando atualização 📥"
                        else -> "Nova versão 🚀"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(12.dp))
                Text("Atual: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text("Nova: ${updateInfo.version}", style = MaterialTheme.typography.bodyMedium)

                updateInfo.changelog?.let {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("📋 Novidades", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(20.dp))

                when {
                    installationCancelled -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Você cancelou a instalação. Deseja tentar novamente?",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                downloadedFilePath?.let { path ->
                                    installationCancelled = false
                                    installApk(context, path)
                                } ?: run {
                                    Toast.makeText(context, "Arquivo não encontrado", Toast.LENGTH_SHORT).show()
                                    installationCancelled = false
                                    showDownloadOptions = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔄 Tentar instalar novamente")
                        }
                        
                        if (!updateInfo.isRequired) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    installationCancelled = false
                                    showDownloadOptions = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Fazer novo download")
                            }
                        }
                    }
                    
                    isDownloading -> {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("$progress%")
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = {
                            downloadId?.let { id ->
                                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                manager.remove(id)
                                downloadId = null
                                isDownloading = false
                                progress = 0
                            }
                        }) {
                            Text("Cancelar download")
                        }
                    }
                    
                    else -> {
                        if (showDownloadOptions) {
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            startDownloadUnique(context, updateInfo) { id ->
                                                downloadId = id
                                                isDownloading = true
                                                showDownloadOptions = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📱 Baixar pelo app")
                                }

                                OutlinedButton(
                                    onClick = { openOfficialWebsite(context) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🌐 Baixar do site oficial")
                                }

                                if (!updateInfo.isRequired) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = onRemindLaterClick) { Text("Lembrar depois") }
                                        TextButton(onClick = onCancelClick) { Text("Fechar") }
                                    }
                                }
                            }
                        } else if (!updateInfo.isRequired) {
                            TextButton(onClick = { showDownloadOptions = true }) { Text("Voltar") }
                        }
                    }
                }
            }
        }
    }
}

private fun startDownloadUnique(context: Context, updateInfo: AppVersionInfo, onIdReceived: (Long) -> Unit) {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val baseFileName = "auris_update_${updateInfo.version.replace(".", "_")}"
    
    var counter = 1
    var finalFile: File
    var fileName: String
    
    do {
        fileName = if (counter == 1) "$baseFileName.apk" else "$baseFileName($counter).apk"
        finalFile = File(downloadsDir, fileName)
        counter++
    } while (finalFile.exists())
    
    val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
        .setTitle("Atualizando Auris - ${updateInfo.version}")
        .setDescription("Baixando... (${fileName})")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setAllowedOverMetered(true)
        .setDestinationUri(Uri.fromFile(finalFile))

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    onIdReceived(manager.enqueue(request))
    Toast.makeText(context, "Download: $fileName", Toast.LENGTH_SHORT).show()
}

private fun openOfficialWebsite(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pereirasaymonsilva-a11y.github.io/Auris-website/"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir site", Toast.LENGTH_SHORT).show()
    }
}

private fun installApk(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Arquivo não encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Erro: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}