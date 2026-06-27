package com.flatcode.littleplayer.activity

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.viewmodel.AlbumDetailsViewModel
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import com.flatcode.littleplayer.unit.DATA
import com.flatcode.littleplayer.unit.VOID

class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity
    private val viewModel: AlbumDetailsViewModel by viewModels()
    private var adapter: AlbumDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        setupRecyclerView()
        observeViewModel()

        val albumName = intent.getStringExtra(DATA.ALBUM_NAME)
        viewModel.filterSongsByAlbum(albumName)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager =
            LinearLayoutManager(context, RecyclerView.VERTICAL, false)
    }

    private fun observeViewModel() {
        viewModel.albumSongs.observe(this) { songs ->
            if (songs.isNotEmpty()) {
                val firstSongId = songs[0].id
                VOID.coiImage(context, firstSongId, binding.image)
                VOID.coiImageBlur(context, firstSongId, binding.imageBlur, 50)

                adapter = AlbumDetailsAdapter(context, songs)
                binding.recyclerView.adapter = adapter
            }
        }
    }
}