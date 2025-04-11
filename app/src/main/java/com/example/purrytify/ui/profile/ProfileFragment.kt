package com.example.purrytify.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.purrytify.LoginActivity
import com.example.purrytify.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(ProfileViewModel::class.java)

        // Inflate layout
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up observers
        setupObservers()

        // Load data
        loadProfileData()

        // Set up button click listeners
        setupClickListeners()
    }

    private fun setupObservers() {
        // Observe profile picture
        viewModel.profilePicture.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.profileImage.setImageBitmap(bitmap)
            } else {
                // Set a placeholder image if bitmap is null
                binding.profileImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Show/hide loading indicator if you have one
            binding.progressBar?.isVisible = isLoading
        }

        // Observe errors
        viewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadProfileData() {
        // Get SharedPreferences
        val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

        // Ask ViewModel to load profile data
        viewModel.loadProfileData(prefs)

        binding.profileName.text = prefs.getString("username", "")
    }

    private fun setupClickListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener {
            // Handle logout
            val prefs = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean("is_logged_in", false)
                remove("access_token")
                remove("refresh_token")
                remove("profile_pict")
                apply()
            }

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

            // Navigate back to LoginActivity
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            // Clear back stack so user can't go back to the app without logging in again
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Edit profile button
//        binding.btnEditProfile?.setOnClickListener {
//            // Handle edit profile
//            Toast.makeText(requireContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show()
//            // Navigate to edit profile screen if needed
//            // findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
//        }

        binding.btnEditProfile?.setOnClickListener {
            Toast.makeText(requireContext(), "Tombol edit dipencet", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}