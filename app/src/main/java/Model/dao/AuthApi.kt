package Model.dao

import retrofit2.http.Body
import retrofit2.http.POST
import Model.data.AuthResponse
import Model.data.LoginRequest
import Model.data.RegisterRequest

interface AuthApi {
    @POST("auth.php")
    suspend fun loginUser(@Body loginRequest: LoginRequest): AuthResponse

    @POST("register.php")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): AuthResponse
}