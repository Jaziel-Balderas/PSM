package Model.repository

import Model.dao.PostApi
import Model.data.LikeResponse
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
            // Crear partes del formulario multipart
            val userIdPart = userId.toRequestBody("text/plain".toMediaTypeOrNull())
            val titlePart = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val locationPart = location.toRequestBody("text/plain".toMediaTypeOrNull())
            val isPublicPart = if (isPublic) "1" else "0"
            val isPublicPartBody = isPublicPart.toRequestBody("text/plain".toMediaTypeOrNull())

            // Crear partes para cada imagen
            val imageParts = imageFiles.mapIndexed { index, file ->
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("images[]", file.name, requestFile)
            }

            val response = api.createPost(
                userIdPart,
                titlePart,
                descriptionPart,
                locationPart,
                isPublicPartBody,
                imageParts
            )

            if (response.isSuccessful) {
                response.body()
            } else {
                PostResponse(false, "Error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            PostResponse(false, "Error de red: ${e.message}")
        }
    }
    
    suspend fun getPosts(currentUserId: Int = 0, userId: Int? = null, limit: Int = 50, offset: Int = 0): PostsResponse? {
        return try {
            Log.d("PostRepository", "getPosts called with currentUserId=$currentUserId, userId=$userId")
            val response = api.getPosts(currentUserId, userId, limit, offset)
            
            Log.d("PostRepository", "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("PostRepository", "Response body: success=${body?.success}, posts count=${body?.posts?.size}")
                body
            } else {
                val errorMsg = "Error: ${response.code()} - ${response.message()}"
                Log.e("PostRepository", errorMsg)
                PostsResponse(false, errorMsg, emptyList(), 0)
            }
        } catch (e: Exception) {
            val errorMsg = "Error de red: ${e.message}"
            Log.e("PostRepository", errorMsg, e)
            PostsResponse(false, errorMsg, emptyList(), 0)
        }
    }
    
    suspend fun likePost(postId: Int, userId: Int): LikeResponse? {
        return try {
            val response = api.likePost(postId, userId)
            
            if (response.isSuccessful) {
                response.body()
            } else {
                LikeResponse(false, "Error: ${response.code()} - ${response.message()}", false, 0)
            }
        } catch (e: Exception) {
            LikeResponse(false, "Error de red: ${e.message}", false, 0)
        }
    }
}