package com.example.psm.UI.controller

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Model.repository.UserRepository
import kotlinx.coroutines.launch
import Model.data.RegisterRequest

class AuthVIewModel(private val userRepository: UserRepository) : ViewModel() {

    // PATRÓN OBSERVER: LiveData se utilizar para hacer una notificación de
    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> = _loginStatus

    private val _registerStatus = MutableLiveData<Boolean>()
    val registerStatus: LiveData<Boolean> = _registerStatus
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

    //Función del Controlador para manejar el evento de Registro e implementa Model-Controller y Observer.

    fun register(request: RegisterRequest) {
        viewModelScope.launch { // Esto evita que la aplicación se congele (REFRESH DE DATOS)
            try {
                val response = userRepository.register(request)
                if (response.success) {
                    _registerStatus.value = true
                    _errorMessage.value = ""
                } else {
                    _registerStatus.value = false
                    _errorMessage.value = response.message ?: "Usuario/email ya registrado"
                }
            } catch (e: Exception) {
                _registerStatus.value = false
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }
}