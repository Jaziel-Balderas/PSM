package Model.data

data class AuthResponse(

    // Indica si la operación fue exitosa
    val success: Boolean,

    // Un mensaje del servidor
    val message: String? = null,

    //Si el login es exitoso
    val user: User? = null
)
