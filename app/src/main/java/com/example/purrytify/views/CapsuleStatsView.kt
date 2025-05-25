package com.example.purrytify.viewmodel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.ArtistEntity
import com.example.purrytify.data.local.db.entities.CapsuleEntity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.views.MusicDbViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class CapsuleStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    capsule: CapsuleEntity?,
    topSongsList: List<SongEntity>,
    topArtistsList: List<ArtistEntity>
) : LinearLayout(context, attrs, defStyleAttr) {

    // Views
    private val monthYearTextView: TextView
    private val shareButton: ImageButton
    private val minutesTextView: TextView
    private val timeListenedCard: CardView
    private val topArtistTextView: TextView
    private val topSongTextView: TextView
    private val streakImageView: ImageView
    private val streakTitleTextView: TextView
    private val streakDescriptionTextView: TextView
    private val streakDatesTextView: TextView
    private val streakShareButton: ImageButton
    private val topSongsImageView: ImageView
    private val topArtistsImageView: ImageView

    // Data
    private val capsuleData: MutableLiveData<CapsuleEntity?> = MutableLiveData()
    private val topSongs: MutableList<SongEntity> = mutableListOf()
    private val topArtists: MutableList<ArtistEntity> = mutableListOf()

    // Click listeners
    private var onTimeListenedClickListener: (() -> Unit)? = null
    private var onTopArtistClickListener: (() -> Unit)? = null
    private var onTopSongClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.capsule_stats_component, this, true)
        orientation = VERTICAL

        capsuleData.value = capsule
        topSongs.addAll(topSongsList)
        topArtists.addAll(topArtistsList)

        // Find views
        monthYearTextView = findViewById(R.id.tvMonthYear)
        shareButton = findViewById(R.id.btnShare)
        minutesTextView = findViewById(R.id.tvMinutes)
        timeListenedCard = findViewById(R.id.cardTimeListened)
        topArtistTextView = findViewById(R.id.tvTopArtist)
        topSongTextView = findViewById(R.id.tvTopSong)
        streakImageView = findViewById(R.id.ivStreakImage)
        streakTitleTextView = findViewById(R.id.tvStreakTitle)
        streakDescriptionTextView = findViewById(R.id.tvStreakDescription)
        streakDatesTextView = findViewById(R.id.tvStreakDates)
        streakShareButton = findViewById(R.id.btnStreakShare)
        topSongsImageView = findViewById(R.id.topSongCover)
        topArtistsImageView = findViewById(R.id.topArtistCover)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        timeListenedCard.setOnClickListener { onTimeListenedClickListener?.invoke() }
        findViewById<CardView>(R.id.cardTopArtist).setOnClickListener { onTopArtistClickListener?.invoke() }
        findViewById<CardView>(R.id.cardTopSong).setOnClickListener { onTopSongClickListener?.invoke() }
        shareButton.setOnClickListener { onShareClickListener?.invoke() }
        streakShareButton.setOnClickListener { onShareClickListener?.invoke() }
    }

    fun setMonthYear() {
        capsuleData.value?.capsuleDate?.let {
            val dateTime = OffsetDateTime.parse(it)
            monthYearTextView.text = dateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
    }

    fun setMinutes() {
        val minutes = capsuleData.value?.minuteListened ?: 0
        minutesTextView.text = "$minutes minutes"
    }

    fun setTopArtist() {
        if (topArtists.isNotEmpty()) {
            val artist = topArtists[0]
            topArtistTextView.text = artist.artistName
            ImageUtils.loadImage(context, artist.artistPicture, topArtistsImageView)
        } else {
            topArtistTextView.text = "No data yet"
        }
    }

    fun setTopSong() {
        if (topSongs.isNotEmpty()) {
            val song = topSongs[0]
            topSongTextView.text = song.title
            ImageUtils.loadImage(context, song.artworkURI, topSongsImageView)
        } else {
            topSongTextView.text = "No data yet"
        }
    }

    fun setStreakImage(songStreak: SongEntity?) {
        songStreak?.let {
            ImageUtils.loadImage(context, it.artworkURI, streakImageView)
        }
    }

    fun setStreakInfo(songStreak: SongEntity?) {
        if (songStreak == null) {
            streakTitleTextView.text = ""
            streakDescriptionTextView.text = ""
            streakDatesTextView.text = "No data yet"
            return
        }

        try {
            val interval = capsuleData.value?.songStreakInterval?.split(",")
            if (interval != null && interval.size == 2) {
                val startDate = OffsetDateTime.parse(interval[0])
                val endDate = OffsetDateTime.parse(interval[1])

                val dayCount = endDate.dayOfMonth - startDate.dayOfMonth + 1
                streakTitleTextView.text = "You had a $dayCount-day streak!"
                streakDescriptionTextView.text = "You played ${songStreak.title} by ${songStreak.artist} day after day. You were on fire!"
                streakDatesTextView.text = "${startDate.format(DateTimeFormatter.ofPattern("MMM d"))}-${endDate.format(DateTimeFormatter.ofPattern("d, yyyy"))}"
            } else {
                streakDatesTextView.text = "No data yet"
            }
        } catch (e: Exception) {
            streakDatesTextView.text = "No data yet"
        }
    }

    // Public click listener setters
    fun setOnTimeListenedClickListener(listener: () -> Unit) {
        onTimeListenedClickListener = listener
    }

    fun setOnTopArtistClickListener(listener: () -> Unit) {
        onTopArtistClickListener = listener
    }

    fun setOnTopSongClickListener(listener: () -> Unit) {
        onTopSongClickListener = listener
    }

    fun setOnShareClickListener(listener: () -> Unit) {
        onShareClickListener = listener
    }

    // Optional: expose capsule data externally
    fun getCapsuleData(): CapsuleEntity? = capsuleData.value
}
