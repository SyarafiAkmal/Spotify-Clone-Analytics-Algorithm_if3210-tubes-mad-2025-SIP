package com.example.purrytify.ui.profile

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.purrytify.databinding.EditProfileBinding
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.R
import com.example.purrytify.api.ApiClient
import com.example.purrytify.models.EditProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response

class EditProfileFragment : DialogFragment() {

    private var _binding: EditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = EditProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
        val countries = resources.getStringArray(R.array.countries)


        val arrayAdapter = ArrayAdapter(
            requireContext(),
            R.layout.dropdown_item,
            countries
        )

        // Set the adapter to the AutoCompleteTextView
        binding.dropdownCountry.setAdapter(arrayAdapter)

        val prefs = requireContext().getSharedPreferences("app_prefs", MODE_PRIVATE)
        val defaultCountry = profileViewModel.countriesMap[prefs.getString("country_code", "")]
        val defaultIndex = countries.indexOf(defaultCountry)
        if (defaultIndex >= 0) {
            binding.dropdownCountry.setText(countries[defaultIndex], false)
        } else {
            // Fallback in case Indonesia is not in the array
            binding.dropdownCountry.setText(countries[0], false)
        }

        setupButtons()
        return binding.root
    }

    private fun refreshProfile() {

    }

    private fun setupButtons() {
        binding.btnSaveProfile.setOnClickListener {
            // Handle save button click
            val dropDownText: String = binding.dropdownCountry.text.toString()
            val prefs: SharedPreferences = requireContext().getSharedPreferences("app_prefs", MODE_PRIVATE)

            // Show loading state
            binding.btnSaveProfile.isEnabled = false
            binding.btnSaveProfile.text = "Saving..."

            viewLifecycleOwner.lifecycleScope.launch {
                Log.d("EditProfile", "Starting edit profile request")
                try {
                    val countryCode = profileViewModel.reversedCountriesMap[dropDownText.toString()]
                    Log.d("EditProfile", "Country code: $countryCode for country: ${dropDownText.toString()}")

                    val token = "Bearer ${prefs.getString("access_token", "")}"
                    Log.d("EditProfile", "Token: ${token.take(15)}...")

                    val response = ApiClient.api.editProfile(
                        EditProfile(
                            location = countryCode,
                            profilePhoto = null
                        ),
                        token
                    )

                    Log.d("EditProfile", "Response received: ${response.code()}")

                    // Always show a toast regardless of success/failure
                    withContext(Dispatchers.Main) {
                        try {
                            if (isAdded) {
                                val message = if (response.isSuccessful) {
                                    val body = response.body()
                                    "Profile updated successfully: ${body?.message ?: "Success"}"
                                } else {
                                    "Update failed: ${response.code()} ${response.message()}"
                                }

                                Log.d("EditProfile", "Showing toast: $message")
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                                if (response.isSuccessful) {
                                    val body = response.body()
                                    // Update SharedPreferences with new country code
                                    prefs.edit().putString("country_code", countryCode).apply()

                                    // Reload profile data
                                    profileViewModel.loadProfileData(prefs)

                                    // Notify the ProfileFragment to refresh
                                    parentFragmentManager.setFragmentResult("profile_updated", Bundle())

                                    dismiss() // Only dismiss on success
                                } else {
                                    // Reset button if failed
                                    binding.btnSaveProfile.isEnabled = true
                                    binding.btnSaveProfile.text = "Save Changes"
                                }
                            } else {
                                Log.e("EditProfile", "Fragment not attached, can't show Toast")
                            }
                        } catch (e: Exception) {
                            Log.e("EditProfile", "Error showing toast: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EditProfile", "Exception in API call: ${e.message}", e)

                    try {
                        withContext(Dispatchers.Main) {
                            if (isAdded && context != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Error: ${e.message ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()

                                // Reset button on error
                                binding.btnSaveProfile.isEnabled = true
                                binding.btnSaveProfile.text = "Save Changes"
                            }
                        }
                    } catch (toastException: Exception) {
                        Log.e("EditProfile", "Error showing error toast: ${toastException.message}", toastException)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Optional: Make dialog full width
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // Get screen dimensions
            val displayMetrics = requireContext().resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // Calculate desired width (e.g., 90% of screen width)
            val dialogWidth = (screenWidth * 0.8).toInt()

            // Set parameters
            val params = attributes
            params.width = dialogWidth
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT

            // Center the dialog
            params.gravity = android.view.Gravity.CENTER

            // Apply the parameters
            attributes = params
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}