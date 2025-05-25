package com.example.purrytify.viewmodel  // Use your actual package

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import com.example.purrytify.R
import com.example.purrytify.data.local.db.entities.ArtistEntity
import com.example.purrytify.data.local.db.entities.CapsuleEntity
import com.example.purrytify.data.local.db.entities.SongEntity
import com.example.purrytify.utils.ImageUtils
import com.example.purrytify.views.MusicDbViewModel
import java.time.LocalDateTime
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

    private lateinit var musicDb: MusicDbViewModel

    // Header views
    private val monthYearTextView: TextView
    private val shareButton: ImageButton

    // Time listened views
    private val minutesTextView: TextView
    private val timeListenedCard: CardView

    // Top artist & song views
    private val topArtistTextView: TextView
    private val topSongTextView: TextView
    private val capsuleData: MutableLiveData<CapsuleEntity?> = MutableLiveData()
    private val topSongs: MutableList<SongEntity> = mutableListOf()
    private val topArtists: MutableList<ArtistEntity> = mutableListOf()

    // Streak views
    private val streakImageView: ImageView
    private val streakTitleTextView: TextView
    private val streakDescriptionTextView: TextView
    private val streakDatesTextView: TextView
    private val streakShareButton: ImageButton
    private val topSongsImageView: ImageView
    private val topArtistsImageView: ImageView

    // Optional click listeners
    private var onTimeListenedClickListener: (() -> Unit)? = null
    private var onTopArtistClickListener: (() -> Unit)? = null
    private var onTopSongClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null

    init {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.capsule_stats_component, this, true)

        orientation = VERTICAL

        // Initialize data
        capsuleData.value = capsule

        topSongs.addAll(topSongsList)
        topArtists.addAll(topArtistsList)

        // Initialize views
        monthYearTextView = findViewById(R.id.tvMonthYear)
        shareButton = findViewById(R.id.btnShare)

        timeListenedCard = findViewById(R.id.cardTimeListened)
        minutesTextView = findViewById(R.id.tvMinutes)

        topArtistTextView = findViewById(R.id.tvTopArtist)
        topSongTextView = findViewById(R.id.tvTopSong)

        streakImageView = findViewById(R.id.ivStreakImage)
        streakTitleTextView = findViewById(R.id.tvStreakTitle)
        streakDescriptionTextView = findViewById(R.id.tvStreakDescription)
        streakDatesTextView = findViewById(R.id.tvStreakDates)
        streakShareButton = findViewById(R.id.btnStreakShare)

        topSongsImageView = findViewById(R.id.topSongCover)
        topArtistsImageView = findViewById(R.id.topArtistCover)

        // Set up click listeners
        setupClickListeners()
    }

    private fun setupClickListeners() {
        timeListenedCard.setOnClickListener {
            onTimeListenedClickListener?.invoke()
        }

        findViewById<CardView>(R.id.cardTopArtist).setOnClickListener {
            onTopArtistClickListener?.invoke()
        }

        findViewById<CardView>(R.id.cardTopSong).setOnClickListener {
            onTopSongClickListener?.invoke()
        }

        shareButton.setOnClickListener {
            onShareClickListener?.invoke()
        }

        streakShareButton.setOnClickListener {
            onShareClickListener?.invoke()
        }
    }

    // Public setter methods
    fun setMonthYear() {
        val dateTime = OffsetDateTime.parse(capsuleData.value?.capsuleDate)
        monthYearTextView.text = "${dateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}"
    }

    fun setMinutes() {
        minutesTextView.text = "${capsuleData.value?.minuteListened} minutes"
    }

    fun setTopArtist() {
        if (topArtists.isNotEmpty()) {
            topArtistTextView.text = topArtists[0].artistName
            ImageUtils.loadImage(context, topArtists[0].artistPicture, topArtistsImageView)
        } else {
            topArtistTextView.text = "No data yet"
        }
    }

    fun setTopSong() {
        if (topSongs.isNotEmpty()) {
            topSongTextView.text = topSongs[0].title
            ImageUtils.loadImage(context, topSongs[0].artworkURI, topSongsImageView)
        } else {
            topSongTextView.text = "No data yet"
        }
    }

    fun setStreakImage(songStreak: SongEntity?) {
        songStreak?.let { song ->
            ImageUtils.loadImage(context, song.artworkURI, streakImageView)
        }
    }

    fun setStreakInfo(songStreak: SongEntity?) {
        songStreak?.let { song ->
            try {
                val days = 0
                val dateInterval = (capsuleData.value?.songStreakInterval)?.split(",")
                streakDescriptionTextView.text = "You played ${songStreak.title} by ${songStreak.artist} day after day. You were on fire!"
                val startDate = OffsetDateTime.parse(dateInterval?.get(0))
                val endDate = OffsetDateTime.parse(dateInterval?.get(1))
//                Toast.makeText(context, "Date ${dateInterval?.get(0)} ${dateInterval?.get(1)}", Toast.LENGTH_SHORT).show()
                streakTitleTextView.text = "You had a ${endDate.format(DateTimeFormatter.ofPattern("d")).toInt() - startDate.format(DateTimeFormatter.ofPattern("d")).toInt() + 1}-day streak!"

                streakDatesTextView.text = "${startDate.format(DateTimeFormatter.ofPattern("MMM d"))}-${endDate.format(DateTimeFormatter.ofPattern("d, yyyy"))}"
            } catch (e: Exception) {
                streakDatesTextView.text = "No data yet"
            }
        }
    }

    // Click listener setters
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
}