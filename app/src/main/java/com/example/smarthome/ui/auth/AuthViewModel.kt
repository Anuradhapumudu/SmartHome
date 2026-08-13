package com.example.smarthome.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthome.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _user = MutableStateFlow<FirebaseUser?>(repository.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAuthState().collect {
                _user.value = it
            }
        }
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.login(email, pass)
            } catch (e: FirebaseAuthException) {
                _error.value = mapFirebaseError(e)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Login failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signup(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _error.value = "Email and password cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.signup(email, pass)
            } catch (e: FirebaseAuthException) {
                _error.value = mapFirebaseError(e)
            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Signup failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun mapFirebaseError(e: FirebaseAuthException): String {
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address format"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "This email is already registered"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak"
            "ERROR_CONFIGURATION_NOT_FOUND" -> "Email/Password sign-in is not enabled in Firebase Console"
            "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection"
            else -> e.localizedMessage ?: "Authentication failed"
        }
    }

    fun logout() {
        repository.logout()
    }

    fun clearError() {
        _error.value = null
    }
}
