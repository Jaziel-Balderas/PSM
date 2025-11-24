package Model.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import Model.data.User

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "psm_local.db"
        private const val DATABASE_VERSION = 1
        
        // Tabla de usuarios
        private const val TABLE_USERS = "users_cache"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_NAME = "nameuser"
        private const val COLUMN_LASTNAME = "lastnames"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PHONE = "phone"
        private const val COLUMN_ADDRESS = "direccion"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PROFILE_IMAGE = "profile_image_url"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_LASTNAME TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_PHONE TEXT NOT NULL,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_USERNAME TEXT NOT NULL,
                $COLUMN_PROFILE_IMAGE TEXT
            )
        """.trimIndent()
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    fun insertUser(user: User): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, user.userId)
            put(COLUMN_NAME, user.nameuser)
            put(COLUMN_LASTNAME, user.lastnames)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_ADDRESS, user.direccion)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_PROFILE_IMAGE, user.profile_image_url)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUserById(userId: Int): User? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                nameuser = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                lastnames = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LASTNAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EMAIL)),
                phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE)),
                direccion = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USERNAME)),
                profile_image_url = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROFILE_IMAGE))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    fun updateUser(user: User): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, user.nameuser)
            put(COLUMN_LASTNAME, user.lastnames)
            put(COLUMN_EMAIL, user.email)
            put(COLUMN_PHONE, user.phone)
            put(COLUMN_ADDRESS, user.direccion)
            put(COLUMN_USERNAME, user.username)
            put(COLUMN_PROFILE_IMAGE, user.profile_image_url)
        }
        return db.update(TABLE_USERS, values, "$COLUMN_USER_ID = ?", arrayOf(user.userId.toString()))
    }

    fun deleteUser(userId: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_USERS, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
    }

    fun clearAllUsers() {
        val db = writableDatabase
        db.delete(TABLE_USERS, null, null)
    }
}
