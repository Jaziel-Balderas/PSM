package Model.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import Model.data.Draft

class DraftManager private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "drafts.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_DRAFTS = "drafts"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_IMAGE_BASE64 = "image_base64"
        private const val COLUMN_IMAGE_URIS = "image_uris"
        private const val COLUMN_LOCATION = "location"
        private const val COLUMN_IS_PUBLIC = "is_public"
        private const val COLUMN_CREATED_AT = "created_at"
        private const val COLUMN_UPDATED_AT = "updated_at"

        @Volatile
        private var instance: DraftManager? = null

        fun init(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = DraftManager(context.applicationContext)
                        Log.d("DraftManager", "DraftManager inicializado")
                    }
                }
            }
        }

        fun getInstance(): DraftManager {
            return instance ?: throw IllegalStateException(
                "DraftManager debe ser inicializado con init(context) antes de usar getInstance()"
            )
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_DRAFTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID INTEGER NOT NULL,
                $COLUMN_TITLE TEXT,
                $COLUMN_CONTENT TEXT,
                $COLUMN_IMAGE_BASE64 TEXT,
                $COLUMN_IMAGE_URIS TEXT,
                $COLUMN_LOCATION TEXT,
                $COLUMN_IS_PUBLIC INTEGER DEFAULT 1,
                $COLUMN_CREATED_AT INTEGER NOT NULL,
                $COLUMN_UPDATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        
        db.execSQL(createTable)
        Log.d("DraftManager", "Tabla de borradores creada")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_DRAFTS ADD COLUMN $COLUMN_IMAGE_URIS TEXT")
        }
    }

    /**
     * Guarda o actualiza un borrador
     */
    fun saveDraft(draft: Draft): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, draft.userId)
            put(COLUMN_TITLE, draft.title)
            put(COLUMN_CONTENT, draft.content)
            put(COLUMN_IMAGE_BASE64, draft.imageBase64)
            put(COLUMN_IMAGE_URIS, draft.imageUris)
            put(COLUMN_LOCATION, draft.location)
            put(COLUMN_IS_PUBLIC, if (draft.isPublic) 1 else 0)
            put(COLUMN_CREATED_AT, draft.createdAt)
            put(COLUMN_UPDATED_AT, System.currentTimeMillis())
        }

        val id = if (draft.id > 0) {
            // Actualizar borrador existente
            db.update(TABLE_DRAFTS, values, "$COLUMN_ID = ?", arrayOf(draft.id.toString()))
            Log.d("DraftManager", "Borrador actualizado: ${draft.id}")
            draft.id
        } else {
            // Insertar nuevo borrador
            val newId = db.insert(TABLE_DRAFTS, null, values)
            Log.d("DraftManager", "Borrador guardado con ID: $newId")
            newId
        }

        db.close()
        return id
    }

    /**
     * Obtiene el último borrador del usuario
     */
    fun getLastDraft(userId: Int): Draft? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DRAFTS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_UPDATED_AT DESC",
            "1"
        )

        var draft: Draft? = null
        if (cursor.moveToFirst()) {
            draft = Draft(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_BASE64)),
                imageUris = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URIS)),
                location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)),
                isPublic = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PUBLIC)) == 1,
                createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
            )
        }

        cursor.close()
        db.close()
        return draft
    }

    /**
     * Obtiene todos los borradores del usuario
     */
    fun getAllDrafts(userId: Int): List<Draft> {
        val drafts = mutableListOf<Draft>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DRAFTS,
            null,
            "$COLUMN_USER_ID = ?",
            arrayOf(userId.toString()),
            null,
            null,
            "$COLUMN_UPDATED_AT DESC"
        )

        while (cursor.moveToNext()) {
            drafts.add(
                Draft(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                    imageBase64 = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_BASE64)),
                    imageUris = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URIS)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)),
                    isPublic = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PUBLIC)) == 1,
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_UPDATED_AT))
                )
            )
        }

        cursor.close()
        db.close()
        Log.d("DraftManager", "Borradores encontrados: ${drafts.size}")
        return drafts
    }

    /**
     * Elimina un borrador
     */
    fun deleteDraft(draftId: Long): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_DRAFTS, "$COLUMN_ID = ?", arrayOf(draftId.toString()))
        db.close()
        
        val success = rowsDeleted > 0
        Log.d("DraftManager", "Borrador eliminado: $success")
        return success
    }

    /**
     * Elimina todos los borradores del usuario
     */
    fun deleteAllDrafts(userId: Int): Boolean {
        val db = writableDatabase
        val rowsDeleted = db.delete(TABLE_DRAFTS, "$COLUMN_USER_ID = ?", arrayOf(userId.toString()))
        db.close()
        
        Log.d("DraftManager", "Borradores eliminados: $rowsDeleted")
        return rowsDeleted > 0
    }

    /**
     * Verifica si hay cambios sin guardar
     */
    fun hasUnsavedChanges(
        content: String?,
        imageBase64: String?,
        title: String?,
        location: String?
    ): Boolean {
        return !content.isNullOrBlank() || 
               !imageBase64.isNullOrBlank() || 
               !title.isNullOrBlank() || 
               !location.isNullOrBlank()
    }
}
