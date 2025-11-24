package Model.data

data class User(
    val userId: Int, // Coincide con user_id
    val nameuser: String, // Coincide con nameuser
    val lastnames: String, // Coincide con lastnames
    val username: String, // Coincide con username
    val email: String, // Coincide con email
    val phone: String, // Coincide con phone (si lo cambias a VARCHAR)
    val direccion: String? = null, // Coincide con direccion
    val profile_image_url: String? = null // Coincide con profile_image_url (si lo ajustas)

)