package Model.repository


import di.APIService
import Model.repository.SessionManager
import Model.data.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import Model.dao.UpdateApi

import di.ApiServiceProvider

class UserRepository {

    // Acceso al DAO Lógico (Singleton)
    private val authApi = APIService.authService
    private val updateApi = ApiServiceProvider.updateService
    private val _currentUserProfile = MutableLiveData<User?>()
    private val currentUserProfile: LiveData<User?> = _currentUserProfile


    /*Carga el usuario actual del Singleton SessionManager y notifica a los Observers.
      Es llamado por el ViewModel (Controller) para forzar el refresh de la vista.*/

    fun loadProfile() {
        SessionManager.currentUser?.let {
            _currentUserProfile.value = it.copy() // Fuerza nueva instancia
        } ?: run {
            _currentUserProfile.value = null
        }
    }

    fun getCurrentUserProfile(): LiveData<User?> {
        // Inicializa la carga para que el Profile Fragment vea los datos al inicio
        loadProfile()
        return currentUserProfile
    }

    // AUTENTICAR EL USUARIO
    suspend fun login(user: String, password: String): AuthResponse {

        val request = LoginRequest(user, password)

        // 3. Llama a la API
        val response = authApi.loginUser(request)

        if (response.success && response.user != null) {
            // Si es exitoso, usa el Singleton SessionManager para guardar al usuario
            SessionManager.createSession(response.user)
        }

        // 5. Devuelve la respuesta al Controller
        return response
    }

    suspend fun register(request: RegisterRequest): AuthResponse {
        return authApi.registerUser(request)
    }

    suspend fun updateProfile(request: UpdateProfileRequest): AuthResponse {
        val response = updateApi.sendUpdate(request) // Se comunica con update_profile.php

        if (response.success && response.user != null) {
            // El servidor devolvió el nuevo objeto 'User' (datos frescos)
            val newUser = response.user.copy() // Fuerza nueva instancia
            SessionManager.createSession(newUser)
            _currentUserProfile.postValue(newUser) // Notifica a los Observers
        }
        return response
    }
}
