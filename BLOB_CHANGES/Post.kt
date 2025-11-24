package Model.data

data class Post(
    val postId: String? = null,
    val userId: String,
    val username: String? = null,
    val title: String,
    val description: String,
    val location: String? = null,
    val imageUrls: List<String> = emptyList(), // Base64 strings
    val isPublic: Boolean = true,
    val createdAt: String? = null,
    val profileResId: Int? = null,
    val profileImageBase64: String? = null, // Imagen de perfil en base64
    var likeCount: Int = 0,
    var isLiked: Boolean = false
)

data class PostResponse(
    val success: Boolean,
    val message: String,
    val postId: String? = null
)

data class PostsResponse(
    val success: Boolean,
    val message: String,
    val posts: List<Post>,
    val count: Int
)

data class UserInfo(
    val nameuser: String,
    val lastnames: String,
    val username: String,
    val profileImageBase64: String?
)
