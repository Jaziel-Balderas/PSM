package Model.repository

import Model.dao.CommentApi
import Model.data.*
import di.AppModule
import android.util.Log

class CommentRepository {
    private val api = AppModule.retrofitInstance.create(CommentApi::class.java)
    
    suspend fun getComments(postId: Int, userId: Int): CommentsResponse? {
        return try {
            Log.d("CommentRepository", "getComments: postId=$postId, userId=$userId")
            val response = api.getComments(postId, userId)
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("CommentRepository", "Success: ${body?.comments?.size} comments")
                body
            } else {
                Log.e("CommentRepository", "Error: ${response.code()} - ${response.message()}")
                CommentsResponse(false, 0, emptyList(), "Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception: ${e.message}", e)
            CommentsResponse(false, 0, emptyList(), "Error de red: ${e.message}")
        }
    }
    
    suspend fun createComment(postId: Int, userId: Int, commentText: String): CommentResponse? {
        return try {
            Log.d("CommentRepository", "createComment: postId=$postId, userId=$userId, text=$commentText")
            val response = api.createComment(postId, userId, commentText)
            
            if (response.isSuccessful) {
                val body = response.body()
                Log.d("CommentRepository", "Comment created: ${body?.comment?.commentId}")
                body
            } else {
                Log.e("CommentRepository", "Error: ${response.code()} - ${response.message()}")
                CommentResponse(false, "Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception: ${e.message}", e)
            CommentResponse(false, "Error de red: ${e.message}")
        }
    }
    
    suspend fun likeComment(commentId: Int, userId: Int): CommentLikeResponse? {
        return try {
            Log.d("CommentRepository", "likeComment: commentId=$commentId, userId=$userId")
            val response = api.likeComment(commentId, userId)
            
            if (response.isSuccessful) {
                response.body()
            } else {
                Log.e("CommentRepository", "Error: ${response.code()} - ${response.message()}")
                CommentLikeResponse(false, "Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CommentRepository", "Exception: ${e.message}", e)
            CommentLikeResponse(false, "Error de red: ${e.message}")
        }
    }
    
    suspend fun getReplies(commentId: Int): RepliesResponse? {
        return try {
            val response = api.getReplies(commentId)
            
            if (response.isSuccessful) {
                response.body()
            } else {
                RepliesResponse(false, 0, emptyList(), "Error: ${response.code()}")
            }
        } catch (e: Exception) {
            RepliesResponse(false, 0, emptyList(), "Error de red: ${e.message}")
        }
    }
    
    suspend fun createReply(commentId: Int, userId: Int, replyText: String): ReplyResponse? {
        return try {
            val response = api.createReply(commentId, userId, replyText)
            
            if (response.isSuccessful) {
                response.body()
            } else {
                ReplyResponse(false, "Error: ${response.code()}")
            }
        } catch (e: Exception) {
            ReplyResponse(false, "Error de red: ${e.message}")
        }
    }
}
