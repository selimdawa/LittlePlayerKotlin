package com.flatcode.littleplayer.Activity

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.flatcode.littleplayer.Fragment.AlbumFragment
import com.flatcode.littleplayer.Fragment.SongsFragment
import com.flatcode.littleplayer.Model.MusicFiles
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.Unit.DATA
import com.flatcode.littleplayer.databinding.ActivityMainBinding
import java.util.ArrayList

class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: ActivityMainBinding
    private val context: Context = this@MainActivity
    private val MY_SORT = "SortOrder"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        permission()
    }

    private fun permission() {
        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(applicationContext, permissionToRequest) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permissionToRequest), REQUEST_CODE_PERMISSION)
        } else {
            musicFiles = getAllAudio(context)
            initViewPager()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                musicFiles = getAllAudio(context)
                initViewPager()
            } else {
                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }
                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permissionToRequest), REQUEST_CODE_PERMISSION)
            }
        }
    }

    private fun initViewPager() {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        viewPagerAdapter.addFragments(SongsFragment(), DATA.SONGS)
        viewPagerAdapter.addFragments(AlbumFragment(), DATA.ALBUMS)
        binding.viewPager.adapter = viewPagerAdapter
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        private val fragments = ArrayList<Fragment>()
        private val titles = ArrayList<String>()

        fun addFragments(fragment: Fragment, title: String) {
            fragments.add(fragment)
            titles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titles[position]
        }
    }

    fun getAllAudio(context: Context): ArrayList<MusicFiles> {
        val preferences = getSharedPreferences(MY_SORT, MODE_PRIVATE)
        val sortOrder = preferences.getString(DATA.SORTING, DATA.SORT_BY_NAME)
        val duplicate = ArrayList<String>()
        albums.clear()
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
            MediaStore.Audio.Media._ID
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

                val musicFile = MusicFiles(path, title, artist, album, duration, id)
                tempAudioList.add(musicFile)
                if (!duplicate.contains(album)) {
                    albums.add(musicFile)
                    duplicate.add(album)
                }
            }
        }
        return tempAudioList
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        val menuItem = menu?.findItem(R.id.search_option)
        val searchView = menuItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(s: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(s: String?): Boolean {
        val userInput = s?.lowercase() ?: ""
        val myFiles = ArrayList<MusicFiles>()
        val files = musicFiles ?: return false

        for (song in files) {
            if (song.title?.lowercase()?.contains(userInput) == true) {
                myFiles.add(song)
            }
        }
        SongsFragment.musicAdapter?.updateList(myFiles)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val editor = getSharedPreferences(MY_SORT, MODE_PRIVATE).edit()
        val itemId = item.itemId

        if (itemId == R.id.name) {
            editor.putString(DATA.SORTING, DATA.SORT_BY_NAME)
            editor.apply()
            this.recreate()
        } else if (itemId == R.id.date) {
            editor.putString(DATA.SORTING, DATA.SORT_BY_DATE)
            editor.apply()
            this.recreate()
        } else if (itemId == R.id.size) {
            editor.putString(DATA.SORTING, DATA.SORT_BY_SIZE)
            editor.apply()
            this.recreate()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        val preferences = getSharedPreferences(MUSIC_LAST_PLAYED, MODE_PRIVATE)
        val path = preferences.getString(MUSIC_FILE, null)
        val artist = preferences.getString(ARTIST_NAME, null)
        val songName = preferences.getString(SONG_NAME, null)
        if (path != null) {
            SHOW_MINI_PLAYER = true
            PATH_TO_FRAG = path
            ARTIST_TO_FRAG = artist
            SONG_NAME_TO_FRAG = songName
        } else {
            SHOW_MINI_PLAYER = false
            PATH_TO_FRAG = null
            ARTIST_TO_FRAG = null
            SONG_NAME_TO_FRAG = null
        }
    }

    companion object {
        const val REQUEST_CODE_PERMISSION = 1
        var musicFiles: ArrayList<MusicFiles>? = null
        var shuffleBoolean = false
        var repeatBoolean = false
        var albums = ArrayList<MusicFiles>()

        const val MUSIC_LAST_PLAYED = "LAST_PLAYED"
        const val MUSIC_FILE = "STORED_MUSIC"
        var SHOW_MINI_PLAYER = false
        var PATH_TO_FRAG: String? = null
        var ARTIST_TO_FRAG: String? = null
        var SONG_NAME_TO_FRAG: String? = null
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"
    }
}