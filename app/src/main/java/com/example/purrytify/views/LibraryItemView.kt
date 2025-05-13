package com.example.purrytify.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.SongEntity

class LibraryItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val coverImageView: ImageView
    private val titleTextView: TextView
    private val artistTextView: TextView
    private var song: SongEntity? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_library_item, this, true)

        coverImageView = findViewById(R.id.libraryItemCover)
        titleTextView = findViewById(R.id.libraryItemTitle)
        artistTextView = findViewById(R.id.libraryItemArtist)
    }

    fun setAlbumImage(resourceId: Int) {
        coverImageView.setImageResource(resourceId)
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setArtist(artist: String) {
        artistTextView.text = artist
    }

    fun setSong(song: SongEntity) {
        this.song = song
        setTitle(song.title)
        setArtist(song.artist)
        // You'll need to handle the image separately with a proper method
    }

    fun getSong(): SongEntity? {
        return song
    }
}