package com.flatcode.littleplayer.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
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
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MusicViewModel by viewModels()

    private val MUSIC_FILE_KEY = stringPreferencesKey(MUSIC_FILE)
    private val ARTIST_NAME_KEY = stringPreferencesKey(ARTIST_NAME)
    private val SONG_NAME_KEY = stringPreferencesKey(SONG_NAME)

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

        if (ContextCompat.checkSelfPermission(
                this, permissionToRequest
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(permissionToRequest), REQUEST_CODE_PERMISSION
            )
        } else {
            setupAppFlow()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupAppFlow()
            } else {
                permission()
            }
        }
    }

    private fun setupAppFlow() {
        viewModel.loadAudioData()
        initNavigationWithViewPager()
    }

    private fun initNavigationWithViewPager() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController

        val viewPagerAdapter = ViewPagerAdapter(this)
        viewPagerAdapter.addFragment(SongsFragment(), DATA.SONGS)
        viewPagerAdapter.addFragment(AlbumsFragment(), DATA.ALBUMS)
        viewPagerAdapter.addFragment(ArtistsFragment(), DATA.ARTISTS)
        viewPagerAdapter.addFragment(FoldersFragment(), DATA.FOLDERS)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> navController.navigate(R.id.songsFragment)
                    1 -> navController.navigate(R.id.albumsFragment)
                    2 -> navController.navigate(R.id.artistsFragment)
                    3 -> navController.navigate(R.id.foldersFragment)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val preferences = dataStore.data.first()
            val path = preferences[MUSIC_FILE_KEY]

            SHOW_MINI_PLAYER = !path.isNullOrEmpty()
            PATH_TO_FRAG = path
            ARTIST_TO_FRAG = preferences[ARTIST_NAME_KEY]
            SONG_NAME_TO_FRAG = preferences[SONG_NAME_KEY]
        }
    }

    class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        private val fragments = ArrayList<Fragment>()
        private val titles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            titles.add(title)
        }

        override fun getItemCount(): Int = fragments.size
        override fun createFragment(position: Int): Fragment = fragments[position]
        fun getPageTitle(position: Int): String = titles[position]
    }

    companion object {
        const val REQUEST_CODE_PERMISSION = 1
        const val MUSIC_FILE = "STORED_MUSIC"
        const val ARTIST_NAME = "ARTIST NAME"
        const val SONG_NAME = "SONG NAME"

        var SHOW_MINI_PLAYER = false
        var PATH_TO_FRAG: String? = null
        var ARTIST_TO_FRAG: String? = null
        var SONG_NAME_TO_FRAG: String? = null
        var shuffleBoolean = false
        var repeatBoolean = false
    }
}