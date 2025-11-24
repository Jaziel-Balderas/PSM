package Model.data

data class LikeResponse(
    val success: Boolean,
    val message: String,
    val liked: Boolean,
    val likeCount: Int
)
