package Model.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts_cache ORDER BY created_at DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>
    
    @Query("SELECT * FROM posts_cache ORDER BY created_at DESC")
    suspend fun getAllPosts(): List<PostEntity>
    
    @Query("SELECT * FROM posts_cache WHERE post_id = :postId")
    suspend fun getPostById(postId: Int): PostEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)
    
    @Update
    suspend fun updatePost(post: PostEntity)
    
    @Query("UPDATE posts_cache SET likes_count = :likes, dislikes_count = :dislikes, user_vote = :userVote WHERE post_id = :postId")
    suspend fun updateVotes(postId: Int, likes: Int, dislikes: Int, userVote: Int?)
    
    @Delete
    suspend fun deletePost(post: PostEntity)
    
    @Query("DELETE FROM posts_cache")
    suspend fun clearAllPosts()
    
    @Query("DELETE FROM posts_cache WHERE cached_at < :timestamp")
    suspend fun deleteOldPosts(timestamp: Long)
}

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions WHERE status = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingActions(): List<PendingActionEntity>
    
    @Query("SELECT * FROM pending_actions ORDER BY created_at DESC")
    fun getAllActionsFlow(): Flow<List<PendingActionEntity>>
    
    @Insert
    suspend fun insertAction(action: PendingActionEntity): Long
    
    @Update
    suspend fun updateAction(action: PendingActionEntity)
    
    @Delete
    suspend fun deleteAction(action: PendingActionEntity)
    
    @Query("DELETE FROM pending_actions WHERE status = 'FAILED' AND created_at < :timestamp")
    suspend fun deleteOldFailedActions(timestamp: Long)
    
    @Query("UPDATE pending_actions SET status = :status WHERE id = :actionId")
    suspend fun updateStatus(actionId: Long, status: String)
}
