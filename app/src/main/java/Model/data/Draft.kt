package Model.data

data class Draft(
    val id: Long = 0,
    val userId: Int,
    val title: String? = null,
    val content: String? = null,
    val imageBase64: String? = null,
    val location: String? = null,
    val isPublic: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
