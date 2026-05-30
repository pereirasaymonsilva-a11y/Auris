package com.goldensystem.auris.data.gdrive

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.goldensystem.auris.data.database.AlbumEntity
import com.goldensystem.auris.data.database.ArtistEntity
import com.goldensystem.auris.data.database.GDriveDao
import com.goldensystem.auris.data.database.GDriveFolderEntity
import com.goldensystem.auris.data.database.GDriveSongEntity
import com.goldensystem.auris.data.database.MusicDao
import com.goldensystem.auris.data.database.SongArtistCrossRef
import com.goldensystem.auris.data.database.SongEntity
import com.goldensystem.auris.data.database.SourceType
import com.goldensystem.auris.data.database.toSong
import com.goldensystem.auris.data.model.Song
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import kotlin.math.absoluteValue
import javax.inject.Inject
import javax.inject.Singleton

class AuthorizationRequiredException(val intent: Intent) : Exception("Authorization required")

@Singleton
class GDriveRepository @Inject constructor(
    private val api: GDriveApiService,
    private val dao: GDriveDao,
    private val musicDao: MusicDao,
    private val gdriveStreamProxy: Lazy<GDriveStreamProxy>,
    @ApplicationContext private val context: Context
) {
    data class BulkSyncResult(
        val folderCount: Int,
        val syncedSongCount: Int,
        val failedFolderCount: Int
    )

    data class DriveFolder(val id: String, val name: String)

    private companion object {
        const val GDRIVE_SONG_ID_OFFSET = 6_000_000_000_000L
        const val GDRIVE_ALBUM_ID_OFFSET = 7_000_000_000_000L
        const val GDRIVE_ARTIST_ID_OFFSET = 8_000_000_000_000L
        const val GDRIVE_PARENT_DIRECTORY = "/Cloud/GoogleDrive"
        const val GDRIVE_GENRE = "Google Drive"
    }

    // SharedPreferences criptografada apenas para dados do usuário (email, displayName, avatar)
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
        Timber.e(e, "GDriveRepository: Failed to create EncryptedSharedPreferences, falling back to plain")
        context.getSharedPreferences("gdrive_prefs_plain", Context.MODE_PRIVATE)
    }

    private val _isLoggedInFlow = MutableStateFlow(false)
    val isLoggedInFlow: StateFlow<Boolean> = _isLoggedInFlow.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Armazena o email da conta logada (usado para obter tokens frescos)
    private var accountEmail: String? = null

    init {
        restoreSessionFromStorage()
        scope.launch {
            try {
                val valid = ensureValidToken()
                _isLoggedInFlow.value = valid && api.hasToken()
            } catch (e: Exception) {
                Timber.e(e, "Startup token validation failed")
                _isLoggedInFlow.value = false
            }
        }
    }

    val isLoggedIn: Boolean get() = api.hasToken()
    val userEmail: String? get() = prefs.getString("gdrive_email", null)
    val userDisplayName: String? get() = prefs.getString("gdrive_display_name", null)
    val userAvatar: String? get() = prefs.getString("gdrive_avatar", null)

    // Restaura apenas dados do usuário (email) - NUNCA salvamos token em disco
    fun restoreSessionFromStorage() {
        accountEmail = prefs.getString("gdrive_email", null)
        if (!accountEmail.isNullOrBlank()) {
            _isLoggedInFlow.value = true
            // Não setamos token na API agora; será obtido sob demanda via ensureValidToken()
        } else {
            api.clearAccessToken()
            _isLoggedInFlow.value = false
        }
    }

    /**
     * Obtém um token de acesso fresco do GoogleAuthUtil usando o email da conta.
     * Esse método é chamado automaticamente antes de qualquer operação que precise do token.
     */
    suspend fun ensureValidToken(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val email = accountEmail
                if (email.isNullOrBlank()) {
                    Timber.d("ensureValidToken: no account email")
                    return@withContext false
                }

                val scope = "oauth2:${GDriveConstants.SCOPE_DRIVE_READONLY}"
                val freshToken = GoogleAuthUtil.getToken(context, email, scope)
                if (freshToken.isNotBlank()) {
                    api.setAccessToken(freshToken)
                    Timber.d("ensureValidToken: obtained fresh token")
                    true
                } else {
                    Timber.d("ensureValidToken: token is blank")
                    false
                }
            } catch (e: UserRecoverableAuthException) {
                // O usuário precisa interagir (ex: conceder permissão novamente)
                Timber.e(e, "UserRecoverableAuthException - need to launch intent")
                // A UI deve capturar isso e mostrar a intent de autorização
                false
            } catch (e: Exception) {
                Timber.e(e, "Failed to get fresh token")
                false
            }
        }
    }

    /**
     * Login usando o GoogleAuthUtil. Ignoramos serverAuthCode e idToken, usamos apenas o email.
     */
    suspend fun loginWithCredential(
        idToken: String,
        serverAuthCode: String?,
        email: String?,
        displayName: String?,
        profilePictureUri: String?
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (email.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Email não recebido do Google"))
                }

                val scope = "oauth2:${GDriveConstants.SCOPE_DRIVE_READONLY}"
                val accessToken = GoogleAuthUtil.getToken(context, email, scope)

                // Salva apenas dados do usuário, NUNCA o token
                prefs.edit()
                    .putString("gdrive_email", email)
                    .putString("gdrive_display_name", displayName)
                    .putString("gdrive_avatar", profilePictureUri)
                    .apply()

                accountEmail = email
                api.setAccessToken(accessToken)
                _isLoggedInFlow.value = true

                // Inicia o proxy de streaming
                gdriveStreamProxy.get().start()

                Result.success(displayName ?: email)
            } catch (e: UserRecoverableAuthException) {
                // Lança uma exceção especial contendo o Intent para que a Activity possa tratar
                throw AuthorizationRequiredException(e.intent ?: Intent())
            } catch (e: Exception) {
                Timber.e(e, "Falha no login GDrive")
                Result.failure(e)
            }
        }
    }

    /**
     * Força a renovação do token via GoogleAuthUtil.
     */
    suspend fun refreshAccessToken(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val email = accountEmail ?: return@withContext Result.failure(Exception("No account email"))
                val scope = "oauth2:${GDriveConstants.SCOPE_DRIVE_READONLY}"
                val newToken = GoogleAuthUtil.getToken(context, email, scope)
                if (newToken.isNotBlank()) {
                    api.setAccessToken(newToken)
                    Result.success(newToken)
                } else {
                    Result.failure(Exception("Empty token from GoogleAuthUtil"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        api.clearAccessToken()
        prefs.edit().clear().apply()
        musicDao.clearAllGDriveSongs()
        dao.clearAllSongs()
        dao.clearAllFolders()
        accountEmail = null
        _isLoggedInFlow.value = false
        gdriveStreamProxy.get().stop()
    }

    // --- Folder Management ---

    fun getFolders(): Flow<List<GDriveFolderEntity>> = dao.getAllFolders()

    suspend fun listDriveFolders(parentId: String = "root"): Result<List<DriveFolder>> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ensureValidToken()) return@withContext Result.failure(Exception("Failed to obtain valid token"))
                val allFolders = mutableListOf<DriveFolder>()
                var pageToken: String? = null

                do {
                    val raw = api.listFolders(parentId, pageToken)
                    val root = JSONObject(raw)
                    val files = root.optJSONArray("files")

                    if (files != null) {
                        for (i in 0 until files.length()) {
                            val file = files.optJSONObject(i) ?: continue
                            allFolders.add(
                                DriveFolder(
                                    id = file.optString("id"),
                                    name = file.optString("name")
                                )
                            )
                        }
                    }
                    pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
                } while (pageToken != null)

                Result.success(allFolders)
            } catch (e: Exception) {
                Timber.e(e, "Failed to list Drive folders")
                Result.failure(e)
            }
        }
    }

    suspend fun createMusicFolder(parentId: String = "root"): Result<DriveFolder> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ensureValidToken()) return@withContext Result.failure(Exception("Failed to obtain valid token"))
                val raw = api.createFolder("Auris Music", parentId)
                val json = JSONObject(raw)
                val folder = DriveFolder(
                    id = json.optString("id"),
                    name = json.optString("name")
                )
                Result.success(folder)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create music folder")
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

    suspend fun removeFolder(folderId: String) {
        dao.deleteSongsByFolder(folderId)
        dao.deleteFolder(folderId)
        syncUnifiedLibrarySongsFromGDrive()
    }

    // --- Sync ---

    suspend fun syncFolderSongs(folderId: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                if (!ensureValidToken()) return@withContext Result.failure(Exception("Failed to obtain valid token"))
                val allEntities = mutableListOf<GDriveSongEntity>()
                var pageToken: String? = null

                do {
                    val raw = api.listAudioFiles(folderId, pageToken)
                    val root = JSONObject(raw)
                    val files = root.optJSONArray("files")

                    if (files != null) {
                        for (i in 0 until files.length()) {
                            val file = files.optJSONObject(i) ?: continue
                            allEntities.add(parseFileToEntity(file, folderId))
                        }
                    }
                    pageToken = root.optString("nextPageToken").takeIf { it.isNotBlank() }
                } while (pageToken != null)

                dao.deleteSongsByFolder(folderId)
                dao.insertSongs(allEntities)

                dao.insertFolder(
                    GDriveFolderEntity(
                        id = folderId,
                        name = dao.getAllFoldersList().find { it.id == folderId }?.name ?: "Drive Folder",
                        songCount = allEntities.size,
                        lastSyncTime = System.currentTimeMillis()
                    )
                )

                syncUnifiedLibrarySongsFromGDrive()

                Timber.d("Synced ${allEntities.size} songs for folder $folderId")
                Result.success(allEntities.size)
            } catch (e: Exception) {
                Timber.e(e, "Failed to sync folder $folderId")
                Result.failure(e)
            }
        }
    }

    suspend fun syncAllFoldersAndSongs(): Result<BulkSyncResult> {
        return withContext(Dispatchers.IO) {
            val folders = dao.getAllFoldersList()
            if (folders.isEmpty()) {
                return@withContext Result.success(BulkSyncResult(0, 0, 0))
            }

            var totalSongs = 0
            var failedCount = 0

            folders.forEach { folder ->
                val result = syncFolderSongs(folder.id)
                result.fold(
                    onSuccess = { count -> totalSongs += count },
                    onFailure = { failedCount++ }
                )
            }

            Result.success(BulkSyncResult(folders.size, totalSongs, failedCount))
        }
    }

    // --- Songs ---

    fun getAllSongs(): Flow<List<Song>> = dao.getAllGDriveSongs().map { list -> list.map { it.toSong() } }

    fun getFolderSongs(folderId: String): Flow<List<Song>> =
        dao.getSongsByFolder(folderId).map { list -> list.map { it.toSong() } }

    // --- Streaming ---

    fun getStreamUrl(fileId: String): String = api.getStreamUrl(fileId)
    
    /**
     * Retorna o header de autorização, mas antes garante que o token está válido.
     * Chamado pelo proxy de streaming.
     */
    suspend fun getAuthHeader(): String {
        ensureValidToken()
        return api.getAuthHeader()
    }

    // --- Unified Library Sync ---

    private suspend fun syncUnifiedLibrarySongsFromGDrive() {
        val gdriveSongs = dao.getAllGDriveSongsList()
        val existingUnifiedGDriveIds = musicDao.getAllGDriveSongIds()

        if (gdriveSongs.isEmpty()) {
            if (existingUnifiedGDriveIds.isNotEmpty()) {
                musicDao.clearAllGDriveSongs()
            }
            return
        }

        val songs = ArrayList<SongEntity>(gdriveSongs.size)
        val artists = LinkedHashMap<Long, ArtistEntity>()
        val albums = LinkedHashMap<Long, AlbumEntity>()
        val crossRefs = mutableListOf<SongArtistCrossRef>()

        gdriveSongs.forEach { gdriveSong ->
            val songId = toUnifiedSongId(gdriveSong.driveFileId)
            val artistNames = parseArtistNames(gdriveSong.artist)
            val primaryArtistName = artistNames.firstOrNull() ?: "Unknown Artist"
            val primaryArtistId = toUnifiedArtistId(primaryArtistName)

            artistNames.forEachIndexed { index, artistName ->
                val artistId = toUnifiedArtistId(artistName)
                artists.putIfAbsent(
                    artistId,
                    ArtistEntity(
                        id = artistId,
                        name = artistName,
                        trackCount = 0,
                        imageUrl = null
                    )
                )
                crossRefs.add(
                    SongArtistCrossRef(
                        songId = songId,
                        artistId = artistId,
                        isPrimary = index == 0
                    )
                )
            }

            val albumId = toUnifiedAlbumId(gdriveSong.album)
            val albumName = gdriveSong.album.ifBlank { "Unknown Album" }
            albums.putIfAbsent(
                albumId,
                AlbumEntity(
                    id = albumId,
                    title = albumName,
                    artistName = primaryArtistName,
                    artistId = primaryArtistId,
                    songCount = 0,
                    dateAdded = gdriveSong.dateAdded,
                    year = 0,
                    albumArtUriString = gdriveSong.albumArtUrl
                )
            )

            songs.add(
                SongEntity(
                    id = songId,
                    title = gdriveSong.title,
                    artistName = gdriveSong.artist.ifBlank { primaryArtistName },
                    artistId = primaryArtistId,
                    albumArtist = null,
                    albumName = albumName,
                    albumId = albumId,
                    contentUriString = "gdrive://${gdriveSong.driveFileId}",
                    albumArtUriString = gdriveSong.albumArtUrl,
                    duration = gdriveSong.duration,
                    genre = GDRIVE_GENRE,
                    filePath = "",
                    parentDirectoryPath = GDRIVE_PARENT_DIRECTORY,
                    isFavorite = false,
                    lyrics = null,
                    trackNumber = 0,
                    year = 0,
                    dateAdded = gdriveSong.dateAdded.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    mimeType = gdriveSong.mimeType,
                    bitrate = gdriveSong.bitrate,
                    sampleRate = null,
                    telegramChatId = null,
                    telegramFileId = null,
                    sourceType = SourceType.GDRIVE
                )
            )
        }

        val albumCounts = songs.groupingBy { it.albumId }.eachCount()
        val finalAlbums = albums.values.map { album ->
            album.copy(songCount = albumCounts[album.id] ?: 0)
        }

        val currentUnifiedSongIds = songs.map { it.id }.toSet()
        val deletedUnifiedSongIds = existingUnifiedGDriveIds.filter { it !in currentUnifiedSongIds }

        musicDao.incrementalSyncMusicData(
            songs = songs,
            albums = finalAlbums,
            artists = artists.values.toList(),
            crossRefs = crossRefs,
            deletedSongIds = deletedUnifiedSongIds
        )
    }

    // --- Parsing Helpers ---

    private fun parseFileToEntity(file: JSONObject, folderId: String): GDriveSongEntity {
        val fileId = file.optString("id")
        val fileName = file.optString("name", "Unknown")
        val mimeType = file.optString("mimeType", "audio/mpeg")
        val fileSize = file.optLong("size", 0L)
        val modifiedTime = file.optString("modifiedTime", "")
        val thumbnailLink =
            file.optString("thumbnailLink")
                .ifBlank { file.optString("iconLink") }
                .takeIf { it.isNotBlank() }

        val nameWithoutExt = fileName.substringBeforeLast(".")
        val parts = nameWithoutExt.split(" - ", limit = 2)

        val (artist, title) = if (parts.size == 2) {
            parts[0].trim() to parts[1].trim()
        } else {
            "Unknown Artist" to nameWithoutExt.trim()
        }

        val dateModified = try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(modifiedTime)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        return GDriveSongEntity(
            id = "${folderId}_${fileId}",
            driveFileId = fileId,
            folderId = folderId,
            title = title,
            artist = artist,
            album = "Google Drive",
            albumId = -1L,
            duration = 0L,
            albumArtUrl = thumbnailLink,
            mimeType = mimeType,
            bitrate = null,
            fileSize = fileSize,
            dateAdded = dateModified,
            dateModified = dateModified
        )
    }

    private fun parseArtistNames(rawArtist: String): List<String> {
        if (rawArtist.isBlank()) return listOf("Unknown Artist")
        val parsed = rawArtist.split(Regex("\\s*[,/&;+]\\s*"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        return if (parsed.isEmpty()) listOf("Unknown Artist") else parsed
    }

    private fun toUnifiedSongId(driveFileId: String): Long {
        return -(GDRIVE_SONG_ID_OFFSET + driveFileId.hashCode().toLong().absoluteValue)
    }

    private fun toUnifiedAlbumId(albumName: String): Long {
        val normalized = albumName.lowercase().hashCode().toLong().absoluteValue
        return -(GDRIVE_ALBUM_ID_OFFSET + normalized)
    }

    private fun toUnifiedArtistId(artistName: String): Long {
        return -(GDRIVE_ARTIST_ID_OFFSET + artistName.lowercase().hashCode().toLong().absoluteValue)
    }
}