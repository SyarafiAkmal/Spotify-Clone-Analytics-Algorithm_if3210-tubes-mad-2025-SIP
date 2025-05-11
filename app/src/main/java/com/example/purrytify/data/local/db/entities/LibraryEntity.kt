package com.example.purrytify.data.local.db.entities
import androidx.room.Entity
import androidx.room.PrimaryKey


//enum class LibraryStatus(val value: String) {
//    LIKE("like"),
//    LIBRARY("library"),
//    LISTENED("listened")
//}

@Entity(
    tableName = "library_entity",
    primaryKeys = ["userEmail", "songId"]
)
data class LibraryEntity(
    val songId: Int,
    val userEmail: String,
    val libraryStatus: String
)