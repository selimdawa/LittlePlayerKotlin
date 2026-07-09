package com.flatcode.littleplayer.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityMainBinding
import com.flatcode.littleplayer.fragment.AlbumsFragment
import com.flatcode.littleplayer.fragment.ArtistsFragment
import com.flatcode.littleplayer.fragment.FoldersFragment
import com.flatcode.littleplayer.fragment.SongsFragment
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.MusicPreferences
import com.flatcode.littleplayer.utils.dataStore
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MusicViewModel by viewModels()
    private val themeKey = stringPreferencesKey("color_option")
    private var initialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            dataStore.data.map { it[themeKey] ?: "ONE" }.collectLatest {
                if (initialized) binding.root.post { recreate() } else initialized = true
            }
        }

        binding.toolbar.settings.setOnClickListener {
            val entries = resources.getStringArray(R.array.reply_entries)
            val values = resources.getStringArray(R.array.reply_values)
            AlertDialog.Builder(this)
                .setTitle("Select Theme")
                .setItems(entries) { _, which ->
                    lifecycleScope.launch {
                        dataStore.edit { prefs -> prefs[themeKey] = values[which] }
                    }
                }.show()
        }
        permission()
    }

    private fun permission() {
        val perm =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), REQUEST_CODE_PERMISSION)
        } else {
            viewModel.loadAudioData()
            val navController =
                (supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment).navController
            val adapter = ViewPagerAdapter(this).apply {
                addFragment(SongsFragment(), DATA.SONGS)
                addFragment(AlbumsFragment(), DATA.ALBUMS)
                addFragment(ArtistsFragment(), DATA.ARTISTS)
                addFragment(FoldersFragment(), DATA.FOLDERS)
            }
            binding.viewPager.adapter = adapter
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
                tab.text = adapter.getPageTitle(pos)
            }.attach()
            binding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    navController.navigate(
                        when (pos) {
                            0 -> R.id.songsFragment; 1 -> R.id.albumsFragment; 2 -> R.id.artistsFragment; else -> R.id.foldersFragment
                        }
                    )
                }
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) permission() else permission()
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val path = prefs[MusicPreferences.MUSIC_FILE_KEY]
            SHOW_MINI_PLAYER = !path.isNullOrEmpty()
            PATH_TO_FRAG = path
            ARTIST_TO_FRAG = prefs[MusicPreferences.ARTIST_NAME_KEY]
            SONG_NAME_TO_FRAG = prefs[MusicPreferences.SONG_NAME_KEY]
        }
    }

    class ViewPagerAdapter(act: AppCompatActivity) : FragmentStateAdapter(act) {
        private val frags = ArrayList<Fragment>()
        private val titles = ArrayList<String>()
        fun addFragment(f: Fragment, t: String) {
            frags.add(f); titles.add(t)
        }

        override fun getItemCount() = frags.size
        override fun createFragment(pos: Int) = frags[pos]
        fun getPageTitle(pos: Int) = titles[pos]
    }

    companion object {
        const val REQUEST_CODE_PERMISSION = 1
        var SHOW_MINI_PLAYER = false
        var PATH_TO_FRAG: String? = null
        var ARTIST_TO_FRAG: String? = null
        var SONG_NAME_TO_FRAG: String? = null
        var shuffleBoolean = false
        var repeatBoolean = false
    }
}