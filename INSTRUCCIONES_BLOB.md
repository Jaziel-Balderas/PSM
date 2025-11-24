# Guía de implementación completa de BLOB

## ✅ PASO 1: Actualizar las tablas de la base de datos

Ejecuta en tu navegador:
**http://localhost:8080/PSM/BD/setup_blob_tables.php**

Esto modificará:
- `users.profile_image_url` → MEDIUMBLOB
- `posts.image_urls` → LONGBLOB

## ✅ PASO 2: Verificar que los archivos Android están actualizados

Los siguientes archivos ya fueron modificados:

### Model/data/Post.kt
- ✅ Campo `profileImageBase64: String?` agregado
- ✅ Campo `imageUrls: List<String>` ahora contiene base64

### UI/adapter/PostsAdapter.kt
- ✅ Usa `base64ToBitmap()` para imagen de perfil
- ✅ Usa `PostMediaAdapterBase64` para imágenes de posts
- ✅ **NO** usa Glide (eliminado)

### UI/adapter/PostMediaAdapterBase64.kt
- ✅ Nuevo adaptador creado para convertir base64 a Bitmap

### build.gradle.kts
- ✅ Glide eliminado (ya no es necesario)

## ✅ PASO 3: Sync Gradle

En Android Studio:
1. Click en "Sync Now" o
2. File → Sync Project with Gradle Files

## ✅ PASO 4: Probar la aplicación

### Cargar posts existentes:
Los posts se cargarán automáticamente al iniciar sesión.

### Crear nuevos posts:
Las imágenes se convertirán automáticamente a base64 y se guardarán en BLOB.

## 📊 Cómo funciona ahora:

### Backend (PHP):
1. **create_post.php**: Convierte imágenes → base64 → guarda en BLOB
2. **get_posts.php**: Lee BLOB → convierte a base64 → envía a Android

### Android:
1. **PostsAdapter**: Recibe base64 → convierte a Bitmap → muestra en ImageView
2. **PostMediaAdapterBase64**: Lo mismo para las imágenes del ViewPager2

## ⚠️ IMPORTANTE:

### Ventajas de BLOB:
✅ Todo en la base de datos
✅ Backup más fácil
✅ No necesitas carpeta uploads/
✅ No necesitas Glide

### Limitaciones:
❌ Base de datos más grande
❌ Consultas más lentas con muchas imágenes
❌ Límite de tamaño (MEDIUMBLOB: 16MB, LONGBLOB: 4GB)

## 🔧 Solución de problemas:

### Si las imágenes no se muestran:
1. Verifica que las tablas están en BLOB: `DESCRIBE users` y `DESCRIBE posts`
2. Verifica los logs en Logcat (busca "PostsAdapter" o "DashLayout")
3. Prueba el endpoint directamente: `http://localhost:8080/PSM/BD/get_posts.php?current_user_id=1`

### Si falla al crear posts:
1. Verifica que `create_post.php` está actualizado
2. Verifica los permisos de PHP para leer archivos temporales
3. Verifica el tamaño máximo de upload en php.ini

## 📝 Resumen de cambios:

**Base de datos:**
- ✅ `users.profile_image_url` → MEDIUMBLOB
- ✅ `posts.image_urls` → LONGBLOB

**PHP:**
- ✅ `get_posts.php` → Lee BLOB y convierte a base64
- ✅ `create_post.php` → Convierte imágenes a base64 y guarda en BLOB

**Android:**
- ✅ `Post.kt` → Campo `profileImageBase64`
- ✅ `PostsAdapter.kt` → Conversión base64 → Bitmap
- ✅ `PostMediaAdapterBase64.kt` → Nuevo adaptador
- ✅ Glide eliminado

Todo está listo para usar BLOB. Solo ejecuta el setup_blob_tables.php y sync Gradle.
