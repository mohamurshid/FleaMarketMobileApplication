package com.labs.fleamarketapp.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.labs.fleamarketapp.LoginActivity
import com.labs.fleamarketapp.R
import com.labs.fleamarketapp.adapter.ProfilePagerAdapter
import com.labs.fleamarketapp.adapter.ProfileSection
import com.labs.fleamarketapp.databinding.FragmentProfileBinding
import com.labs.fleamarketapp.util.ImagePicker.registerImagePicker
import com.labs.fleamarketapp.viewmodel.UserViewModel
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by activityViewModels()

    private var mediator: TabLayoutMediator? = null
    private val sections = listOf(
        ProfileSection("My Listings", "Track and manage every item you’ve posted."),
        ProfileSection("My Orders", "View purchases and pickup schedules."),
        ProfileSection("My Bids", "Stay on top of auctions you’re participating in."),
        ProfileSection("Settings", "Update your profile, notifications, and preferences.")
    )

    private val photoPicker = registerImagePicker { uri ->
        uri?.let { updateProfilePhoto(it) }
    }

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
        setupViewPager()
        setupStats()
        setupClickListeners()
        updateProfile(viewModel.currentUser.value)
        observeUser()
    }

    private fun setupViewPager() {
        binding.profilePager.adapter = ProfilePagerAdapter(this, sections)
        mediator = TabLayoutMediator(binding.profileTabs, binding.profilePager) { tab, position ->
            tab.text = sections[position].title
        }.also { it.attach() }
    }

    private fun setupStats() {
        binding.statsSold.statValue.text = "12"
        binding.statsSold.statLabel.text = "Items Sold"

        binding.statsBought.statValue.text = "8"
        binding.statsBought.statLabel.text = "Items Bought"

        binding.statsMember.statValue.text = "2021"
        binding.statsMember.statLabel.text = "Member Since"
    }

    private fun setupClickListeners() {
        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
        binding.editProfileButton.setOnClickListener {
            photoPicker.launch("image/*")
        }
        binding.editPhotoButton.setOnClickListener {
            photoPicker.launch("image/*")
        }
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentUser.collect { user ->
                    if (_binding != null) updateProfile(user)
                }
            }
        }
    }

    private fun updateProfile(user: com.labs.fleamarketapp.data.User?) {
        if (user == null) {
            binding.profileName.text = "Not logged in"
            binding.profileEmail.text = ""
            binding.profileImage.setImageResource(R.drawable.ic_launcher_foreground)
            return
        }
        binding.profileName.text = user.name
        binding.profileEmail.text = user.email
        binding.profileRating.text = "Rating • ${String.format("%.1f", user.rating)}"
        val photo = user.profileImageUrl
        if (!photo.isNullOrBlank()) {
            Glide.with(requireContext())
                .load(photo)
                .circleCrop()
                .into(binding.profileImage)
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun updateProfilePhoto(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .circleCrop()
            .into(binding.profileImage)
        // TODO persist photo via viewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator?.detach()
        _binding = null
    }
}
