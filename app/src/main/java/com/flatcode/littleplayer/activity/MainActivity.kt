package com.flatcode.littleplayer.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.TIRAMISU
import android.text.TextUtils.TruncateAt
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
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
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.checkAudioPermissions
import com.flatcode.littleplayer.utils.collectWithLifecycle
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.utils.openPlayer
import com.flatcode.littleplayer.viewmodel.FavoritesViewModel
import com.flatcode.littleplayer.viewmodel.MusicEvent
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.flatcode.littleplayer.viewmodel.NowPlayerViewModel
import com.flatcode.littleplayer.viewmodel.RecentViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import io.selimdawa.multicolors.MultiColorManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@UnstableApi
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private val viewModel: MusicViewModel by viewModels()
    private val favoritesViewModel: FavoritesViewModel by viewModels()
    private val nowPlayerViewModel: NowPlayerViewModel by viewModels()
    private val recentViewModel: RecentViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (SDK_INT >= TIRAMISU) {
            permissions[READ_MEDIA_AUDIO] == true
        } else {
            permissions[READ_EXTERNAL_STORAGE] == true
        }
        if (audioGranted) setupUI()
    }

    override fun setupViews() {
        applyEdgeToEdge(topView = binding.toolbar.card)

        setupSearchSwitcher()
        setupBackPressed()

        binding.toolbar.searchContainer.setOnClickListener { launchActivity<SearchActivity>() }
        binding.toolbar.settings.setOnClickListener { launchActivity<SettingsActivity>() }
        binding.toolbar2.cardPlaylists.setOnClickListener { launchActivity<PlaylistsActivity>() }
        binding.toolbar2.cardFavourites.setOnClickListener { launchActivity<FavoritesActivity>() }
        binding.toolbar2.cardRecent.setOnClickListener { launchActivity<RecentActivity>() }

        checkAudioPermissions(permissionLauncher) { setupUI() }
    }

    private fun setupSearchSwitcher() {
        binding.toolbar.searchTextSwitcher.setFactory {
            TextView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.text_size_search_hint)
                )
                maxLines = 1
                ellipsize = TruncateAt.END
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gray))
            }
        }

        binding.toolbar.searchTextSwitcher.apply {
            inAnimation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_in_up)
            outAnimation = AnimationUtils.loadAnimation(this@MainActivity, R.anim.slide_out_up)
            setText(getString(R.string.search))
        }

        lifecycleScope.launch {
            viewModel.filteredMusicFiles.collectLatest { songs ->
                if (songs.isNotEmpty()) {
                    var index = 0
                    while (true) {
                        binding.toolbar.searchTextSwitcher.setText(
                            songs[index].title ?: getString(R.string.search)
                        )
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

    override fun observeViewModel() {
        viewModel.event.collectWithLifecycle(this) { event ->
            if (event is MusicEvent.PlaySong) openPlayer(event.position)
        }

        viewModel.isInitialLoading.collectWithLifecycle(this) { isLoading ->
            binding.loadingOverlay.isVisible = isLoading
        }

        nowPlayerViewModel.currentPlayingSong.collectWithLifecycle(this) { song ->
            binding.fragBottomPlayer.root.isVisible = song != null
        }

        recentViewModel.recentSongs.collectWithLifecycle(this) { songs ->
            val lastSong = songs.firstOrNull()
            if (lastSong != null) {
                binding.toolbar2.ivRecent.loadSongImage(
                    lastSong.albumId, lastSong.path, lastSong.cachedImagePath, lastSong.album
                )
            } else {
                binding.toolbar2.ivRecent.setImageDrawable(null)
                binding.toolbar2.ivRecent.setTag(R.id.image_model_tag, null)
            }
        }

        favoritesViewModel.favoriteSongs.collectWithLifecycle(this) { songs ->
            val lastSong = songs.firstOrNull()
            if (lastSong != null) {
                binding.toolbar2.ivFavourites.loadSongImage(
                    lastSong.albumId, lastSong.path, lastSong.cachedImagePath, lastSong.album
                )
            } else {
                binding.toolbar2.ivFavourites.setImageDrawable(null)
                binding.toolbar2.ivFavourites.setTag(R.id.image_model_tag, null)
            }
        }

        lifecycleScope.launch {
            MultiColorManager.currentThemeId.collect {
                MultiColorManager.applyTheme(this@MainActivity)

                // Refresh themed fallbacks without flickering real images
                fun refreshIfThemed(iv: ImageView, song: MusicFiles?) {
                    // If tag is an Int, it means it's a resource (fallback), so reset to force re-theming
                    if (iv.getTag(R.id.image_model_tag) is Int) {
                        iv.setTag(R.id.image_model_tag, null)
                    }
                    if (song != null) {
                        iv.loadSongImage(song.albumId, song.path, song.cachedImagePath, song.album)
                    } else {
                        iv.setImageDrawable(null)
                        iv.setTag(R.id.image_model_tag, null)
                    }
                }

                refreshIfThemed(
                    binding.toolbar2.ivRecent, recentViewModel.recentSongs.value.firstOrNull()
                )
                refreshIfThemed(
                    binding.toolbar2.ivFavourites,
                    favoritesViewModel.favoriteSongs.value.firstOrNull()
                )
            }
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
            (tabBinding.tabContainer.layoutParams as MarginLayoutParams).setMargins(
                marginSmall, 0, marginSmall, 0
            )
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