package com.flatcode.littleplayer.activity

import android.Manifest
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityMainBinding
import com.flatcode.littleplayer.fragment.AlbumsFragment
import com.flatcode.littleplayer.fragment.ArtistsFragment
import com.flatcode.littleplayer.fragment.FoldersFragment
import com.flatcode.littleplayer.fragment.SongsFragment
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.getLibraryColor
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageByPath
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MusicViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            setupUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        setupSearchSwitcher()

        binding.toolbar.searchContainer.setOnClickListener { startSearchActivity() }
        binding.toolbar.settings.setOnClickListener { launchActivity<SettingsActivity>() }
        binding.toolbar2.cardPlaylists.setOnClickListener { launchActivity<PlaylistsActivity>() }
        binding.toolbar2.cardFavourites.setOnClickListener { launchActivity<FavoritesActivity>() }
        binding.toolbar2.cardRecent.setOnClickListener { launchActivity<RecentActivity>() }

        checkPermissions()
        setupBackPressed()
    }

    private fun startSearchActivity() {
        val intent = Intent(this, SearchActivity::class.java)
        val options = ActivityOptions.makeCustomAnimation(this, R.anim.slide_in_up, R.anim.slide_out_up)
        startActivity(intent, options.toBundle())
    }

    private fun setupSearchSwitcher() {
        binding.toolbar.searchTextSwitcher.setFactory {
            TextView(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                textSize = 14f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(getLibraryColor("colorError"))
            }
        }

        binding.toolbar.searchTextSwitcher.inAnimation =
            AnimationUtils.loadAnimation(this, R.anim.slide_in_up)
        binding.toolbar.searchTextSwitcher.outAnimation =
            AnimationUtils.loadAnimation(this, R.anim.slide_out_up)
        binding.toolbar.searchTextSwitcher.setText(getString(R.string.search))

        lifecycleScope.launch {
            delay(5.seconds)
            viewModel.filteredMusicFiles.collect { songs ->
                if (songs.isNotEmpty()) {
                    var index = 0
                    while (true) {
                        binding.toolbar.searchTextSwitcher.setText(songs[index].title)
                        index = (index + 1) % songs.size
                        delay(5.seconds)
                    }
                }
            }
        }
    }

    private fun setupBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.tabLayout.selectedTabPosition != 0) {
                    binding.tabLayout.getTabAt(0)?.select()
                } else {
                    finish()
                }
            }
        })
    }

    private fun observeViewModel() {
        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
            song?.let {
                binding.toolbar2.ivRecent.loadSongImage(it.albumId, it.path, it.cachedImagePath)
            }
        }

        favoritesViewModel.favoriteSongs.collectWithLifecycle(this) { songs ->
            if (songs.isNotEmpty()) {
                val lastSong = songs.last()
                binding.toolbar2.ivFavourites.loadSongImageByPath(
                    lastSong.path, lastSong.cachedImagePath
                )
            }
        }
    }

    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            setupUI()
        }
    }

    private fun setupUI() {
        viewModel.loadAudioData()
        val adapter = ViewPagerAdapter(this).apply {
            addFragment(SongsFragment(), DATA.SONGS)
            addFragment(AlbumsFragment(), DATA.ALBUMS)
            addFragment(ArtistsFragment(), DATA.ARTISTS)
            addFragment(FoldersFragment(), DATA.FOLDERS)
        }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            val tabView = LayoutInflater.from(this).inflate(R.layout.item_tab, binding.tabLayout, false)
            val title = tabView.findViewById<TextView>(R.id.tabTitle)
            title.text = adapter.getPageTitle(pos)

            val container = tabView.findViewById<android.view.View>(R.id.tabContainer)
            val params = container.layoutParams as android.view.ViewGroup.MarginLayoutParams
            val density = resources.displayMetrics.density
            val margin10 = (10 * density).toInt()
            val margin5 = (5 * density).toInt()

            params.marginStart = if (pos == 0) margin10 else margin5
            params.marginEnd = if (pos == adapter.itemCount - 1) margin10 else margin5
            container.layoutParams = params

            tab.customView = tabView
        }.attach()
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
}