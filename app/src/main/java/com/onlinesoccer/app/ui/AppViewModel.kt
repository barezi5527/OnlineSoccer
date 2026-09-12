package com.onlinesoccer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onlinesoccer.app.core.auth.AuthUiState
import com.onlinesoccer.app.core.auth.LoginResult
import com.onlinesoccer.app.core.auth.SessionManager
import com.onlinesoccer.app.core.storage.TokenStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    val authState: StateFlow<AuthUiState> = sessionManager.state

    private val _email = MutableStateFlow(tokenStorage.lastEmail.orEmpty())
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _loggingIn = MutableStateFlow(false)
    val loggingIn: StateFlow<Boolean> = _loggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    init {
        viewModelScope.launch { sessionManager.restore() }
    }

    fun onEmailChange(value: String) {
        _email.value = value
    }

    fun onPasswordChange(value: String) {
        _password.value = value
    }

    fun login() {
        if (_loggingIn.value) return
        val email = _email.value.trim()
        val password = _password.value
        if (email.isEmpty() || password.isEmpty()) {
            _loginError.value = "Bitte Mail und Passwort eingeben."
            return
        }
        viewModelScope.launch {
            _loggingIn.value = true
            _loginError.value = null
            tokenStorage.lastEmail = email
            when (val result = sessionManager.login(email, password)) {
                is LoginResult.Success -> Unit
                is LoginResult.Failure -> _loginError.value = result.message
            }
            _loggingIn.value = false
        }
    }

    fun guestLogin() {
        if (_loggingIn.value) return
        viewModelScope.launch {
            _loggingIn.value = true
            _loginError.value = null
            val ok = sessionManager.guestLogin()
            if (!ok) _loginError.value = "Demo-Zugang konnte nicht hergestellt werden."
            _loggingIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }
}