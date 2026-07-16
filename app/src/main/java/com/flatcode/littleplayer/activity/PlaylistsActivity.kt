package com.flatcode.littleplayer.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.flatcode.littleplayer.databinding.ActivityPlaylistsBinding

class PlaylistsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}