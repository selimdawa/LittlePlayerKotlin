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
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityMainBinding
import com.flatcode.littleplayer.fragment.AlbumFragment
import com.flatcode.littleplayer.fragment.SongsFragment
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

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
                this,
                permissionToRequest
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(permissionToRequest),
                REQUEST_CODE_PERMISSION
            )
        } else {
            setupAppFlow()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
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
        initViewPager()
    }

    private fun initViewPager() {
        val viewPagerAdapter = ViewPagerAdapter(this)
        viewPagerAdapter.addFragment(SongsFragment(), DATA.SONGS)
        viewPagerAdapter.addFragment(AlbumFragment(), DATA.ALBUMS)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = viewPagerAdapter.getPageTitle(position)
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        val menuItem = menu?.findItem(R.id.search_option)
        val searchView = menuItem?.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onQueryTextSubmit(s: String?): Boolean = false

    override fun onQueryTextChange(s: String?): Boolean {
        viewModel.filterSongs(s ?: "")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.name -> viewModel.updateSortOrder(DATA.SORT_BY_NAME)
            R.id.date -> viewModel.updateSortOrder(DATA.SORT_BY_DATE)
            R.id.size -> viewModel.updateSortOrder(DATA.SORT_BY_SIZE)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
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