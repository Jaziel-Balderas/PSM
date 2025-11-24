# Instrucciones para implementar BLOB en tu proyecto

## 1. ACTUALIZAR BASE DE DATOS

Ejecuta en tu navegador:
http://localhost:8080/PSM/BD/setup_blob_tables.php

Esto modificará las tablas:
- `users.profile_image_url` → `users.profile_image` (MEDIUMBLOB)
- `posts` → Se agrega columna `images` (LONGBLOB)

## 2. MODIFICACIONES EN ANDROID

### A. Actualizar modelo Post.kt
Reemplazar el archivo:
`app/src/main/java/Model/data/Post.kt`

Con el contenido de:
`BLOB_CHANGES/Post.kt`

Cambios principales:
- Agregar campo: `val profileImageBase64: String? = null`
- imageUrls ahora contendrá strings en base64

### B. Actualizar PostsAdapter.kt
Reemplazar el archivo:
`app/src/main/java/com/example/psm/UI/adapter/PostsAdapter.kt`

Con el contenido de:
`BLOB_CHANGES/PostsAdapter.kt`

Cambios principales:
- Función `base64ToBitmap()` para convertir base64 a Bitmap
- Cargar imagen de perfil desde base64
- Usar PostMediaAdapterBlob para las imágenes

### C. Crear PostMediaAdapterBlob.kt
Crear nuevo archivo:
`app/src/main/java/com/example/psm/UI/adapter/PostMediaAdapterBlob.kt`

Copiar el contenido de:
`BLOB_CHANGES/PostMediaAdapterBlob.kt`

### D. Actualizar PostApi.kt
Cambiar la URL del endpoint:
```kotlin
@GET("get_posts_blob.php")
suspend fun getPosts(
    @Query("current_user_id") currentUserId: Int = 0,
    @Query("user_id") userId: Int? = null,
    @Query("limit") limit: Int = 50,
    @Query("offset") offset: Int = 0
): Response<PostsResponse>
```

## 3. VENTAJAS DE USAR BLOB

✅ Imágenes almacenadas directamente en la BD
✅ No necesitas gestionar archivos en el servidor
✅ Backup más simple (todo en la BD)
✅ No necesitas Glide o Picasso (Base64 → Bitmap directo)
✅ Mejor seguridad (imágenes no accesibles por URL directa)

## 4. DESVENTAJAS

❌ Mayor tamaño de la base de datos
❌ Consultas más lentas con muchas imágenes
❌ Mayor uso de memoria RAM
❌ No se puede acceder a imágenes desde navegador directamente

## 5. RECOMENDACIÓN

Para tu app social:
- **BLOB** para imágenes de perfil (pequeñas)
- **URLs** para imágenes de posts (pueden ser grandes y múltiples)

## 6. ENDPOINTS PHP DISPONIBLES

- `get_posts_blob.php` - Obtener posts con imágenes en base64
- `create_post_blob.php` - Crear posts guardando imágenes como BLOB

## 7. PRÓXIMOS PASOS

1. Ejecutar setup_blob_tables.php
2. Reemplazar archivos Android
3. Sync Gradle
4. Probar la app

¿Necesitas ayuda con algún paso específico?
