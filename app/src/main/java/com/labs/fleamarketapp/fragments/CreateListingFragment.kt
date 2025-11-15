package com.labs.fleamarketapp.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButtonToggleGroup
import com.labs.fleamarketapp.R
import com.labs.fleamarketapp.adapter.SelectedImageAdapter
import com.labs.fleamarketapp.data.PickupLocations
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.databinding.FragmentCreateListingBinding
import com.labs.fleamarketapp.util.FormValidator
import com.labs.fleamarketapp.util.ImagePicker.registerMultiImagePicker
import com.labs.fleamarketapp.viewmodel.ItemViewModel
import com.labs.fleamarketapp.viewmodel.UserViewModel
import java.util.Locale

class CreateListingFragment : Fragment() {

    private var _binding: FragmentCreateListingBinding? = null
    private val binding get() = _binding!!

    private val itemViewModel: ItemViewModel by activityViewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private val selectedImages = mutableListOf<Uri>()
    private val imageAdapter = SelectedImageAdapter(::removeImage)

    private val galleryPicker = registerMultiImagePicker { uris ->
        if (uris.isEmpty()) return@registerMultiImagePicker
        val remaining = MAX_IMAGES - selectedImages.size
        if (remaining <= 0) {
            Toast.makeText(requireContext(), "Maximum of $MAX_IMAGES images", Toast.LENGTH_SHORT).show()
            return@registerMultiImagePicker
        }
        selectedImages.addAll(uris.take(remaining))
        imageAdapter.submitList(selectedImages.toList())
    }

    private val categories = listOf(
        "Electronics" to 1L,
        "Books" to 2L,
        "Clothing" to 3L,
        "Jewellery" to 4L,
        "Furniture" to 5L,
        "Services" to null
    )
    private val conditions = listOf("New", "Like New", "Good", "Fair")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateListingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDropdowns()
        setupImagesRecycler()
        setupItemTypeToggle()
        setupObservers()
        setupButtons()
    }

    private fun setupDropdowns() {
        binding.categoryAutoComplete.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories.map { it.first })
        )
        binding.conditionAutoComplete.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, conditions)
        )
        binding.pickupLocationAutoComplete.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, PickupLocations.options)
        )
        binding.conditionAutoComplete.setText(conditions.first(), false)
        binding.pickupLocationAutoComplete.setText(PickupLocations.options.first(), false)
    }

    private fun setupImagesRecycler() {
        binding.selectedImagesRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }

    private fun setupItemTypeToggle() {
        binding.itemTypeToggle.check(R.id.fixedPriceButton)
        binding.itemTypeToggle.addOnButtonCheckedListener { _: MaterialButtonToggleGroup, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.fixedPriceButton -> {
                    binding.priceInput.visibility = View.VISIBLE
                    binding.startingBidInput.visibility = View.GONE
                    binding.auctionDurationInput.visibility = View.GONE
                    binding.startingBidEditText.text?.clear()
                    binding.auctionDurationEditText.text?.clear()
                }
                R.id.auctionButton -> {
                    binding.priceInput.visibility = View.GONE
                    binding.priceEditText.text?.clear()
                    binding.startingBidInput.visibility = View.VISIBLE
                    binding.auctionDurationInput.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupObservers() {
        itemViewModel.createItemState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.postListingButton.isEnabled = false
                is UiState.Success -> {
                    binding.postListingButton.isEnabled = true
                    Toast.makeText(context, "Listing created!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                is UiState.Error -> {
                    binding.postListingButton.isEnabled = true
                    Toast.makeText(context, state.message ?: "Failed to create listing", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.addImagesButton.setOnClickListener { galleryPicker.launch("image/*") }
        binding.postListingButton.setOnClickListener { submitForm() }
    }

    private fun submitForm() {
        val user = userViewModel.currentUser.value ?: run {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        if (!validateInput()) return

        val isAuction = binding.itemTypeToggle.checkedButtonId == R.id.auctionButton
        val priceValue = if (!isAuction) binding.priceEditText.text.toString().toDoubleOrNull() else null
        val startingBid = if (isAuction) binding.startingBidEditText.text.toString().toDoubleOrNull() else null
        val durationHours = if (isAuction) binding.auctionDurationEditText.text.toString().toIntOrNull() ?: 0 else 0
        val auctionEndTime = if (isAuction && durationHours > 0) {
            System.currentTimeMillis() + durationHours * 60 * 60 * 1000L
        } else null

        val categoryName = binding.categoryAutoComplete.text.toString()
        val categoryId = categories.firstOrNull { it.first == categoryName }?.second
        val condition = binding.conditionAutoComplete.text.toString()
            .uppercase(Locale.US)
            .replace(" ", "_")
        val pickupLocation = binding.pickupLocationAutoComplete.text.toString()

        itemViewModel.createItem(
            sellerId = user.id,
            title = binding.titleEditText.text.toString().trim(),
            description = binding.descriptionEditText.text.toString().trim(),
            price = priceValue,
            startingBid = startingBid,
            condition = condition,
            itemType = if (isAuction) "AUCTION" else "FIXED_PRICE",
            images = selectedImages.map { it.toString() },
            categoryId = categoryId,
            auctionEndTime = auctionEndTime,
            pickupLocation = pickupLocation
        )
    }

    private fun validateInput(): Boolean {
        var valid = true
        valid = FormValidator.requiredText(binding.titleInput, binding.titleEditText.text) && valid
        valid = FormValidator.requiredText(binding.descriptionInput, binding.descriptionEditText.text) && valid
        valid = FormValidator.dropdownSelection(binding.categoryInput, binding.categoryAutoComplete.text?.toString()) && valid
        valid = FormValidator.dropdownSelection(binding.conditionInput, binding.conditionAutoComplete.text?.toString()) && valid
        valid = FormValidator.dropdownSelection(binding.pickupLocationInput, binding.pickupLocationAutoComplete.text?.toString()) && valid

        when (binding.itemTypeToggle.checkedButtonId) {
            R.id.fixedPriceButton -> {
                val price = binding.priceEditText.text.toString().toDoubleOrNull()
                valid = FormValidator.positiveNumber(binding.priceInput, price) && valid
            }
            R.id.auctionButton -> {
                val bid = binding.startingBidEditText.text.toString().toDoubleOrNull()
                val duration = binding.auctionDurationEditText.text.toString().toIntOrNull()
                valid = FormValidator.positiveNumber(binding.startingBidInput, bid) && valid
                valid = FormValidator.positiveInteger(binding.auctionDurationInput, duration) && valid
            }
            else -> {
                Toast.makeText(context, "Select a pricing option", Toast.LENGTH_SHORT).show()
                valid = false
            }
        }
        return valid
    }

    private fun removeImage(uri: Uri) {
        selectedImages.remove(uri)
        imageAdapter.submitList(selectedImages.toList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MAX_IMAGES = 6
    }
}

