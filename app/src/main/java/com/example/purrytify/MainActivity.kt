package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.purrytify.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isLoggedIn = false // Changed from "lateinit var isLoggedIn: False" to "var isLoggedIn = false"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isLoggedIn) {
            // User is logged in, show main activity
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Set up navigation
            val navView: BottomNavigationView = binding.navView
            val navController = findNavController(R.id.nav_host_fragment_activity_main)
            val appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.navigation_home, R.id.navigation_library, R.id.navigation_profile
                )
            )
            navView.setupWithNavController(navController)
        } else {
            // User is not logged in, redirect to login activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close MainActivity so user can't go back with back button
        }
    }
}