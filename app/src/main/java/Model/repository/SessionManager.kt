package Model.repository

import Model.data.User
import Model.database.DatabaseHelper
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SessionManager {

    private const val TAG = "SessionManager"

    // La información del usuario logueado.
    var currentUser: User? = null
        private set // puede modificar la sesión

    private var sharedPreferences: SharedPreferences? = null
    private var databaseHelper: DatabaseHelper? = null
    
    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "userId"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"

    val isLoggedIn: Boolean
        get() = sharedPreferences?.getBoolean(KEY_IS_LOGGED_IN, false) ?: false

    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "SharedPreferences inicializado")
        }
        if (databaseHelper == null) {
            databaseHelper = DatabaseHelper(context.applicationContext)
            Log.d(TAG, "DatabaseHelper inicializado")
        }
    }

    fun createSession(user: User) {
        currentUser = user
        Log.d(TAG, "Creando sesión para usuario: ${user.username} (ID: ${user.userId})")
        
        // Guardar en SharedPreferences
        sharedPreferences?.edit()?.apply {
            putString(KEY_USER_ID, user.userId.toString())
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Log.d(TAG, "Sesión guardada en SharedPreferences")
        
        // Guardar/actualizar en SQLite
        databaseHelper?.let { db ->
            val existingUser = db.getUserById(user.userId)
            if (existingUser != null) {
                val updated = db.updateUser(user)
                Log.d(TAG, "Usuario actualizado en SQLite: $updated filas afectadas")
            } else {
                val insertId = db.insertUser(user)
                Log.d(TAG, "Usuario insertado en SQLite con ID: $insertId")
            }
        }
    }

    fun loadSessionFromCache(): Boolean {
        val userId = getUserId()?.toIntOrNull()
        Log.d(TAG, "Intentando cargar sesión desde cache. UserID: $userId, isLoggedIn: $isLoggedIn")
        
        if (userId == null) {
            Log.d(TAG, "No se encontró UserID en SharedPreferences")
            return false
        }
        
        databaseHelper?.let { db ->
            val cachedUser = db.getUserById(userId)
            if (cachedUser != null && isLoggedIn) {
                currentUser = cachedUser
                Log.d(TAG, "Sesión restaurada desde SQLite para: ${cachedUser.username}")
                return true
            } else {
                Log.d(TAG, "No se encontró usuario en SQLite o sesión no está activa")
            }
        }
        return false
    }

    fun clearSession() {
        val userId = currentUser?.userId
        val username = currentUser?.username
        currentUser = null
        
        Log.d(TAG, "Cerrando sesión de usuario: $username (ID: $userId)")
        
        // Limpiar SharedPreferences
        sharedPreferences?.edit()?.clear()?.apply()
        Log.d(TAG, "SharedPreferences limpiado")
        
        // Eliminar de SQLite
        userId?.let { 
            val deleted = databaseHelper?.deleteUser(it)
            Log.d(TAG, "Usuario eliminado de SQLite: $deleted fila(s)")
        }
    }
    
    fun getUserId(): String? {
        return sharedPreferences?.getString(KEY_USER_ID, null)
    }
}