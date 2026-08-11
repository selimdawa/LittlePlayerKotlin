package com.flatcode.littleplayer.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flatcode.littleplayer.R
import com.flatcode.littleplayer.databinding.ActivityTagEditorBinding
import com.flatcode.littleplayer.model.MusicFiles
import com.flatcode.littleplayer.utils.DATA
import com.flatcode.littleplayer.utils.loadSongImage
import com.flatcode.littleplayer.viewmodel.MusicViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class TagEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTagEditorBinding
    private val viewModel: MusicViewModel by viewModels()
    private var song: MusicFiles? = null
    private var newArtworkUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newArtworkUri = it
            binding.ivCover.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTagEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        song = intent.getParcelableExtra(DATA.SONG)
        if (song == null) {
            finish()
            return
        }

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.etTitle.setText(song?.title)
        binding.etArtist.setText(song?.artist)
        binding.etAlbum.setText(song?.album)
        binding.ivCover.loadSongImage(song?.albumId, song?.path, song?.cachedImagePath)
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnEditCover.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { saveTags() }
    }

    private fun saveTags() {
        val currentSong = song ?: return
        val path = currentSong.path ?: return
        val newTitle = binding.etTitle.text.toString()
        val newArtist = binding.etArtist.text.toString()
        val newAlbum = binding.etAlbum.text.toString()

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    val audioFile = AudioFileIO.read(file)
                    val tag = audioFile.tag ?: audioFile.createDefaultTag()

                    tag.setField(FieldKey.TITLE, newTitle)
                    tag.setField(FieldKey.ARTIST, newArtist)
                    tag.setField(FieldKey.ALBUM, newAlbum)

                    newArtworkUri?.let { uri ->
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            val tempFile = File(cacheDir, "temp_artwork.jpg")
                            FileOutputStream(tempFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            val artwork = ArtworkFactory.createArtworkFromFile(tempFile)
                            tag.deleteArtworkField()
                            tag.setField(artwork)
                            tempFile.delete()
                        }
                    }

                    audioFile.commit()
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }

            if (success) {
                viewModel.updateMetadata(currentSong.id ?: "", newTitle, newArtist, newAlbum)
                Toast.makeText(this@TagEditorActivity, R.string.tags_saved_successfully, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@TagEditorActivity, R.string.error_saving_tags, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
