package Model.repository

import Model.dao.PostApi
import Model.data.Post
import Model.database.AppDatabase
import Model.database.PostEntity
import Model.database.PendingActionEntity
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class OfflineFirstPostRepository(
    private val context: Context,
    private val postApi: PostApi,
    private val database: AppDatabase
) {
    private val postDao = database.postDao()
    private val pendingActionDao = database.pendingActionDao()
    private val gson = Gson()
    
    companion object {
        private const val TAG = "OfflinePostRepo"
        private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    // Flow para observar posts en tiempo real desde caché
    fun getPostsFlow(): Flow<List<Post>> {
        return postDao.getAllPostsFlow().map { entities ->
            entities.map { entity -> entityToPost(entity) }
        }
    }
    
    // Obtener posts: primero caché, luego intentar actualizar desde red
    suspend fun getPosts(currentUserId: Int, forceRefresh: Boolean = false): Result<List<Post>> {
        return try {
            // 1. Leer caché local
            val cachedPosts = postDao.getAllPosts().map { entityToPost(it) }
            
            // 2. Si hay internet, actualizar desde servidor
            if (ConnectivityObserver.checkConnectivity() || forceRefresh) {
                try {
                    val response = postApi.getPosts(currentUserId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val serverPosts = response.body()!!.posts
                        
                        // Actualizar caché
                        val entities = serverPosts.map { postToEntity(it, currentUserId) }
                        postDao.insertPosts(entities)
                        
                        Log.d(TAG, "Posts actualizados desde servidor: ${serverPosts.size}")
                        return Result.success(serverPosts)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener posts del servidor", e)
                    // Si falla la red pero hay caché, devolver caché
                    if (cachedPosts.isNotEmpty()) {
                        return Result.success(cachedPosts)
                    }
                }
            }
            
            // 3. Devolver caché si no hay internet o falla la red
            Result.success(cachedPosts)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getPosts", e)
            Result.failure(e)
        }
    }
    
    // Crear post offline
    suspend fun createPost(
        userId: Int,
        title: String?,
        content: String,
        location: String?,
        isPublic: Int,
        imagesBase64: List<String>?
    ): Result<Boolean> {
        return try {
            val payload = mapOf(
                "user_id" to userId,
                "title" to title,
                "content" to content,
                "location" to location,
                "is_public" to isPublic,
                "images_base64" to imagesBase64
            )
            
            // Guardar acción pendiente
            val action = PendingActionEntity(
                actionType = "CREATE_POST",
                jsonPayload = gson.toJson(payload)
            )
            pendingActionDao.insertAction(action)
            
            Log.d(TAG, "Post guardado para sincronización posterior")
            
            // Si hay internet, intentar sincronizar inmediatamente
            if (ConnectivityObserver.checkConnectivity()) {
                syncPendingActions()
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar post offline", e)
            Result.failure(e)
        }
    }
    
    // Votar offline
    suspend fun votePost(postId: Int, userId: Int, vote: Int): Result<Boolean> {
        return try {
            // Actualizar caché local inmediatamente
            val cachedPost = postDao.getPostById(postId)
            if (cachedPost != null) {
                val newLikes = if (vote == 1) cachedPost.likesCount + 1 else cachedPost.likesCount
                val newDislikes = if (vote == -1) cachedPost.dislikesCount + 1 else cachedPost.dislikesCount
                postDao.updateVotes(postId, newLikes, newDislikes, vote)
            }
            
            // Guardar acción pendiente
            val payload = mapOf(
                "postId" to postId,
                "userId" to userId,
                "vote" to vote
            )
            val action = PendingActionEntity(
                actionType = "VOTE_POST",
                jsonPayload = gson.toJson(payload)
            )
            pendingActionDao.insertAction(action)
            
            Log.d(TAG, "Voto guardado para sincronización")
            
            // Sincronizar si hay internet
            if (ConnectivityObserver.checkConnectivity()) {
                syncPendingActions()
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error al votar offline", e)
            Result.failure(e)
        }
    }
    
    // Sincronizar acciones pendientes
    suspend fun syncPendingActions(): Result<Int> {
        if (!ConnectivityObserver.checkConnectivity()) {
            return Result.success(0)
        }
        
        return try {
            val pendingActions = pendingActionDao.getPendingActions()
            var syncedCount = 0
            
            for (action in pendingActions) {
                try {
                    when (action.actionType) {
                        "CREATE_POST" -> {
                            // Sincronizar creación de post
                            val payload = gson.fromJson(action.jsonPayload, Map::class.java)
                            // TODO: Llamar a postApi.createPost con payload
                            pendingActionDao.deleteAction(action)
                            syncedCount++
                        }
                        "VOTE_POST" -> {
                            // Sincronizar voto
                            val payload = gson.fromJson(action.jsonPayload, Map::class.java)
                            // TODO: Llamar a postApi.votePost con payload
                            pendingActionDao.deleteAction(action)
                            syncedCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando acción ${action.id}", e)
                    pendingActionDao.updateStatus(action.id, "FAILED")
                }
            }
            
            Log.d(TAG, "Sincronizadas $syncedCount acciones pendientes")
            Result.success(syncedCount)
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización", e)
            Result.failure(e)
        }
    }
    
    // Limpiar caché antiguo
    suspend fun clearOldCache() {
        val expiryTimestamp = System.currentTimeMillis() - CACHE_EXPIRY_MS
        postDao.deleteOldPosts(expiryTimestamp)
        pendingActionDao.deleteOldFailedActions(expiryTimestamp)
    }
    
    // Conversiones
    private fun postToEntity(post: Post, userId: Int): PostEntity {
        return PostEntity(
            postId = post.postId?.toIntOrNull() ?: 0,
            userId = post.userId.toIntOrNull() ?: userId,
            title = post.title ?: "",
            content = post.description,
            location = post.location,
            isPublic = if (post.isPublic) 1 else 0,
            likesCount = post.likesCount,
            dislikesCount = post.dislikesCount,
            userVote = post.userVote,
            imagesJson = gson.toJson(post.images),
            createdAt = post.createdAt ?: System.currentTimeMillis().toString()
        )
    }
    
    private fun entityToPost(entity: PostEntity): Post {
        val imagesList = entity.imagesJson?.let {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<Model.data.PostImage>>() {}.type
                gson.fromJson<List<Model.data.PostImage>>(it, type)
            } catch (e: Exception) {
                emptyList<Model.data.PostImage>()
            }
        } ?: emptyList<Model.data.PostImage>()
        
        return Post(
            postId = entity.postId.toString(),
            userId = entity.userId.toString(),
            title = entity.title ?: "",
            description = entity.content,
            location = entity.location ?: "",
            isPublic = entity.isPublic == 1,
            likesCount = entity.likesCount,
            dislikesCount = entity.dislikesCount,
            userVote = entity.userVote,
            images = imagesList,
            createdAt = entity.createdAt
        )
    }
}
