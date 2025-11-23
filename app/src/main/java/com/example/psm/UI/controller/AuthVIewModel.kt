package com.example.psm.UI.controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel // NECESARIO para que Android lo reconozca
import androidx.lifecycle.viewModelScope
import Model.repository.UserRepository
import kotlinx.coroutines.launch

class AuthVIewModel(private val userRepository: UserRepository) : ViewModel() {

    // PATRÓN OBSERVER: LiveData como Sujeto Observable
    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    // Función del Controlador
    fun login(username: String, password: String) {

        viewModelScope.launch {
            try {
                // Llama al Modelo (UserRepository)
                val response = userRepository.login(username, password)

                if (response.success) {
                    _loginStatus.value = true
                } else {
                    _loginStatus.value = false
                    _errorMessage.value = response.message ?: "Usuario no encontrado"
                }
            } catch (e: Exception) {
                _loginStatus.value = false
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }
}