package Model.repository

import Model.data.User

object SessionManager {

    // La información del usuario logueado.
    var currentUser: User? = null
        private set // puede modificar la sesión

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun createSession(user: User) {
        currentUser = user
        //guardar el ID
    }

    fun clearSession() {
        currentUser = null
        //borrar los datos de SharedPreferences
    }
}