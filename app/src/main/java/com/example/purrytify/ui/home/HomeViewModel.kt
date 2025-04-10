package com.example.purrytify.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text
}

data class Song(
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: String,
    val artworkUri: String
)