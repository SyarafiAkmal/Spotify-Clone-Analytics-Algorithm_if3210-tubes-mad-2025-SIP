package com.example.purrytify.viewmodel  // Use your actual package

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import com.example.purrytify.R

class CapsuleStatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Header views
    private val monthYearTextView: TextView
    private val shareButton: ImageButton

    // Time listened views
    private val minutesTextView: TextView
    private val timeListenedCard: CardView

    // Top artist & song views
    private val topArtistTextView: TextView
    private val topSongTextView: TextView

    // Streak views
    private val streakImageView: ImageView
    private val streakTitleTextView: TextView
    private val streakDescriptionTextView: TextView
    private val streakDatesTextView: TextView
    private val streakShareButton: ImageButton

    // Optional click listeners
    private var onTimeListenedClickListener: (() -> Unit)? = null
    private var onTopArtistClickListener: (() -> Unit)? = null
    private var onTopSongClickListener: (() -> Unit)? = null
    private var onShareClickListener: (() -> Unit)? = null

    init {
        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.capsule_stats_component, this, true)

        orientation = VERTICAL

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
    fun setMonthYear(monthYear: String) {
        monthYearTextView.text = monthYear
    }

    fun setMinutes(minutes: Int) {
        minutesTextView.text = "$minutes minutes"
    }

    fun setTopArtist(artist: String) {
        topArtistTextView.text = artist
    }

    fun setTopSong(song: String) {
        topSongTextView.text = song
    }

    fun setStreakImage(@DrawableRes imageResId: Int) {
        streakImageView.setImageResource(imageResId)
    }

    fun setStreakInfo(days: Int, songTitle: String, artistName: String, startDate: String, endDate: String) {
        streakTitleTextView.text = "You had a $days-day streak!"
        streakDescriptionTextView.text = "You played $songTitle by $artistName day after day. You were on fire!"
        streakDatesTextView.text = "$startDate-$endDate"
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