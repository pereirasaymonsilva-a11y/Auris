package com.goldensystem.auris.data.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.goldensystem.auris.R

@Immutable
enum class LibraryTabId(
    val storageKey: String,
    @StringRes val titleRes: Int,
    val defaultSort: SortOption
) {
    SONGS("SONGS", R.string.songs, SortOption.SongTitleAZ),
    ALBUMS("ALBUMS", R.string.albums, SortOption.AlbumTitleAZ),
    ARTISTS("ARTIST", R.string.artists, SortOption.ArtistNameAZ),
    PLAYLISTS("PLAYLISTS", R.string.playlists, SortOption.PlaylistNameAZ),
    FOLDERS("FOLDERS", R.string.folders, SortOption.FolderNameAZ),
    LIKED("LIKED", R.string.liked, SortOption.LikedSongDateLiked);

    companion object {
        fun fromStorageKey(key: String): LibraryTabId =
            entries.firstOrNull { it.storageKey == key } ?: SONGS
    }
}

fun String.toLibraryTabIdOrNull(): LibraryTabId? =
    LibraryTabId.entries.firstOrNull { it.storageKey == this }