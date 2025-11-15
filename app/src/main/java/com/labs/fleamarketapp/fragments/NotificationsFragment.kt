package com.labs.fleamarketapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.databinding.FragmentNotificationsBinding
import com.labs.fleamarketapp.ui.adapter.NotificationAdapter
import com.labs.fleamarketapp.viewmodel.NotificationViewModel
import com.labs.fleamarketapp.viewmodel.UserViewModel
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {
    
    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()
    private val notificationAdapter = NotificationAdapter(
        onToggleRead = { notification, markRead ->
            if (markRead) notificationViewModel.markAsRead(notification.id)
            else notificationViewModel.markAsUnread(notification.id)
        },
        onOpenItem = { notification ->
            notification.itemId?.let {
                val bundle = bundleOf("itemId" to it)
                findNavController().navigate(com.labs.fleamarketapp.R.id.nav_item_detail, bundle)
            } ?: Toast.makeText(requireContext(), "No linked item", Toast.LENGTH_SHORT).show()
        }
    )
    private var activeUserId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupFilters()
        setupActions()
        setupObservers()
        observeUser()
    }
    
    private fun setupRecyclerView() {
        binding.notificationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = notificationAdapter
        }
    }
    
    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val unreadOnly = checkedIds.contains(binding.chipUnread.id)
            activeUserId?.let { notificationViewModel.loadNotifications(it, unreadOnly) }
        }
    }
    
    private fun setupActions() {
        binding.markAllReadButton.setOnClickListener {
            notificationViewModel.markAllAsRead()
        }
    }
    
    private fun setupObservers() {
        notificationViewModel.notificationsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.emptyStateCard.visibility = View.GONE
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        binding.emptyStateCard.visibility = View.VISIBLE
                    } else {
                        binding.emptyStateCard.visibility = View.GONE
                    }
                    notificationAdapter.submitList(state.data)
                }
                is UiState.Error -> {
                    binding.emptyStateCard.visibility = View.VISIBLE
                    binding.emptyText.text = state.message
                }
            }
        }
        
        notificationViewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.markAllReadButton.isEnabled = count > 0
            binding.headerSubtitle.text = if (count > 0) {
                getString(com.labs.fleamarketapp.R.string.notifications_unread_count, count)
            } else {
                getString(com.labs.fleamarketapp.R.string.notifications_subtitle)
            }
        }
    }
    
    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.currentUser.collectLatest { user ->
                if (user != null) {
                    activeUserId = user.id
                    binding.emptyStateCard.visibility = View.GONE
                    val unreadOnly = binding.chipUnread.isChecked
                    notificationViewModel.loadNotifications(user.id, unreadOnly)
                } else {
                    activeUserId = null
                    binding.emptyStateCard.visibility = View.VISIBLE
                    notificationAdapter.submitList(emptyList())
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        val userId = activeUserId ?: return
        val unreadOnly = binding.chipUnread.isChecked
        notificationViewModel.loadNotifications(userId, unreadOnly)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

