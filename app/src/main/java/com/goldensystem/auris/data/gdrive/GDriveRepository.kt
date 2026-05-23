package com.goldensystem.auris.data.gdrive

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.GoogleAuthUtil
import com.goldensystem.auris.data.database.*
import com.goldensystem.auris.data.model.Song
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class GDriveRepository @Inject constructor(
    private val api: GDriveApiService,
    private val dao: GDriveDao,
    private val musicDao: MusicDao,
    private val gdriveStreamProxy: Lazy<GDriveStreamProxy>,
    @ApplicationContext private val context: Context
) {

    data class DriveFolder(val id: String, val name: String)

    data class BulkSyncResult(
        val folderCount: Int,
        val syncedSongCount: Int,
        val failedFolderCount: Int
    )

    private companion object {
        const val GDRIVE_SONG_ID_OFFSET = 6_000_000_000_000L
        const val GDRIVE_ALBUM_ID_OFFSET = 7_000_000_000_000L
        const val GDRIVE_ARTIST_ID_OFFSET = 8_000_000_000_000L
        const val GDRIVE_PARENT_DIRECTORY = "/Cloud/GoogleDrive"
        const val GDRIVE_GENRE = "Google Drive"
    }

    // ----------------------------
    // PREFS (SESSION STORAGE)
    // ----------------------------

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "gdrive_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Timber.e(e, "Encrypted prefs failed, fallback")
        context.getSharedPreferences("gdrive_prefs_plain", Context.MODE_PRIVATE)
    }

    // ----------------------------
    // AUTH STATE (SINGLE SOURCE)
    // ----------------------------

    private val _isLoggedInFlow = MutableStateFlow(false)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    val isLoggedIn: Boolean
        get() = api.hasToken()

    val userEmail: String?
        get() = prefs.getString("gdrive_email", null)

    val userDisplayName: String?
        get() = prefs.getString("gdrive_display_name", null)

    val userAvatar: String?
        get() = prefs.getString("gdrive_avatar", null)

    // ----------------------------
    // INIT
    // ----------------------------

    init {
        restoreSessionFromStorage()
        Timber.d("GDriveRepository initialized")
    }

    // ----------------------------
    // SESSION RESTORE (FIX REAL)
    // ----------------------------

    fun restoreSessionFromStorage() {
        val token = prefs.getString("gdrive_access_token", null)

        if (!token.isNullOrBlank()) {
            api.setAccessToken(token)

            val valid = api.hasToken()
            _isLoggedInFlow.value = valid

            Timber.d("GDrive session restored valid=$valid")
        } else {
            api.clearAccessToken()
            _isLoggedInFlow.value = false
        }
    }

    // ----------------------------
    // LOGIN (FIXED + STABLE)
    // ----------------------------

    suspend fun loginWithCredential(
        idToken: String,
        serverAuthCode: String?,
        email: String? = null,
        displayName: String? = null,
        profilePictureUri: String? = null
    ): Result<String> {

        return withContext(Dispatchers.IO) {
            try {

                val accountEmail = email
                    ?: return@withContext Result.failure(Exception("Email não recebido"))

                val scope = "oauth2:${GDriveConstants.SCOPE_DRIVE_READONLY}"

                val accessToken = GoogleAuthUtil.getToken(
                    context,
                    accountEmail,
                    scope
                )

                if (accessToken.isBlank()) {
                    return@withContext Result.failure(Exception("Token vazio"))
                }

                // ----------------------------
                // SAVE SESSION
                // ----------------------------

                prefs.edit()
                    .putString("gdrive_access_token", accessToken)
                    .putString("gdrive_email", email)
                    .putString("gdrive_display_name", displayName)
                    .putString("gdrive_avatar", profilePictureUri)
                    .putLong(
                        "gdrive_token_expires_at",
                        System.currentTimeMillis() + 3600_000L
                    )
                    .apply()

                // ----------------------------
                // APPLY TO API
                // ----------------------------

                api.setAccessToken(accessToken)

                // IMPORTANT: REAL STATE
                _isLoggedInFlow.value = api.hasToken()

                // START STREAM SYSTEM
                gdriveStreamProxy.get().start()

                return@withContext Result.success(
                    displayName ?: email ?: "Usuário"
                )

            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                return@withContext Result.failure(e)
            }
        }
    }

    // ----------------------------
    // LOGOUT
    // ----------------------------

    suspend fun logout() {
        api.clearAccessToken()
        prefs.edit().clear().apply()
        musicDao.clearAllGDriveSongs()
        dao.clearAllFolders()
        _isLoggedInFlow.value = false
    }

    // ----------------------------
    // TOKEN VALIDATION
    // ----------------------------

    private suspend fun ensureValidToken() {
        val expiresAt = prefs.getLong("gdrive_token_expires_at", 0L)

        if (System.currentTimeMillis() > expiresAt - 300_000L) {
            refreshAccessToken()
        }
    }

    // ----------------------------
    // REFRESH TOKEN
    // ----------------------------

    suspend fun refreshAccessToken(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {

                val refreshToken = prefs.getString("gdrive_refresh_token", null)
                    ?: return@withContext Result.failure(Exception("No refresh token"))

                val response = api.refreshToken(
                    refreshToken,
                    GDriveConstants.WEB_CLIENT_ID,
                    ""
                )

                val json = JSONObject(response)
                val accessToken = json.optString("access_token")

                if (accessToken.isBlank()) {
                    return@withContext Result.failure(Exception("Refresh failed"))
                }

                prefs.edit()
                    .putString("gdrive_access_token", accessToken)
                    .apply()

                api.setAccessToken(accessToken)
                _isLoggedInFlow.value = true

                return@withContext Result.success(accessToken)

            } catch (e: Exception) {
                Timber.e(e, "Refresh error")
                return@withContext Result.failure(e)
            }
        }
    }

    // ----------------------------
    // FOLDERS
    // ----------------------------

    fun getFolders(): Flow<List<GDriveFolderEntity>> =
        dao.getAllFolders()

    suspend fun listDriveFolders(parentId: String = "root"): Result<List<DriveFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                ensureValidToken()

                val result = mutableListOf<DriveFolder>()
                var pageToken: String? = null

                do {
                    val raw = api.listFolders(parentId, pageToken)
                    val json = JSONObject(raw)
                    val files = json.optJSONArray("files")

                    if (files != null) {
                        for (i in 0 until files.length()) {
                            val f = files.optJSONObject(i) ?: continue
                            result.add(
                                DriveFolder(
                                    id = f.optString("id"),
                                    name = f.optString("name")
                                )
                            )
                        }
                    }

                    pageToken = json.optString("nextPageToken")
                        .takeIf { it.isNotBlank() }

                } while (pageToken != null)

                Result.success(result)

            } catch (e: Exception) {
                Timber.e(e, "listDriveFolders failed")
                Result.failure(e)
            }
        }
    }

    suspend fun createMusicFolder(parentId: String = "root"): Result<DriveFolder> {
        return withContext(Dispatchers.IO) {
            try {
                ensureValidToken()

                val raw = api.createFolder("PixelPlay Music", parentId)
                val json = JSONObject(raw)

                Result.success(
                    DriveFolder(
                        id = json.optString("id"),
                        name = json.optString("name")
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "createMusicFolder failed")
                Result.failure(e)
            }
        }
    }

    suspend fun addFolder(folderId: String, name: String) {
        dao.insertFolder(
            GDriveFolderEntity(
                id = folderId,
                name = name,
                songCount = 0,
                lastSyncTime = 0
            )
        )
    }

    // ----------------------------
    // STREAM
    // ----------------------------

    fun getStreamUrl(fileId: String): String =
        api.getStreamUrl(fileId)

    fun getAuthHeader(): String =
        api.getAuthHeader()
}