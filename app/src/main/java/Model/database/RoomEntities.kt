package Model.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "posts_cache")
data class PostEntity(
    @PrimaryKey
    @ColumnInfo(name = "post_id")
    val postId: Int,
    
    @ColumnInfo(name = "user_id")
    val userId: Int,
    
    @ColumnInfo(name = "title")
    val title: String?,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "location")
    val location: String?,
    
    @ColumnInfo(name = "is_public")
    val isPublic: Int,
    
    @ColumnInfo(name = "likes_count")
    val likesCount: Int,
    
    @ColumnInfo(name = "dislikes_count")
    val dislikesCount: Int,
    
    @ColumnInfo(name = "user_vote")
    val userVote: Int?,  // null, 1, or -1
    
    @ColumnInfo(name = "images_json")
    val imagesJson: String?,  // JSON array of base64 images
    
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    
    @ColumnInfo(name = "cached_at")
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "action_type")
    val actionType: String,  // "CREATE_POST", "VOTE_POST", "UPDATE_PROFILE", "UPDATE_POST"
    
    @ColumnInfo(name = "json_payload")
    val jsonPayload: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    
    @ColumnInfo(name = "status")
    val status: String = "PENDING"  // PENDING, SYNCING, FAILED
)

@Entity(tableName = "draft_posts")
data class DraftPostEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "draft_id")
    val draftId: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Int,
    
    @ColumnInfo(name = "title")
    val title: String?,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "location")
    val location: String?,
    
    @ColumnInfo(name = "is_public")
    val isPublic: Int,
    
    @ColumnInfo(name = "images_json")
    val imagesJson: String?,  // JSON array of base64 images
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_post_edits")
data class PendingPostEditEntity(
    @PrimaryKey
    @ColumnInfo(name = "post_id")
    val postId: Int,
    
    @ColumnInfo(name = "user_id")
    val userId: Int,
    
    @ColumnInfo(name = "title")
    val title: String?,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "location")
    val location: String?,
    
    @ColumnInfo(name = "is_public")
    val isPublic: Int,
    
    @ColumnInfo(name = "images_json")
    val imagesJson: String?,
    
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
