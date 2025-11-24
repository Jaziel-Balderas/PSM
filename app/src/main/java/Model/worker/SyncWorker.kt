package Model.worker

import Model.dao.PostApi
import Model.database.AppDatabase
import Model.repository.ConnectivityObserver
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val database = AppDatabase.getInstance(context)
    private val pendingActionDao = database.pendingActionDao()
    private val gson = Gson()
    
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "PSM_SYNC_WORK"
    }
    
    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker iniciado")
        
        // Verificar conectividad
        if (!ConnectivityObserver.checkConnectivity()) {
            Log.d(TAG, "Sin conectividad, sync cancelado")
            return Result.retry()
        }
        
        try {
            val pendingActions = pendingActionDao.getPendingActions()
            Log.d(TAG, "Acciones pendientes: ${pendingActions.size}")
            
            if (pendingActions.isEmpty()) {
                return Result.success()
            }
            
            // Inicializar Retrofit
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/PSM/BD/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            
            val postApi = retrofit.create(PostApi::class.java)
            
            var successCount = 0
            var failCount = 0
            
            for (action in pendingActions) {
                try {
                    // Actualizar estado a SYNCING
                    pendingActionDao.updateStatus(action.id, "SYNCING")
                    
                    when (action.actionType) {
                        "CREATE_POST" -> {
                            @Suppress("UNCHECKED_CAST")
                            val payload = gson.fromJson(action.jsonPayload, Map::class.java) as Map<String, Any?>
                            
                            val userId = (payload["user_id"] as? Double)?.toInt() ?: 0
                            val title = payload["title"] as? String
                            val content = payload["content"] as? String ?: ""
                            val location = payload["location"] as? String
                            val isPublic = (payload["is_public"] as? Double)?.toInt() ?: 1
                            
                            // Construir multipart request con RequestBody
                            val userIdPart = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
                            val isPublicPart = isPublic.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            val titlePart = title?.toRequestBody("text/plain".toMediaTypeOrNull())
                            val locationPart = location?.toRequestBody("text/plain".toMediaTypeOrNull())
                            
                            // TODO: Manejar imágenes base64
                            val response = postApi.createPost(
                                userId = userIdPart,
                                title = titlePart,
                                content = contentPart,
                                location = locationPart,
                                isPublic = isPublicPart,
                                images = null
                            )
                            
                            if (response.isSuccessful && response.body()?.success == true) {
                                pendingActionDao.deleteAction(action)
                                successCount++
                                Log.d(TAG, "Post sincronizado exitosamente")
                            } else {
                                throw Exception("Respuesta no exitosa: ${response.code()}")
                            }
                        }
                        
                        "VOTE_POST" -> {
                            @Suppress("UNCHECKED_CAST")
                            val payload = gson.fromJson(action.jsonPayload, Map::class.java) as Map<String, Any?>
                            
                            val postId = (payload["postId"] as? Double)?.toInt() ?: 0
                            val userId = (payload["userId"] as? Double)?.toInt() ?: 0
                            val vote = (payload["vote"] as? Double)?.toInt() ?: 0
                            
                            val response = postApi.votePost(postId, userId, vote)
                            
                            if (response.isSuccessful && response.body()?.success == true) {
                                pendingActionDao.deleteAction(action)
                                successCount++
                                Log.d(TAG, "Voto sincronizado exitosamente")
                            } else {
                                throw Exception("Respuesta no exitosa: ${response.code()}")
                            }
                        }
                        
                        else -> {
                            Log.w(TAG, "Tipo de acción desconocido: ${action.actionType}")
                            pendingActionDao.deleteAction(action)
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error sincronizando acción ${action.id}: ${action.actionType}", e)
                    
                    // Incrementar contador de reintentos
                    val newRetryCount = action.retryCount + 1
                    if (newRetryCount > 3) {
                        // Marcar como fallido después de 3 intentos
                        pendingActionDao.updateStatus(action.id, "FAILED")
                        Log.e(TAG, "Acción ${action.id} marcada como FAILED después de 3 intentos")
                    } else {
                        // Actualizar contador y resetear a PENDING
                        pendingActionDao.updateAction(action.copy(retryCount = newRetryCount, status = "PENDING"))
                    }
                    failCount++
                }
            }
            
            Log.d(TAG, "Sincronización completada: $successCount exitosas, $failCount fallidas")
            
            return if (failCount > 0) {
                Result.retry()
            } else {
                Result.success()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error general en SyncWorker", e)
            return Result.retry()
        }
    }
}
