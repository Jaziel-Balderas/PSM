package Model.dao

import androidx.room.*
import Model.database.PostEntity
import Model.database.DraftPostEntity
import Model.database.PendingPostEditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    // Posts cache
    @Query("SELECT * FROM posts_cache ORDER BY cached_at DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>
    
    @Query("SELECT * FROM posts_cache ORDER BY cached_at DESC")
    suspend fun getAllPosts(): List<PostEntity>
    
    @Query("SELECT * FROM posts_cache WHERE post_id = :postId")
    suspend fun getPostById(postId: Int): PostEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)
    
    @Query("UPDATE posts_cache SET likes_count = :likes, dislikes_count = :dislikes, user_vote = :vote WHERE post_id = :postId")
    suspend fun updateVotes(postId: Int, likes: Int, dislikes: Int, vote: Int?)
    
    @Query("DELETE FROM posts_cache WHERE cached_at < :timestamp")
    suspend fun deleteOldPosts(timestamp: Long)
    
    @Query("DELETE FROM posts_cache WHERE post_id = :postId")
    suspend fun deletePost(postId: Int)
}

@Dao
interface DraftPostDao {
    @Query("SELECT * FROM draft_posts WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getDraftsFlow(userId: Int): Flow<List<DraftPostEntity>>
    
    @Query("SELECT * FROM draft_posts WHERE user_id = :userId ORDER BY updated_at DESC")
    suspend fun getDrafts(userId: Int): List<DraftPostEntity>
    
    @Query("SELECT * FROM draft_posts WHERE draft_id = :draftId")
    suspend fun getDraftById(draftId: Long): DraftPostEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: DraftPostEntity): Long
    
    @Update
    suspend fun updateDraft(draft: DraftPostEntity)
    
    @Query("DELETE FROM draft_posts WHERE draft_id = :draftId")
    suspend fun deleteDraft(draftId: Long)
    
    @Query("DELETE FROM draft_posts WHERE user_id = :userId")
    suspend fun deleteAllDrafts(userId: Int)
}

@Dao
interface PendingPostEditDao {
    @Query("SELECT * FROM pending_post_edits WHERE user_id = :userId ORDER BY updated_at DESC")
    fun getPendingEditsFlow(userId: Int): Flow<List<PendingPostEditEntity>>
    
    @Query("SELECT * FROM pending_post_edits WHERE user_id = :userId")
    suspend fun getPendingEdits(userId: Int): List<PendingPostEditEntity>
    
    @Query("SELECT * FROM pending_post_edits WHERE post_id = :postId")
    suspend fun getPendingEditByPostId(postId: Int): PendingPostEditEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingEdit(edit: PendingPostEditEntity)
    
    @Query("DELETE FROM pending_post_edits WHERE post_id = :postId")
    suspend fun deletePendingEdit(postId: Int)
    
    @Query("DELETE FROM pending_post_edits WHERE user_id = :userId")
    suspend fun deleteAllPendingEdits(userId: Int)
}
