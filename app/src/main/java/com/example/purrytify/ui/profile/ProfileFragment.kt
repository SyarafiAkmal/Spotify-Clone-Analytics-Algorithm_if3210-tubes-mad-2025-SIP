package com.example.purrytify.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.purrytify.LoginActivity
import com.example.purrytify.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        // Clear login state in SharedPreferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean("is_logged_in", false)
            // Optionally clear other user data
            remove("access_token")
            remove("refresh_token")
            remove("user_id")
            remove("username")
            remove("email")
        }

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Navigate back to LoginActivity
        val intent = Intent(requireActivity(), LoginActivity::class.java)
        // Clear back stack so user can't go back to the app without logging in again
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}