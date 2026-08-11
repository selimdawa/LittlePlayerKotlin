package com.flatcode.littleplayer.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils.TruncateAt
import android.view.Gravity
import android.view.ViewGroup.MarginLayoutParams
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
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityMainBinding
import com.flatcode.littleplayer.databinding.ItemTabBinding
import com.flatcode.littleplayer.fragment.AlbumsFragment
import com.flatcode.littleplayer.fragment.ArtistsFragment
import com.flatcode.littleplayer.fragment.FoldersFragment
import com.flatcode.littleplayer.fragment.SongsFragment
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.loadSongImageByPath
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@UnstableApi
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
        startActivity(intent)
    }

    private fun setupSearchSwitcher() {
        binding.toolbar.searchTextSwitcher.setFactory {
            TextView(this).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                textSize = 14f
                maxLines = 1
                ellipsize = TruncateAt.END
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gray))
            }
        }

        binding.toolbar.searchTextSwitcher.inAnimation =
            AnimationUtils.loadAnimation(this, R.anim.slide_in_up)
        binding.toolbar.searchTextSwitcher.outAnimation =
            AnimationUtils.loadAnimation(this, R.anim.slide_out_up)
        binding.toolbar.searchTextSwitcher.setText(getString(R.string.search))

        lifecycleScope.launch {
            viewModel.filteredMusicFiles.collectLatest { songs ->
                if (songs.isNotEmpty()) {
                    var index = 0
                    while (true) {
                        val title = songs[index].title ?: getString(R.string.search)
                        binding.toolbar.searchTextSwitcher.setText(title)
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
        viewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) {
                openPlayer(event.position)
            }
        }

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
            addFragment(getString(R.string.songs)) { SongsFragment() }
            addFragment(getString(R.string.albums)) { AlbumsFragment() }
            addFragment(getString(R.string.artists)) { ArtistsFragment() }
            addFragment(getString(R.string.folders)) { FoldersFragment() }
        }
        binding.viewPager.adapter = adapter

        val marginSmall = resources.getDimensionPixelSize(R.dimen.tab_margin_small)
        binding.tabLayout.setPadding(marginSmall, 0, marginSmall, 0)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            val tabBinding = ItemTabBinding.inflate(layoutInflater, binding.tabLayout, false)
            tabBinding.tabTitle.text = adapter.getPageTitle(pos)

            val params = tabBinding.tabContainer.layoutParams as MarginLayoutParams
            params.setMargins(marginSmall, 0, marginSmall, 0)
            tabBinding.tabContainer.layoutParams = params

            tab.customView = tabBinding.root
        }.attach()
    }

    class ViewPagerAdapter(act: AppCompatActivity) : FragmentStateAdapter(act) {
        private val titles = ArrayList<String>()
        private val fragments = ArrayList<() -> Fragment>()

        fun addFragment(title: String, fragmentCreator: () -> Fragment) {
            titles.add(title)
            fragments.add(fragmentCreator)
        }

        override fun getItemCount() = fragments.size
        override fun createFragment(pos: Int) = fragments[pos]()
        fun getPageTitle(pos: Int) = titles[pos]
    }
}