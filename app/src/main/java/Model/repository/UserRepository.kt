package Model.repository


import di.APIService
import Model.repository.SessionManager
import Model.data.LoginRequest
import Model.data.AuthResponse

class UserRepository {

    // 1. Acceso al DAO Lógico (Singleton de Retrofit)
    private val authApi = APIService.authService

    // FUNCIÓN PRINCIPAL DE AUTENTICACIÓN
    suspend fun login(user: String, password: String): AuthResponse {

        val request = LoginRequest(user, password)

        // 3. Llama a la API (Lógica de red)
        val response = authApi.loginUser(request)

        // 4. Lógica de Negocio (Aquí es donde se aplica la restricción de NO usar token)
        if (response.success && response.user != null) {
            // Si es exitoso, usa el Singleton SessionManager para guardar al usuario
            SessionManager.createSession(response.user)
        }

        // 5. Devuelve la respuesta al Controller (ViewModel)
        return response
    }

}