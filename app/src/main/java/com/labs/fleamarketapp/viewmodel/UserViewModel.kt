package com.labs.fleamarketapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labs.fleamarketapp.api.ApiClient
import com.labs.fleamarketapp.api.models.LoginRequest
import com.labs.fleamarketapp.api.models.RegisterRequest
import com.labs.fleamarketapp.data.UiState
import com.labs.fleamarketapp.data.User
import com.labs.fleamarketapp.data.UserType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class UserViewModel : ViewModel() {
    
    private val api = ApiClient.api
    
    private val _loginState = MutableLiveData<UiState<User>>()
    val loginState: LiveData<UiState<User>> = _loginState
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()
    
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    private val _profileState = MutableLiveData<UiState<User>>()
    val profileState: LiveData<UiState<User>> = _profileState
    
    private val _signupState = MutableLiveData<UiState<User>>()
    val signupState: LiveData<UiState<User>> = _signupState
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                val response = api.login(LoginRequest(email = email, password = password))
                if (response.isSuccessful && response.body()?.success == true) {
                    val userData = response.body()?.data
                    if (userData != null) {
                        val user = User(
                            id = userData.id,
                            email = userData.email,
                            name = "${userData.firstName} ${userData.lastName}".trim(),
                            phone = userData.phone,
                            rating = userData.rating.toFloat(),
                            userType = resolveUserType(userData.role),
                            status = userData.status
                        )
                        _currentUser.value = user
                        _authToken.value = user.id // Using user ID as token for now
                        _loginState.value = UiState.Success(user)
                    } else {
                        _loginState.value = UiState.Error(response.body()?.message ?: "Login failed")
                    }
                } else {
                    _loginState.value = UiState.Error(response.body()?.message ?: "Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Login failed")
            }
        }
    }
    
    fun logout() {
        _currentUser.value = null
        _authToken.value = null
    }
    
    fun updateProfile(user: User) {
        viewModelScope.launch {
            _profileState.value = UiState.Loading
            try {
                // TODO: Replace with actual API call
                _currentUser.value = user
                _profileState.value = UiState.Success(user)
            } catch (e: Exception) {
                _profileState.value = UiState.Error(e.message ?: "Update failed")
            }
        }
    }
    
    fun isValidStrathmoreEmail(email: String): Boolean {
        return email.endsWith("@strathmore.edu", ignoreCase = true)
    }
    
    fun setUser(user: User) {
        _currentUser.value = user
        _authToken.value = user.authToken
    }
    
    fun signup(email: String, password: String, name: String, userType: UserType, phone: String? = null) {
        viewModelScope.launch {
            _signupState.value = UiState.Loading
            try {
                val (firstName, lastName) = splitName(name)
                val request = RegisterRequest(
                    email = email,
                    password = password,
                    firstName = firstName,
                    lastName = lastName,
                    phone = phone,
                    role = userType.name
                )
                
                val response = api.register(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    val userData = response.body()?.data
                    if (userData != null) {
                        val user = User(
                            id = userData.id,
                            email = userData.email,
                            name = "${userData.firstName} ${userData.lastName}".trim(),
                            phone = userData.phone,
                            rating = userData.rating.toFloat(),
                            userType = resolveUserType(userData.role),
                            status = userData.status
                        )
                        _currentUser.value = user
                        _authToken.value = user.id
                        _signupState.value = UiState.Success(user)
                    } else {
                        _signupState.value = UiState.Error(response.body()?.message ?: "Signup failed")
                    }
                } else {
                    _signupState.value = UiState.Error(response.body()?.message ?: "Signup failed")
                }
            } catch (e: Exception) {
                _signupState.value = UiState.Error(e.message ?: "Signup failed")
            }
        }
    }
    
    private fun splitName(fullName: String): Pair<String, String> {
        val parts = fullName.trim().split("\\s+".toRegex(), limit = 2)
        val first = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: "User"
        val last = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: first
        return first to last
    }
    
    private fun resolveUserType(role: String): UserType =
        when (role.uppercase(Locale.US)) {
            "SELLER" -> UserType.SELLER
            "ADMIN" -> UserType.ADMIN
            else -> UserType.BUYER
        }
}

