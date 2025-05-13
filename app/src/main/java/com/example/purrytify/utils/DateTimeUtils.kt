package com.example.purrytify.utils
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    // ISO 8601 format
    private val ISO_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(ZoneId.of("UTC"))

    // Get current time as ISO string
    fun getCurrentTimeIso(): String {
        return ISO_FORMAT.format(Instant.now())
    }

    // Get empty/default value for new songs
    fun getEmptyTimeValue(): String {
        return "1970-01-01T00:00:00.000Z"
    }
}