package com.flatcode.littleplayer.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.edit
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.unit.DATA

class MusicRepository(private val context: Context) {
    private val mySortPref = "SortOrder"

    fun getAllAudio(): ArrayList<MusicFiles> {
        val preferences = context.getSharedPreferences(mySortPref, Context.MODE_PRIVATE)
        val sortOrder = preferences.getString(DATA.SORTING, DATA.SORT_BY_NAME)
        val tempAudioList = ArrayList<MusicFiles>()

        val order = when (sortOrder) {
            "sortByName" -> MediaStore.MediaColumns.DISPLAY_NAME + " ASC"
            "sortByDate" -> MediaStore.MediaColumns.DATE_ADDED + " ASC"
            "sortBySize" -> MediaStore.MediaColumns.SIZE + " DESC"
            else -> null
        }

        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val cursor: Cursor? = context.contentResolver.query(uri, projection, null, null, order)
        cursor?.use {
            while (it.moveToNext()) {
                val album = it.getString(0) ?: "Unknown"
                val title = it.getString(1) ?: "Unknown"
                val duration = it.getString(2) ?: "0"
                val path = it.getString(3) ?: ""
                val artist = it.getString(4) ?: "Unknown"
                val id = it.getString(5) ?: ""

                tempAudioList.add(MusicFiles(path, title, artist, album, duration, id))
            }
        }
        return tempAudioList
    }

    fun saveSortOrder(sortType: String) {
        context.getSharedPreferences(mySortPref, Context.MODE_PRIVATE).edit {
            putString(DATA.SORTING, sortType)
        }
    }
}