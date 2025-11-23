package Model.data

data class RegisterRequest(
    val nameuser: String,
    val lastnames: String,
    val username: String,
    val password: String,
    val email: String,
    val phone: String,
    val direccion: String? = null,
    val profile_image_url: String? = null
)
