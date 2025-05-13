package com.example.purrytify.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity

class AlbumItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val albumImageView: ImageView
    private val titleTextView: TextView
    private val artistTextView: TextView
    private var albumSong: SongEntity? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_album_item, this, true)

        albumImageView = findViewById(R.id.album_image)
        titleTextView = findViewById(R.id.album_title)
        artistTextView = findViewById(R.id.album_artist)

        orientation = VERTICAL
    }

    fun setAlbumImage(@DrawableRes imageResId: Int) {
        albumImageView.setImageResource(imageResId)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setArtist(artist: String) {
        artistTextView.text = artist
    }

    fun setSong(song: SongEntity?) {
        albumSong = song
    }

    fun getSong(): SongEntity? {
        return albumSong
    }
}