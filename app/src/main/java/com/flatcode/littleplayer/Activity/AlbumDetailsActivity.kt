package com.flatcode.littleplayer.Activity

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flatcode.littleplayer.Adapter.AlbumDetailsAdapter
import com.flatcode.littleplayer.Model.MusicFiles
import com.flatcode.littleplayer.Unit.DATA
import com.flatcode.littleplayer.Unit.VOID
import com.flatcode.littleplayer.databinding.ActivityAlbumDetailsBinding
import java.util.ArrayList

class AlbumDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlbumDetailsBinding
    private val context: Context = this@AlbumDetailsActivity

    private var albumName: String? = null
    private val albumSongs = ArrayList<MusicFiles>()
    private var adapter: AlbumDetailsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlbumDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        albumName = intent.getStringExtra(DATA.ALBUM_NAME)

        val allSongs = MainActivity.musicFiles
        if (allSongs != null && albumName != null) {
            var k = 0
            for (i in allSongs.indices) {
                if (albumName == allSongs[i].album) {
                    albumSongs.add(k, allSongs[i])
                    k++
                }
            }
        }

        if (albumSongs.isNotEmpty()) {
            val image = getAlbumArt(albumSongs[0].path)
            VOID.GlideByte(context, image, binding.image)
            VOID.GlideBlurByte(context, image, binding.imageBlur, 50)
        }
    }

    override fun onResume() {
        super.onResume()
        if (albumSongs.isNotEmpty()) {
            adapter = AlbumDetailsAdapter(context, albumSongs)
            binding.recyclerView.adapter = adapter
            binding.recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun getAlbumArt(uri: String?): ByteArray? {
        if (uri == null) return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(uri)
            val art = retriever.embeddedPicture
            retriever.release()
            art
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}