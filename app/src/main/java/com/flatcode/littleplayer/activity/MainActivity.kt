package com.flatcode.littleplayer.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import com.flatcode.littleplayer.utils.launchActivity
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val nowPlayerViewModel: com.flatcode.littleplayer.viewmodel.NowPlayerViewModel by viewModels()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                nowPlayerViewModel.currentPlayingSong.collect { song ->
                    binding.fragBottomPlayer.isVisible = song != null
                }
            }
        }

        binding.toolbar.searchBar.setOnClickListener { launchActivity<SearchActivity>() }
        binding.toolbar.tvSearchView.setOnClickListener { launchActivity<SearchActivity>() }
        binding.toolbar2.cardFavourites.setOnClickListener { launchActivity<FavoritesActivity>() }
        binding.toolbar2.cardPlaylists.setOnClickListener { launchActivity<PlaylistsActivity>() }

        permission()
    }

    private fun permission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), REQUEST_CODE_PERMISSION)
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
                            0 -> R.id.songsFragment
                            1 -> R.id.albumsFragment
                            2 -> R.id.artistsFragment
                            else -> R.id.foldersFragment
                        }
                    )
                }
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permission()
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
    }
}