package Model.repository


import di.APIService
import Model.repository.SessionManager
import Model.data.LoginRequest
import Model.data.RegisterRequest
import Model.data.AuthResponse

class UserRepository {

    // Acceso al DAO Lógico (Singleton)
    private val authApi = APIService.authService

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

}