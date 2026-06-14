package com.goldensystem.auris.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pix
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.goldensystem.auris.R
import com.goldensystem.auris.utils.generatePixQRCode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    
    // Dados do PIX
    val pixKey = "72fc21a2-0278-4224-8b47-28b15725e2b4"
    val pixPayload = "00020126580014BR.GOV.BCB.PIX013672fc21a2-0278-4224-8b47-28b15725e2b45204000053039865802BR5920SAYMON SILVA PEREIRA6015Divino de Sao L61082959000062070503***6304595B"
    
    // Estados
    var showCopiedFeedback by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGeneratingQR by remember { mutableStateOf(true) }
    
    // Gerar QR Code
    LaunchedEffect(Unit) {
        isGeneratingQR = true
        qrCodeBitmap = generatePixQRCode(pixPayload, 300, 300)
        isGeneratingQR = false
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Título
            Icon(
                imageVector = Icons.Rounded.Pix,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.donate_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.donate_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // ========== CARD PIX COM QR CODE ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cabeçalho PIX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("🇧🇷 ", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = stringResource(R.string.donate_pix_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // QR Code
                    if (isGeneratingQR) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        }
                    } else if (qrCodeBitmap != null) {
                        Card(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    // Ampliar QR Code se quiser
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Image(
                                bitmap = qrCodeBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code PIX",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    // Chave PIX para copiar
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.donate_pix_key),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = pixKey,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(pixKey))
                                    scope.launch {
                                        showCopiedFeedback = true
                                        kotlinx.coroutines.delay(2000)
                                        showCopiedFeedback = false
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar chave PIX",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Feedback de cópia
                    if (showCopiedFeedback) {
                        AssistChip(
                            onClick = { },
                            label = { Text(stringResource(R.string.donate_pix_copied)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Botão abrir app do banco
                    Button(
                        onClick = {
                            // Tenta abrir o app do banco com o payload PIX
                            openPixApp(context, pixPayload)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.donate_pix_button))
                    }
                }
            }
            
            // ========== OUTROS MÉTODOS ==========
            Text(
                text = stringResource(R.string.donate_other_methods),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            // PayPal e GitHub Sponsors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PayPal
                OutlinedButton(
                    onClick = { 
                        openPayPal(context, "pereirasaymonsilva@gmail.com")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("💙 ", style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.donate_paypal))
                }
                
                // GitHub Sponsors
                OutlinedButton(
                    onClick = { 
                        openUrl(context, "https://github.com/sponsors/pereirasaymonsilva-a11y")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("🐙 ", style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.donate_github))
                }
            }
            
            // Mensagem de agradecimento
            Text(
                text = stringResource(R.string.donate_thanks),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
        }
    }
}

// Funções auxiliares
private fun openPixApp(context: Context, pixPayload: String) {
    try {
        // Tenta abrir diretamente no app do banco (método padrão PIX)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://pix.bcb.gov.br/pay/$pixPayload".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback: copiar payload ou abrir navegador
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("PIX Payload", pixPayload)
        clipboard.setPrimaryClip(clip)
        
        // Mostrar snackbar ou toast (opcional)
        android.widget.Toast.makeText(
            context,
            "Payload PIX copiado! Cole no seu app do banco.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

private fun openPayPal(context: Context, email: String) {
    val paypalUrl = "https://paypal.me/$email"
    val paypalUrlAlt = "https://www.paypal.com/paypalme/$email"
    
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = paypalUrl.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Fallback: abrir navegador com o email
        openUrl(context, "https://www.paypal.com/donate/?business=$email&no_recurring=0&currency_code=BRL")
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // Ignora se não houver navegador
    }
}