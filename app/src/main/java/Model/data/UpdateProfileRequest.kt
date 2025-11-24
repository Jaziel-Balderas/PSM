package Model.data


data class UpdateProfileRequest(
    val userId: Int,
    val nameuser: String,
    val lastnames: String,
    val email: String,
    val phone: String,
    val direccion: String?,
    val username: String,
    val newPassword: String? = null,
    val profileImageBase64: String? = null
)