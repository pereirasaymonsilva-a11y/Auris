package com.goldensystem.auris.data.backup.format

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.goldensystem.auris.data.backup.model.BackupManifest
import com.goldensystem.auris.di.BackupGson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.Reader
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupReader @Inject constructor(
    @ApplicationContext private val context: Context,
    @BackupGson private val gson: Gson,
    private val formatDetector: BackupFormatDetector
) {
    companion object {
        const val MAX_MANIFEST_BYTES = 512 * 1024
        const val MAX_MODULE_PAYLOAD_BYTES = 16 * 1024 * 1024
    }

    suspend fun readManifest(uri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormatInternal(uri)

            when (format) {
                BackupFormatDetector.Format.GABK_V3_ZIP -> {
                    readManifestFromZip(uri)
                }
                BackupFormatDetector.Format.GABK_V2_GZIP -> {
                    val json = decompressGzip(uri)
                    gson.fromJson(json, BackupManifest::class.java)
                }
                BackupFormatDetector.Format.UNKNOWN -> {
                    throw IllegalArgumentException("Unrecognized backup file format. Expected .gabk file.")
                }
            }
        }
    }

    suspend fun readModulePayload(uri: Uri, moduleKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormatInternal(uri)

            when (format) {
                BackupFormatDetector.Format.GABK_V3_ZIP -> {
                    readEntryFromZip(uri, "$moduleKey.json", MAX_MODULE_PAYLOAD_BYTES)
                        ?: throw IllegalArgumentException("Module '$moduleKey' not found in backup")
                }
                BackupFormatDetector.Format.GABK_V2_GZIP -> {
                    val json = decompressGzip(uri)
                    // Parse JSON and extract module
                    extractModuleFromJson(json, moduleKey)
                }
                BackupFormatDetector.Format.UNKNOWN -> {
                    throw IllegalArgumentException("Unrecognized backup file format. Expected .gabk file.")
                }
            }
        }
    }

    suspend fun readAllModulePayloads(uri: Uri): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        runCatching {
            val format = detectFormatInternal(uri)

            when (format) {
                BackupFormatDetector.Format.GABK_V3_ZIP -> {
                    readAllEntriesFromZip(uri, MAX_MODULE_PAYLOAD_BYTES)
                }
                BackupFormatDetector.Format.GABK_V2_GZIP -> {
                    val json = decompressGzip(uri)
                    extractAllModulesFromJson(json)
                }
                BackupFormatDetector.Format.UNKNOWN -> {
                    throw IllegalArgumentException("Unrecognized backup file format. Expected .gabk file.")
                }
            }
        }
    }

    suspend fun detectFormat(uri: Uri): Result<BackupFormatDetector.Format> = withContext(Dispatchers.IO) {
        runCatching {
            detectFormatInternal(uri)
        }
    }

    private fun detectFormatInternal(uri: Uri): BackupFormatDetector.Format {
        return context.contentResolver.openInputStream(uri)?.use { input ->
            val header = formatDetector.readHeader(input)
            formatDetector.detect(header)
        } ?: throw IllegalStateException("Unable to open backup file")
    }

    private fun readManifestFromZip(uri: Uri): BackupManifest {
        val json = readEntryFromZip(
            uri = uri,
            entryName = BackupManifest.MANIFEST_FILENAME,
            maxChars = MAX_MANIFEST_BYTES
        ) ?: throw IllegalArgumentException("Manifest not found in backup archive")
        return gson.fromJson(json, BackupManifest::class.java)
    }

    private fun readEntryFromZip(uri: Uri, entryName: String, maxChars: Int): String? {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            skipFully(raw, BackupFormatDetector.GABK_MAGIC_SIZE)
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == entryName) {
                        return zip.bufferedReader(Charsets.UTF_8).use { reader ->
                            readTextLimited(reader, maxChars, "Backup entry '$entryName'")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return null
    }

    private fun readAllEntriesFromZip(uri: Uri, maxChars: Int): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { raw ->
            skipFully(raw, BackupFormatDetector.GABK_MAGIC_SIZE)
            ZipInputStream(raw).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (name != BackupManifest.MANIFEST_FILENAME && name.endsWith(".json")) {
                        val key = name.removeSuffix(".json")
                        entries[key] = zip.bufferedReader(Charsets.UTF_8).use { reader ->
                            readTextLimited(reader, maxChars, "Backup entry '$name'")
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IllegalStateException("Unable to open backup file")
        return entries
    }

    private fun decompressGzip(uri: Uri): String {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open backup file")

        return input.use { raw ->
            skipFully(raw, BackupFormatDetector.GABK_MAGIC_SIZE)
            GZIPInputStream(raw).use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    readTextLimited(reader, MAX_MANIFEST_BYTES * 2, "GZIP backup payload")
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractModuleFromJson(json: String, moduleKey: String): String {
        val map = gson.fromJson(json, Map::class.java) as Map<String, Any>
        val module = map[moduleKey]
            ?: throw IllegalArgumentException("Module '$moduleKey' not found in backup")
        return gson.toJson(module)
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractAllModulesFromJson(json: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val map = gson.fromJson(json, Map::class.java) as Map<String, Any>
        
        map.forEach { (key, value) ->
            if (key != "manifest") {
                result[key] = gson.toJson(value)
            }
        }
        return result
    }

    private fun readTextLimited(reader: Reader, maxChars: Int, sourceLabel: String): String {
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        val builder = StringBuilder(minOf(maxChars, DEFAULT_BUFFER_SIZE))
        var totalChars = 0

        while (true) {
            val read = reader.read(buffer)
            if (read == -1) break

            totalChars += read
            if (totalChars > maxChars) {
                throw IllegalArgumentException(
                    "$sourceLabel exceeds the ${maxChars / (1024 * 1024)}MB in-memory safety limit."
                )
            }

            builder.append(buffer, 0, read)
        }

        return builder.toString()
    }

    private fun skipFully(input: InputStream, byteCount: Int) {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = input.skip(remaining.toLong())
            if (skipped > 0) {
                remaining -= skipped.toInt()
                continue
            }

            if (input.read() == -1) {
                throw IllegalArgumentException("Backup file is truncated.")
            }
            remaining--
        }
    }
}
