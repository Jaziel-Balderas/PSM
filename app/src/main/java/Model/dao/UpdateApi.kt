package Model.dao

import Model.data.AuthResponse
import Model.data.UpdateProfileRequest
import retrofit2.http.Body
import retrofit2.http.PUT // Usamos PUT para la actualización

interface UpdateApi {

    @PUT("update_profile.php")
    suspend fun sendUpdate(@Body request: UpdateProfileRequest): AuthResponse
}