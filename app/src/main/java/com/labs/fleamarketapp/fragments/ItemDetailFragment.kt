package com.labs.fleamarketapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayoutMediator
import com.labs.fleamarketapp.R
import com.labs.fleamarketapp.adapter.BidHistoryAdapter
import com.labs.fleamarketapp.adapter.ImagePagerAdapter
import com.labs.fleamarketapp.data.Item
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.databinding.FragmentItemDetailBinding
import com.labs.fleamarketapp.util.UIHelper
import com.labs.fleamarketapp.viewmodel.AuctionViewModel
import com.labs.fleamarketapp.viewmodel.UserViewModel
import java.util.Locale
import java.util.concurrent.TimeUnit

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val auctionViewModel: AuctionViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private val imageAdapter = ImagePagerAdapter()
    private val bidHistoryAdapter = BidHistoryAdapter()
    private var mediator: TabLayoutMediator? = null

    private var currentItem: Item? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imagePager.adapter = imageAdapter
        mediator = TabLayoutMediator(binding.imagesIndicator, binding.imagePager) { _, _ -> }
        mediator?.attach()

        binding.bidHistoryRecycler.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = bidHistoryAdapter
        }

        setupObservers()
        setupButtons()

        val itemId = arguments?.getString("itemId").orEmpty()
        if (itemId.isNotEmpty()) {
            auctionViewModel.loadItem(itemId)
            auctionViewModel.loadBids(itemId)
        }
    }

    private fun setupObservers() {
        auctionViewModel.itemState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.detailProgress.isVisible = true
                is UiState.Success -> {
                    binding.detailProgress.isVisible = false
                    bindItem(state.data)
                }

                is UiState.Error -> {
                    binding.detailProgress.isVisible = false
                    Toast.makeText(
                        context,
                        state.message ?: "Failed to load item",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        auctionViewModel.bidsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> { /* optional shimmer */
                }

                is UiState.Success -> bidHistoryAdapter.submitList(state.data)
                is UiState.Error -> Toast.makeText(
                    context,
                    state.message ?: "Failed to load bids",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        auctionViewModel.placeBidState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> binding.placeBidButton.isEnabled = false
                is UiState.Success -> {
                    binding.placeBidButton.isEnabled = true
                    binding.bidAmountEditText.text?.clear()
                    Toast.makeText(context, "Bid placed!", Toast.LENGTH_SHORT).show()
                }

                is UiState.Error -> {
                    binding.placeBidButton.isEnabled = true
                    Toast.makeText(context, state.message ?: "Bid failed", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun bindItem(item: Item) {
        currentItem = item

        val images = when {
            item.images.isNotEmpty() -> item.images
            !item.imageUrl.isNullOrBlank() -> listOf(item.imageUrl)
            else -> listOf(
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=60"
            )
        }
        imageAdapter.submitList(images)

        binding.itemTitle.text = item.title
        binding.itemPrice.text = UIHelper.formatPrice(item.price)
        binding.itemCategory.text = "Category: ${item.category.ifBlank { "General" }}"
        binding.pickupChip.text = getString(R.string.label_pickup_short, item.pickupLocation)
        binding.timePostedChip.text = UIHelper.relativeTime(item.createdAt)
        binding.itemDescription.text = item.description
        binding.conditionValue.text = "Condition: ${item.conditionLabel()}"
        binding.categoryValue.text = "Category: ${item.category.ifBlank { "General" }}"
        binding.pickupValue.text = "Pickup: ${item.pickupLocation}"

        binding.sellerName.text = item.sellerName.ifBlank { "Strathmore Seller" }
        binding.sellerRating.text = "Rating • 4.8"
        binding.sellerStats.text = "16 sales • Member since 2021"
        binding.sellerAvatar.setImageResource(R.drawable.ic_launcher_foreground)

        binding.contactButton.setOnClickListener { toast("Contact seller feature coming soon") }
        binding.offerButton.setOnClickListener { toast("Make offer coming soon") }
        binding.favoriteButton.setOnClickListener { toast("Saved to favorites") }

        if (item.isAuction) {
            binding.auctionCard.isVisible = true
            binding.primaryActionButton.isVisible = false
            val currentBid = item.currentBid ?: item.price
            binding.currentBidAmount.text = UIHelper.formatPrice(currentBid)
            binding.timeRemaining.text = formatTimeRemaining(item.auctionEndTime)
        } else {
            binding.auctionCard.isVisible = false
            binding.primaryActionButton.isVisible = true
            binding.primaryActionButton.text = "Buy Now"
            binding.primaryActionButton.setOnClickListener { navigateToCheckout(item) }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        binding.placeBidButton.setOnClickListener {
            val bidAmount = binding.bidAmountEditText.text?.toString()?.toDoubleOrNull()
            if (bidAmount == null || bidAmount <= 0) {
                toast("Enter a valid bid amount")
                return@setOnClickListener
            }
            val user = userViewModel.currentUser.value
            if (user == null) {
                toast("Please log in to bid")
                return@setOnClickListener
            }
            val itemId = currentItem?.id ?: return@setOnClickListener
            auctionViewModel.placeBid(itemId, user.id, bidAmount)
        }

        binding.primaryActionButton.setOnClickListener {
            currentItem?.let { navigateToCheckout(it) }
        }
    }

    private fun navigateToCheckout(item: Item) {
        val bundle = bundleOf(
            "itemId" to item.id,
            "itemTitle" to item.title,
                    "itemPrice" to item.price.toString(),
            "sellerLocation" to item.pickupLocation
        )
        findNavController().navigate(R.id.nav_checkout, bundle)
    }

    private fun formatTimeRemaining(endTime: Long?): String {
        if (endTime == null) return "Ends soon"
        val diff = endTime - System.currentTimeMillis()
        if (diff <= 0) return "Auction ended"
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff - TimeUnit.HOURS.toMillis(hours))
        return if (hours > 0) "Ends in ${hours}h ${minutes}m" else "Ends in ${minutes}m"
    }

    private fun Item.conditionLabel(): String {
        if (condition.isBlank()) return "Good"
        return condition.replace('_', ' ')
            .lowercase(Locale.US)
            .replaceFirstChar { it.titlecase(Locale.US) }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediator?.detach()
        _binding = null
    }
}

