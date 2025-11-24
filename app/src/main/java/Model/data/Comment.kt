package Model.data

data class Comment(
    val commentId: Int = 0,
    val postId: Int = 0,
    val userId: Int = 0,
    val commentText: String = "",
    val likesCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val username: String = "",
    val nameuser: String = "",
    val lastnames: String = "",
    val profileImageBase64: String? = null,
    val userLiked: Boolean = false,
    val repliesCount: Int = 0
)

data class CommentResponse(
    val success: Boolean,
    val message: String? = null,
    val comment: Comment? = null
)

data class CommentsResponse(
    val success: Boolean,
    val count: Int = 0,
    val comments: List<Comment> = emptyList(),
    val message: String? = null
)

data class CommentLikeResponse(
    val success: Boolean,
    val message: String? = null,
    val liked: Boolean = false,
    val likesCount: Int = 0,
    val commentId: Int = 0
)

data class Reply(
    val replyId: Int = 0,
    val commentId: Int = 0,
    val userId: Int = 0,
    val replyText: String = "",
    val createdAt: String = "",
    val username: String = "",
    val nameuser: String = "",
    val lastnames: String = "",
    val profileImageBase64: String? = null
)

data class ReplyResponse(
    val success: Boolean,
    val message: String? = null,
    val reply: Reply? = null
)

data class RepliesResponse(
    val success: Boolean,
    val count: Int = 0,
    val replies: List<Reply> = emptyList(),
    val message: String? = null
)