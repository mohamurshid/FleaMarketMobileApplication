package com.labs.fleamarketapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.labs.fleamarketapp.R
import com.labs.fleamarketapp.adapter.CategoryAdapter
import com.labs.fleamarketapp.adapter.FeaturedListingAdapter
import com.labs.fleamarketapp.data.HomeCategory
import com.labs.fleamarketapp.data.Item
import com.labs.fleamarketapp.data.ItemStatus
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.databinding.FragmentHomeBinding
import com.labs.fleamarketapp.LoginActivity
import com.labs.fleamarketapp.viewmodel.ItemViewModel
import com.labs.fleamarketapp.viewmodel.UserViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.util.UUID

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var categoryAdapter: CategoryAdapter
    private val featuredAdapter = FeaturedListingAdapter(::openItemDetails)

    private var featuredItems: List<Item> = emptyList()
    private var selectedCategory = "All"

    private val categories = listOf(
        HomeCategory("All", "Everything new"),
        HomeCategory("Books", "Texts & notes"),
        HomeCategory("Electronics", "Phones & laptops"),
        HomeCategory("Furniture", "Dorm essentials"),
        HomeCategory("Fashion", "Fits & accessories"),
        HomeCategory("Services", "Tutors & gigs"),
        HomeCategory("Tickets", "Events & socials"),
        HomeCategory("Free", "Giveaways")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBanner()
        setupSearch()
        setupRecyclerViews()
        bindCategories()
        setupObservers()
        setupLoginCta()
        populateSampleData()
        viewModel.loadFeaturedItems()
    }

    private fun setupBanner() {
        binding.createListingButton.setOnClickListener {
            handleRestrictedAction {
                findNavController().navigate(R.id.nav_create_listing)
            }
        }

        binding.shopButton.setOnClickListener {
            handleRestrictedAction {
                findNavController().navigate(R.id.nav_listings)
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged {
            applyFilters()
        }
        binding.filterButton.setOnClickListener {
            Toast.makeText(requireContext(), "Filter coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerViews() {
        binding.categoriesRecycler.apply {
            layoutManager = GridLayoutManager(context, 2)
            isNestedScrollingEnabled = false
        }
        binding.featuredRecycler.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = featuredAdapter
        }
    }

    private fun bindCategories() {
        categoryAdapter = CategoryAdapter(categories) { category ->
            selectedCategory = category.title
            applyFilters()
        }
        binding.categoriesRecycler.adapter = categoryAdapter
    }

    private fun setupObservers() {
        viewModel.featuredItemsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.isVisible = true
                    binding.errorText.isVisible = false
                }
                is UiState.Success -> {
                    binding.progressBar.isVisible = false
                    binding.errorText.isVisible = false
                    updateListings(state.data)
                }
                is UiState.Error -> {
                    binding.progressBar.isVisible = false
                    binding.errorText.isVisible = true
                    binding.errorText.text = state.message
                }
            }
        }
    }

    private fun setupLoginCta() {
        binding.loginButton.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                userViewModel.currentUser.collect { user ->
                    binding.loginButton.isVisible = user == null
                }
            }
        }
    }

    private fun populateSampleData() {
        val sample = listOf(
            sampleItem("ThinkPad X1 Carbon", "Electronics", "Brian O.", "STC", 165000.0),
            sampleItem("Graphic Design Services", "Services", "Lynn M.", "Phase1 Gazebos", 2500.0),
            sampleItem("Principles of Accounting", "Books", "Kevin W.", "Library Atrium", 1800.0),
            sampleItem("Dorm Sofa", "Furniture", "Maria N.", "Parking Lot", 9500.0),
            sampleItem("Canon M50 Camera", "Electronics", "Ian K.", "Phase2 Gazebos", 78000.0)
        )
        updateListings(sample)
    }

    private fun sampleItem(
        title: String,
        category: String,
        seller: String,
        pickup: String,
        price: Double
    ): Item {
        val image = SAMPLE_IMAGES.random()
        return Item(
            id = UUID.randomUUID().toString(),
            title = title,
            description = "Well maintained $title. Available for campus pickup.",
            price = price,
            imageUrl = image,
            images = listOf(image),
            category = category,
            sellerId = "sample",
            sellerName = seller,
            pickupLocation = pickup,
            createdAt = System.currentTimeMillis() - (0..72).random() * 60 * 60 * 1000L,
            status = ItemStatus.AVAILABLE
        )
    }

    private fun updateListings(items: List<Item>) {
        featuredItems = items.sortedByDescending { it.createdAt }.take(6)
        applyFilters()
    }

    private fun applyFilters() {
        val query = binding.searchEditText.text?.toString()?.trim().orEmpty()
        val filtered = featuredItems.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category.equals(selectedCategory, true)
            val matchesQuery = query.isBlank() ||
                    item.title.contains(query, true) ||
                    item.description.contains(query, true)
            matchesCategory && matchesQuery
        }
        featuredAdapter.submitList(filtered)
        binding.emptyStateText.isVisible = filtered.isEmpty()
    }

    private fun openItemDetails(item: Item) {
        handleRestrictedAction {
            val bundle = Bundle().apply { putString("itemId", item.id) }
            findNavController().navigate(R.id.nav_item_detail, bundle)
        }
    }

    private fun handleRestrictedAction(action: () -> Unit) {
        val user = userViewModel.currentUser.value
        if (user == null) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        } else {
            action()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val SAMPLE_IMAGES = listOf(
            "https://images.unsplash.com/photo-1517336714731-489689fd1ca8",
            "https://images.unsplash.com/photo-1523475472560-d2df97ec485c",
            "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab",
            "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f"
        )
    }
}
