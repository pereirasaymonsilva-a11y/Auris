package com.goldensystem.auris.presentation.screens

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
    
    // Estado para controlar se a instalação foi cancelada
    var installationCancelled by remember { mutableStateOf(false) }
    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    
    // Estado para controlar se o usuário já tentou instalar e cancelou
    var retryInstallation by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            showDownloadOptions = true
        }
    }

    // BroadcastReceiver para detectar quando a instalação é cancelada
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_REPLACED,
                    Intent.ACTION_PACKAGE_ADDED -> {
                        // App foi instalado com sucesso
                        val packageName = intent.data?.schemeSpecificPart
                        if (packageName == context?.packageName) {
                            Toast.makeText(context, "App instalado com sucesso!", Toast.LENGTH_LONG).show()
                            installationCancelled = false
                            downloadedFilePath = null
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        
        context.registerReceiver(receiver, filter)
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

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
                    val localUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                    )
                    val localFilename = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_FILENAME)
                    )
                    Quadruple(downloaded, total, status, localUri, localFilename)
                }
            }

            if (done == null) break

            val (downloaded, total, status, localUri, localFilename) = done

            if (total > 0) {
                progress = (downloaded * 100L / total).toInt()
            }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    isDownloading = false
                    
                    // Salva o caminho do arquivo baixado
                    val filePath = if (!localFilename.isNullOrEmpty()) {
                        localFilename
                    } else if (!localUri.isNullOrEmpty()) {
                        if (localUri.startsWith("file://")) Uri.parse(localUri).path
                        else manager.getUriForDownloadedFile(id)?.path ?: localUri
                    } else {
                        null
                    }
                    
                    if (filePath != null) {
                        downloadedFilePath = filePath
                        installationCancelled = false
                        retryInstallation = false
                        installApk(context, filePath)
                    } else {
                        Toast.makeText(context, "Erro ao localizar arquivo baixado", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
                DownloadManager.STATUS_FAILED,
                DownloadManager.STATUS_PAUSED -> {
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
                // Título dinâmico baseado no estado
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
                    // Caso 1: Instalação foi cancelada
                    installationCancelled -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Você cancelou a instalação. Deseja tentar novamente com o mesmo arquivo?",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                downloadedFilePath?.let { filePath ->
                                    installationCancelled = false
                                    retryInstallation = true
                                    installApk(context, filePath)
                                } ?: run {
                                    Toast.makeText(context, "Arquivo não encontrado. Faça o download novamente.", Toast.LENGTH_SHORT).show()
                                    installationCancelled = false
                                    showDownloadOptions = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("🔄 Tentar instalar novamente")
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        if (!updateInfo.isRequired) {
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
                    
                    // Caso 2: Está baixando
                    isDownloading -> {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("$progress%", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { 
                                downloadId?.let { id ->
                                    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                    manager.remove(id)
                                    downloadId = null
                                    isDownloading = false
                                    progress = 0
                                    Toast.makeText(context, "Download cancelado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Cancelar download")
                        }
                    }
                    
                    // Caso 3: Mostrar opções de download
                    else -> {
                        if (showDownloadOptions) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Botão de download pelo app
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

                                // Botão de download pelo site oficial
                                OutlinedButton(
                                    onClick = {
                                        openOfficialWebsite(context)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🌐 Baixar do site oficial")
                                }

                                Spacer(Modifier.height(8.dp))

                                if (!updateInfo.isRequired) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = onRemindLaterClick) { Text("Lembrar depois") }
                                        TextButton(onClick = onCancelClick) { Text("Fechar") }
                                    }
                                }
                            }
                        } else {
                            // Apenas botão de voltar quando o download foi iniciado
                            if (!updateInfo.isRequired) {
                                TextButton(
                                    onClick = { 
                                        showDownloadOptions = true
                                    }
                                ) {
                                    Text("Voltar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Função auxiliar para tentar instalar com callback de cancelamento
private fun installApk(context: Context, filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Arquivo não encontrado: $filePath", Toast.LENGTH_SHORT).show()
            return
        }
        
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        // Concede permissão temporária
        context.grantUriPermission(
            "com.android.packageinstaller",
            apkUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        
        // Também concede permissão para outros instaladores (Google Files, etc)
        listOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.documentsui",
            "com.google.android.documentsui"
        ).forEach { packageName ->
            try {
                context.grantUriPermission(
                    packageName,
                    apkUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignora se não conseguir conceder permissão
            }
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Toast.makeText(context, "Iniciando instalação...", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Nenhum instalador de APK encontrado", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao instalar APK: ${e.message}", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
}

// Função modificada para garantir nome de arquivo único
private fun startDownloadUnique(context: Context, updateInfo: AppVersionInfo, onIdReceived: (Long) -> Unit) {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val baseFileName = "auris_update_${updateInfo.version.replace(".", "_")}"
    
    // Verificar se o arquivo já existe e gerar nome único
    var counter = 1
    var finalFile: File
    var fileName: String
    
    do {
        fileName = if (counter == 1) {
            "$baseFileName.apk"
        } else {
            "$baseFileName($counter).apk"
        }
        finalFile = File(downloadsDir, fileName)
        counter++
    } while (finalFile.exists())
    
    val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
        .setTitle("Atualizando Auris - ${updateInfo.version}")
        .setDescription("Baixando nova versão... (${fileName})")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        .setAllowedOverMetered(true)
        .setDestinationUri(Uri.fromFile(finalFile))

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val id = manager.enqueue(request)
    onIdReceived(id)
    
    Toast.makeText(context, "Download iniciado: $fileName", Toast.LENGTH_SHORT).show()
}

// Função para abrir o site oficial
private fun openOfficialWebsite(context: Context) {
    try {
        val websiteUrl = "https://pereirasaymonsilva-a11y.github.io/Auris-website/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            Toast.makeText(
                context, 
                "Acesse a seção de download no site oficial", 
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(context, "Nenhum navegador encontrado", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao abrir o site: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Data class auxiliar para retornar múltiplos valores
private data class Quadruple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)