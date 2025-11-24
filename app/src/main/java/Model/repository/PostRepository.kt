package Model.repository

import Model.dao.PostApi
import Model.data.VoteResponse
import Model.data.PostResponse
import Model.data.PostsResponse
import di.AppModule
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import android.util.Log

class PostRepository {
    private val api = AppModule.retrofitInstance.create(PostApi::class.java)

    suspend fun createPost(
        userId: String,
        title: String,
        description: String,
        location: String,
        isPublic: Boolean,
        imageFiles: List<File>
    ): PostResponse? {
        return try {
            // Log ANTES de crear RequestBody
            Log.d("PostRepository", "=== DATOS RECIBIDOS ===")
            Log.d("PostRepository", "userId: '$userId'")
            Log.d("PostRepository", "title: '$title'")
            Log.d("PostRepository", "description (content): '$description' (length=${description.length})")
            Log.d("PostRepository", "location: '$location'")
            Log.d("PostRepository", "isPublic: $isPublic")
            Log.d("PostRepository", "imageFiles: ${imageFiles.size}")

            // Validación adicional ANTES de enviar
            if (userId.isEmpty() || userId == "-1") {
                Log.e("PostRepository", "ERROR: userId inválido")
                return PostResponse(false, "Error: Usuario no autenticado")
            }
            
            if (description.isEmpty()) {
                Log.e("PostRepository", "ERROR: description (content) vacío")
                return PostResponse(false, "Error: La descripción es obligatoria")
            }

            // Crear partes del formulario multipart usando RequestBody para campos de texto
            val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            val titlePart = if (title.isNotEmpty()) {
                title.toRequestBody("text/plain".toMediaTypeOrNull())
            } else null
            val contentPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val locationPart = if (location.isNotEmpty()) {
                location.toRequestBody("text/plain".toMediaTypeOrNull())
            } else null
            val isPublicPart = (if (isPublic) "1" else "0").toRequestBody("text/plain".toMediaTypeOrNull())

            // Crear partes para cada imagen
            val imageParts = if (imageFiles.isNotEmpty()) {
                imageFiles.map { file ->
                    Log.d("PostRepository", "Imagen: ${file.name} (${file.length()} bytes)")
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images[]", file.name, requestFile)
                }
            } else null

            Log.d("PostRepository", "Ejecutando API call...")
            
            val response = api.createPost(
                userIdPart,
                titlePart,
                contentPart,
                locationPart,
                isPublicPart,
                imageParts
            )
            
            Log.d("PostRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d("PostRepository", "Response body: ${body?.message}")
                body
            } else {
                val errorMsg = "Error: ${response.code()} - ${response.message()}"
                Log.e("PostRepository", errorMsg)
                PostResponse(false, errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error de red: ${e.message}"
            Log.e("PostRepository", errorMsg, e)
            PostResponse(false, errorMsg)
        }
    }
    
    suspend fun getPosts(userId: Int, limit: Int = 50, offset: Int = 0): PostsResponse? {
        return try {
            Log.d("PostRepository", "getPosts called with userId=$userId limit=$limit offset=$offset")
            val response = api.getPosts(userId, limit, offset)
            
            Log.d("PostRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("PostRepository", "Response body: success=${body?.success}, posts count=${body?.posts?.size}")
                body
            } else {
                val errorMsg = "Error: ${response.code()} - ${response.message()}"
                Log.e("PostRepository", errorMsg)
                PostsResponse(false, emptyList(), 0, limit, offset, errorMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error de red: ${e.message}"
            Log.e("PostRepository", errorMsg, e)
            PostsResponse(false, emptyList(), 0, limit, offset, errorMsg)
        }
    }
    
    suspend fun votePost(postId: Int, userId: Int, vote: Int): VoteResponse? {
        return try {
            val response = api.votePost(postId, userId, vote)
            
            if (response.isSuccessful) {
                response.body()
            } else {
                VoteResponse(false, "Error: ${response.code()} - ${response.message()}", null)
            }
        } catch (e: Exception) {
            VoteResponse(false, "Error de red: ${e.message}", null)
        }
    }
}