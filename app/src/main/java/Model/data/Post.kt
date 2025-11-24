package Model.data

data class Post(
    val postId: String? = null,
    val userId: String = "",
    val username: String? = null,
    val nameuser: String? = null,
    val lastnames: String? = null,
    val title: String = "",
    val description: String = "", // maps to DB 'content'
    val location: String? = null,
    val images: List<PostImage> = emptyList(), // multi-image support
    val isPublic: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val profileImageBase64: String? = null,
    val likesCount: Int = 0,
    val dislikesCount: Int = 0,
    val userVote: Int? = null, // 1 like, -1 dislike, null none
    val commentsCount: Int = 0,
    val isFavorite: Boolean = false
)

data class PostResponse(
    val success: Boolean,
    val message: String,
    val postId: String? = null
)

data class PostsResponse(
    val success: Boolean,
    val posts: List<Post>,
    val count: Int,
    val limit: Int? = null,
    val offset: Int? = null,
    val message: String? = null
)

data class UserInfo(
    val nameuser: String,
    val lastnames: String,
    val username: String,
    val profileImageBase64: String?
)

data class PostImage(
    val imageId: Int,
    val description: String?,
    val base64: String
)

data class VoteResponse(
    val success: Boolean,
    val message: String,
    val post: VotePostData?
)

data class VotePostData(
    val post_id: Int,
    val likes_count: Int,
    val dislikes_count: Int,
    val user_vote: Int?
)

data class FavoriteResponse(
    val success: Boolean,
    val message: String? = null,
    val isFavorite: Boolean = false,
    val postId: Int = 0
)