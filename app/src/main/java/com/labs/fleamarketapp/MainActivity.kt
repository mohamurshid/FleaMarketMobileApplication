package com.labs.fleamarketapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.labs.fleamarketapp.databinding.ActivityMainBinding
import com.labs.fleamarketapp.data.User
import com.labs.fleamarketapp.viewmodel.UserViewModel

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val userViewModel: UserViewModel by viewModels()
    private var navController: NavController? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize user data if passed from LoginActivity
        initializeUserData()
        
        setupBottomNavigation()
    }
    
    private fun initializeUserData() {
        val user = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("USER_DATA", User::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<User>("USER_DATA")
        }
        user?.let {
            // Initialize the ViewModel with user data
            userViewModel.setUser(it)
}
    }
    
    private fun setupBottomNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment

        navHostFragment?.let {
            navController = it.navController
            binding.bottomNavigation.setupWithNavController(it.navController)
            interceptRestrictedDestinations()
        }
    }

    private fun interceptRestrictedDestinations() {
        val restrictedDestinations = setOf(
            R.id.nav_listings,
            R.id.nav_notifications,
            R.id.nav_profile
        )

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val requiresAuth = restrictedDestinations.contains(item.itemId)
            val user = userViewModel.currentUser.value

            if (requiresAuth && user == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnItemSelectedListener false
            }

            navController?.let { controller ->
                return@setOnItemSelectedListener NavigationUI.onNavDestinationSelected(item, controller)
            }
            false
        }

        binding.bottomNavigation.setOnItemReselectedListener { /* no-op */ }
    }
}
