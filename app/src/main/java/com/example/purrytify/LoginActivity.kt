package com.example.purrytify

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.api.ApiClient
import com.example.purrytify.databinding.ActivityLoginBinding
import com.example.purrytify.models.Login
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Login button click handler with API call
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(email: String, password: String) {
        lifecycleScope.launch {
            try {
                // Make API call to login
                val loginRequest = Login(email, password)
                val tokenResponse = ApiClient.api.login(loginRequest)

                // Save tokens to SharedPreferences
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                prefs.edit {
                    putBoolean("is_logged_in", true)
                    putString("access_token", tokenResponse.accessToken)
                    putString("refresh_token", tokenResponse.refreshToken)
                }

                // Fetch user profile
                val profile = ApiClient.api.getProfile("Bearer ${tokenResponse.accessToken}")

                // Save profile data if needed
                prefs.edit {
                    putString("user_id", profile.id.toString())
                    putString("username", profile.username)
                    putString("email", profile.email)
                }

                // Navigate to MainActivity
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()

            } catch (e: Exception) {
                // Show error message
                AlertDialog.Builder(this@LoginActivity)
                    .setTitle("Login Failed")
                    .setMessage(e.message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
}