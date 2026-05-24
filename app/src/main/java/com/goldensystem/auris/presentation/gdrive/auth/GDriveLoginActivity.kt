package com.goldensystem.auris.presentation.gdrive.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.goldensystem.auris.data.gdrive.GDriveConstants
import com.goldensystem.auris.ui.theme.GoogleSansRounded
import com.goldensystem.auris.ui.theme.PixelPlayTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.goldensystem.auris.R

@AndroidEntryPoint
class GDriveLoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelPlayTheme {
                GDriveLoginScreen(onClose = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GDriveLoginScreen(
    viewModel: GDriveLoginViewModel = hiltViewModel(),
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val loginState by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 20.dp, cornerRadiusTL = 20.dp,
        cornerRadiusBR = 20.dp, cornerRadiusBL = 20.dp,
        smoothnessAsPercentTR = 60, smoothnessAsPercentTL = 60,
        smoothnessAsPercentBR = 60, smoothnessAsPercentBL = 60
    )

    LaunchedEffect(loginState) {
    // Pega o valor atual do StateFlow
    val state = loginState
    when (state) {
        is GDriveLoginState.Success -> {
            Toast.makeText(context, context.getString(R.string.auth_gdrive_toast_connected), Toast.LENGTH_SHORT).show()
            onClose()
        }
        is GDriveLoginState.Error -> {
            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
        }
        is GDriveLoginState.NeedAuthorization -> {
            try {
                // Inicia a Intent de autorização do Google
                context.startActivity(state.intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Precisa autorizar novamente. Faça login.", Toast.LENGTH_SHORT).show()
            }
        }
        else -> { /* Outros estados não precisam de ação */ }
    }
}

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.auth_gdrive_title), fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (loginState is GDriveLoginState.FolderSetup) {
                            if (!viewModel.navigateBack()) onClose()
                        } else onClose()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.auth_cd_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = loginState) {
                is GDriveLoginState.Idle, is GDriveLoginState.Error -> {
                    SignInContent(
                        cardShape = cardShape,
                        isError = state is GDriveLoginState.Error,
                        errorMessage = (state as? GDriveLoginState.Error)?.message,
                        onSignIn = { scope.launch { signInWithGoogle(context, viewModel) } }
                    )
                }
                is GDriveLoginState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.auth_gdrive_setting_up), fontFamily = GoogleSansRounded, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is GDriveLoginState.LoggedIn -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
                is GDriveLoginState.FolderSetup -> {
                    FolderSetupContent(
                        state = state,
                        cardShape = cardShape,
                        onFolderClick = { viewModel.navigateIntoFolder(it) },
                        onSelectFolder = { viewModel.selectFolder(it.id, it.name) },
                        onCreateFolder = { viewModel.createMusicFolder() },
                        onBreadcrumbClick = { viewModel.navigateToBreadcrumb(it) }
                    )
                }
                is GDriveLoginState.Success -> {}
                is GDriveLoginState.NeedAuthorization -> {
        // Pode mostrar uma mensagem enquanto tenta autorizar, ou apenas um loading
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Aguardando autorização...")}
                }
            }
        }
    }
}

@Composable
private fun SignInContent(cardShape: AbsoluteSmoothCornerShape, isError: Boolean, errorMessage: String?, onSignIn: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CloudQueue, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.secondary)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.auth_gdrive_connect_title), style = MaterialTheme.typography.headlineSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.auth_gdrive_connect_subtitle), style = MaterialTheme.typography.bodyMedium, fontFamily = GoogleSansRounded, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onSignIn, shape = cardShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary), modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.auth_gdrive_sign_in_with_google), fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        }
        if (isError && errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(text = errorMessage, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = GoogleSansRounded, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun FolderSetupContent(state: GDriveLoginState.FolderSetup, cardShape: AbsoluteSmoothCornerShape, onFolderClick: (FolderItem) -> Unit, onSelectFolder: (FolderItem) -> Unit, onCreateFolder: () -> Unit, onBreadcrumbClick: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(stringResource(R.string.auth_gdrive_select_music_folder_title), style = MaterialTheme.typography.titleMedium, fontFamily = GoogleSansRounded, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Text(stringResource(R.string.auth_gdrive_select_music_folder_subtitle), style = MaterialTheme.typography.bodySmall, fontFamily = GoogleSansRounded, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(12.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            itemsIndexed(state.currentPath) { index, crumb ->
                if (index > 0) Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { onBreadcrumbClick(index) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = crumb.name, style = MaterialTheme.typography.bodyMedium, fontFamily = GoogleSansRounded, fontWeight = if (index == state.currentPath.lastIndex) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(enabled = !state.isLoading, onClick = onCreateFolder), shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.auth_gdrive_create_pixelplay_music_title), style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(stringResource(R.string.auth_gdrive_create_folder_here_hint), style = MaterialTheme.typography.bodySmall, fontFamily = GoogleSansRounded, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.secondary)
            }
        } else if (state.folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.auth_gdrive_no_folders_here), style = MaterialTheme.typography.bodyLarge, fontFamily = GoogleSansRounded, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(state.folders, key = { it.id }) { folder ->
                    FolderCard(folder, cardShape, { onFolderClick(folder) }, { onSelectFolder(folder) })
                }
            }
        }
    }
}

@Composable
private fun FolderCard(folder: FolderItem, cardShape: AbsoluteSmoothCornerShape, onNavigate: () -> Unit, onSelect: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigate), shape = cardShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(12.dp))
            Text(text = folder.name, style = MaterialTheme.typography.titleSmall, fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = onSelect, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), contentColor = MaterialTheme.colorScheme.secondary)) {
                Text(stringResource(R.string.auth_gdrive_use_this_folder), fontFamily = GoogleSansRounded, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.auth_gdrive_cd_open_folder), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }
    }
}

private suspend fun signInWithGoogle(context: android.content.Context, viewModel: GDriveLoginViewModel) {
    try {
        val credentialManager = CredentialManager.create(context)
        
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(GDriveConstants.WEB_CLIENT_ID)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(null)
            .build()
        
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        
        // FORMATO MODERNO - com parâmetros nomeados
        val result = credentialManager.getCredential(
            context = context,
            request = request
        )
        
        val credential = result.credential
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleIdTokenCredential.idToken
        val serverAuthCode = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_SERVER_AUTH_CODE")
        
        // ✅ NOVO: pegar o email direto do googleIdTokenCredential.id
        val email = googleIdTokenCredential.id  // isso já é o email!
        
        viewModel.processCredential(idToken, serverAuthCode, email)
    } catch (e: GetCredentialException) {
        viewModel.processCredential("", null, null)
    } catch (e: Exception) {
        viewModel.processCredential("", null, null)
    }
}