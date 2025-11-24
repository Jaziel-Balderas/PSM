package Model.dao

import retrofit2.http.Body
import retrofit2.http.POST
import Model.data.*
import retrofit2.http.PUT

interface AuthApi {
    @POST("auth.php")
    suspend fun loginUser(@Body loginRequest: LoginRequest): AuthResponse

    @POST("register.php")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): AuthResponse

    @PUT("update_profile.php") // Asumiendo que crearás este endpoint en PHP
    suspend fun sendUpdate(@Body request: UpdateProfileRequest): AuthResponse

}