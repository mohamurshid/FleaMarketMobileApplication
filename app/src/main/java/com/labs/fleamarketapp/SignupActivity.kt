package com.labs.fleamarketapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.data.UserType
import com.labs.fleamarketapp.databinding.ActivitySignupBinding
import com.labs.fleamarketapp.util.ImagePicker.registerImagePicker
import com.labs.fleamarketapp.viewmodel.UserViewModel

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: UserViewModel by viewModels()

    private var selectedPhotoUri: Uri? = null
    private var capturedBitmap: Bitmap? = null

    private val galleryPicker = registerImagePicker { uri ->
        uri?.let {
            selectedPhotoUri = it
            capturedBitmap = null
            binding.removePhotoButton.isVisible = true
            Glide.with(this).load(it).circleCrop().into(binding.profilePreview)
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            capturedBitmap = it
            selectedPhotoUri = null
            binding.removePhotoButton.isVisible = true
            binding.profilePreview.setImageBitmap(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.signupState.observe(this) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.signupButton.isEnabled = false
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.signupButton.isEnabled = true
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_DATA", state.data)
                    })
                    finish()
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.signupButton.isEnabled = true
                    Toast.makeText(this, state.message ?: "Signup failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.signupButton.setOnClickListener {
            val firstName = binding.firstNameEditText.text.toString().trim()
            val lastName = binding.lastNameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val phone = binding.phoneEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            val role =
                if (binding.sellerRadio.isChecked) UserType.SELLER else UserType.BUYER
            val termsAccepted = binding.termsCheckbox.isChecked

            if (validateInput(firstName, lastName, email, phone, password, confirmPassword, termsAccepted)) {
                val fullName = "$firstName $lastName".trim()
                viewModel.signup(email, password, fullName, role, phone.ifEmpty { null })
            }
        }

        binding.loginLink.setOnClickListener { finish() }
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.emailEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) validateEmail(binding.emailEditText.text.toString().trim())
        }

        binding.galleryButton.setOnClickListener { galleryPicker.launch("image/*") }
        binding.cameraButton.setOnClickListener { cameraLauncher.launch(null) }
        binding.removePhotoButton.setOnClickListener { clearPhotoSelection() }
    }

    private fun validateInput(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ): Boolean {
        var isValid = true

        binding.firstNameInputLayout.error = if (firstName.isBlank()) {
            isValid = false
            "First name required"
        } else null

        binding.lastNameInputLayout.error = if (lastName.isBlank()) {
            isValid = false
            "Last name required"
        } else null

        when {
            email.isBlank() -> {
                binding.emailInputLayout.error = "Email is required"
                isValid = false
            }
            !validateEmail(email) -> isValid = false
            else -> binding.emailInputLayout.error = null
        }

        if (phone.isNotBlank() && !validatePhone(phone)) {
            binding.phoneInputLayout.error = "Invalid phone number"
            isValid = false
        } else {
            binding.phoneInputLayout.error = null
        }

        binding.passwordInputLayout.error = when {
            password.isBlank() -> {
                isValid = false
                "Password is required"
            }
            password.length < 6 -> {
                isValid = false
                "At least 6 characters"
            }
            else -> null
        }

        binding.confirmPasswordInputLayout.error = when {
            confirmPassword.isBlank() -> {
                isValid = false
                "Confirm your password"
            }
            password != confirmPassword -> {
                isValid = false
                "Passwords do not match"
            }
            else -> null
        }

        if (!termsAccepted) {
            binding.termsCheckbox.error = "Required"
            isValid = false
        } else {
            binding.termsCheckbox.error = null
        }

        return isValid
    }

    private fun validatePhone(phone: String): Boolean {
        val cleanedPhone = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        return cleanedPhone.matches(Regex("^[+]?[0-9]{7,15}$"))
    }

    private fun validateEmail(email: String): Boolean {
        val valid = viewModel.isValidStrathmoreEmail(email)
        binding.emailErrorText.isVisible = !valid
        binding.emailInputLayout.error = if (valid) null else "Invalid Strathmore email"
        return valid
    }

    private fun clearPhotoSelection() {
        selectedPhotoUri = null
        capturedBitmap = null
        binding.profilePreview.setImageResource(R.drawable.ic_profile_placeholder)
        binding.removePhotoButton.isVisible = false
    }
}
