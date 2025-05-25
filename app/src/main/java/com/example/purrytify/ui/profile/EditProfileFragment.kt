package com.example.purrytify.ui.profile

import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.purrytify.R
import com.example.purrytify.api.ApiClient
import com.example.purrytify.databinding.EditProfileBinding
import com.example.purrytify.models.EditProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditProfileFragment : DialogFragment() {

    private var _binding: EditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var prefs: SharedPreferences
    private var selectedImageUri: Uri? = null

    companion object {
        private const val REQUEST_CODE_PICK_IMAGE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = EditProfileBinding.inflate(inflater, container, false)
        profileViewModel = ViewModelProvider(requireActivity())[ProfileViewModel::class.java]
        prefs = requireContext().getSharedPreferences("app_prefs", MODE_PRIVATE)

        setupCountryDropdown()
        setupButtons()

        return binding.root
    }

    private fun setupCountryDropdown() {
        val countries = resources.getStringArray(R.array.countries)
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, countries)
        binding.dropdownCountry.setAdapter(adapter)

        val defaultCountry = profileViewModel.countriesMap[prefs.getString("country_code", "")] ?: countries[0]
        binding.dropdownCountry.setText(defaultCountry, false)
    }

    private fun setupButtons() {
        binding.btnEditPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val countryText = binding.dropdownCountry.text.toString()
        val countryCode = profileViewModel.reversedCountriesMap[countryText]
        val token = "Bearer ${prefs.getString("access_token", "")}"

        binding.btnSaveProfile.apply {
            isEnabled = false
            text = "Saving..."
        }

        lifecycleScope.launch {
            try {
                val locationBody = countryCode?.let {
                    it.toRequestBody("text/plain".toMediaTypeOrNull())
                }

                val imagePart = selectedImageUri?.let { uri ->
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val imageBytes = inputStream?.readBytes()
                    inputStream?.close()

                    imageBytes?.let {
                        val requestFile = it.toRequestBody("image/*".toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("profilePhoto", "profile.jpg", requestFile)
                    }
                }

                val response = ApiClient.api.editProfile(location = locationBody!!, profilePhoto = imagePart, token = token)

                withContext(Dispatchers.Main) {
                    val message = if (response.isSuccessful) {
                        prefs.edit().putString("country_code", countryCode).apply()
                        profileViewModel.loadProfileData(prefs)
                        parentFragmentManager.setFragmentResult("profile_updated", Bundle())
                        dismiss()
                        "Profile updated successfully."
                    } else {
                        "Update failed: ${response.code()} ${response.message()}"
                    }

                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    resetSaveButton(response.isSuccessful)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    resetSaveButton(false)
                }
            }
        }
    }


    private fun resetSaveButton(success: Boolean) {
        if (!success) {
            binding.btnSaveProfile.apply {
                isEnabled = true
                text = "Save Changes"
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            binding.profileImage.setImageURI(selectedImageUri)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            val screenWidth = requireContext().resources.displayMetrics.widthPixels
            val dialogWidth = (screenWidth * 0.8).toInt()

            val params = attributes
            params.width = dialogWidth
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            params.gravity = android.view.Gravity.CENTER
            attributes = params
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
