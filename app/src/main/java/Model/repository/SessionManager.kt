package Model.repository

import Model.data.User
import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    // La información del usuario logueado.
    var currentUser: User? = null
        private set // puede modificar la sesión

    private var sharedPreferences: SharedPreferences? = null
    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "userId"

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    fun createSession(user: User) {
        currentUser = user
        // Guardar el ID en SharedPreferences
        sharedPreferences?.edit()?.apply {
            putString(KEY_USER_ID, user.userId.toString())
            apply()
        }
    }

    fun clearSession() {
        currentUser = null
        // Borrar los datos de SharedPreferences
        sharedPreferences?.edit()?.clear()?.apply()
    }
    
    fun getUserId(): String? {
        return sharedPreferences?.getString(KEY_USER_ID, null)
    }
}