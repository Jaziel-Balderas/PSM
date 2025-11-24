# ✅ Checklist de Implementación - Sistema de Publicaciones

## 📋 Pasos de Configuración

### 1. Base de Datos
- [ ] Ejecutar el script SQL en `BD/update_posts_table.sql`
- [ ] Verificar que la tabla `posts` tenga estas columnas:
  - `id` (INT, PRIMARY KEY, AUTO_INCREMENT)
  - `user_id` (INT, NOT NULL)
  - `title` (VARCHAR(255), NOT NULL)
  - `description` (TEXT)
  - `location` (VARCHAR(255))
  - `image_urls` (TEXT, NOT NULL) - guarda JSON
  - `is_public` (TINYINT(1), DEFAULT 1)
  - `created_at` (TIMESTAMP)
- [ ] Crear carpeta `BD/uploads/` con permisos de escritura (777)

### 2. Configuración de URLs
- [ ] En `di/AppModule.kt`:
  ```kotlin
  private const val BASE_URL = "http://10.0.2.2:8080/PSM/"
  ```
  - Para emulador: `10.0.2.2`
  - Para dispositivo físico: Tu IP local (ej: `192.168.1.100`)

- [ ] En `BD/create_post.php` (línea ~37):
  ```php
  $imagePath = "http://10.0.2.2:8080/PSM/" . $target_file;
  ```
  - Debe coincidir con la URL del AppModule

### 3. Permisos de Android
- [✓] `INTERNET` - Ya configurado en AndroidManifest.xml
- [✓] `READ_EXTERNAL_STORAGE` - Implícito para API 23+

### 4. Archivos Creados (Verificar)
- [✓] `Model/data/Post.kt`
- [✓] `Model/dao/PostApi.kt`
- [✓] `Model/repository/PostRepository.kt`
- [✓] `UI/controller/PostViewModel.kt`
- [✓] `UI/controller/PostViewModelFactory.kt`
- [✓] `UI/adapter/ImagePreviewAdapter.kt`
- [✓] `res/layout/item_image_preview.xml`
- [✓] `BD/update_posts_table.sql`

### 5. Archivos Modificados (Verificar)
- [✓] `UI/Fragments/publicar.kt`
- [✓] `res/layout/activity_publicar.xml`
- [✓] `BD/create_post.php`

## 🧪 Pruebas a Realizar

### Test 1: Selección de Imágenes
- [ ] Click en imagen principal → Seleccionar 1 imagen
- [ ] Click en botón superior → Seleccionar múltiples imágenes
- [ ] Click en "+ Agregar más" → Añadir más imágenes
- [ ] Click en X de una imagen → Eliminar imagen específica

### Test 2: Validaciones
- [ ] Intentar publicar sin título → Debe mostrar error
- [ ] Intentar publicar sin imágenes → Debe mostrar error
- [ ] Publicar sin descripción → Debe funcionar (es opcional)
- [ ] Publicar sin ubicación → Debe funcionar (es opcional)

### Test 3: Crear Publicación Completa
- [ ] Agregar título: "Mi primera publicación"
- [ ] Seleccionar 3 imágenes
- [ ] Agregar descripción: "Esta es una prueba"
- [ ] Agregar ubicación: "Ciudad de México"
- [ ] Activar/desactivar switch público
- [ ] Presionar PUBLICAR
- [ ] Verificar mensaje de éxito
- [ ] Verificar en BD que se guardó correctamente

### Test 4: Verificación Backend
- [ ] Revisar carpeta `BD/uploads/` → Deben aparecer las imágenes
- [ ] Revisar tabla `posts` → Debe tener el registro
- [ ] Verificar columna `image_urls` → Debe ser un JSON array:
  ```json
  ["http://10.0.2.2:8080/PSM/uploads/1_1234567890_0_imagen.jpg", "..."]
  ```

## 🐛 Solución de Problemas Comunes

### Error: "No se recibieron imágenes"
**Causa:** El backend no está recibiendo los archivos
**Solución:**
1. Verificar que `PostApi.kt` use `@Part images: List<MultipartBody.Part>`
2. Verificar que `create_post.php` busque `$_FILES['images']`

### Error: "Error de red"
**Causa:** No hay conexión con el servidor
**Solución:**
1. Verificar que Apache/servidor esté corriendo
2. Verificar la URL en `AppModule.kt`
3. Probar el endpoint en Postman

### Error: "Error al guardar las imágenes"
**Causa:** Permisos de carpeta uploads/
**Solución:**
```bash
chmod 777 BD/uploads/
```

### Las imágenes no se muestran
**Causa:** URL incorrecta guardada en BD
**Solución:**
1. Verificar URL en `create_post.php`
2. Debe ser accesible desde Android: `http://10.0.2.2:8080/PSM/uploads/...`

## 📊 Estructura de Datos

### Request (Android → PHP)
```
POST /BD/create_post.php
Content-Type: multipart/form-data

Fields:
- user_id: "1"
- title: "Mi publicación"
- description: "Descripción..."
- location: "CDMX"
- is_public: "1"
- images[]: [File, File, File]
```

### Response (PHP → Android)
```json
{
  "success": true,
  "message": "Publicación creada exitosamente",
  "postId": "123"
}
```

### Base de Datos
```
posts table:
id | user_id | title | description | location | image_urls | is_public | created_at
1  | 5       | Test  | Desc...     | CDMX     | ["url1"]   | 1         | 2025-11-23
```

## 🎯 Funcionalidades Implementadas

✅ Título obligatorio
✅ Descripción opcional
✅ Ubicación opcional  
✅ Múltiples imágenes (N cantidad)
✅ Vista previa de imágenes
✅ Eliminar imágenes antes de publicar
✅ Switch público/privado
✅ Indicador de carga
✅ Validaciones de campos
✅ Mensajes de error/éxito
✅ Arquitectura MVVM
✅ Almacenamiento en BD
✅ Upload de archivos al servidor

## 📝 Notas Finales

- **Límite de imágenes:** Actualmente no hay límite, puedes agregar uno si deseas
- **Tamaño de imágenes:** No hay compresión automática, las imágenes se suben en su tamaño original
- **Formatos soportados:** JPG, JPEG, PNG, GIF, WEBP
- **Almacenamiento:** Las URLs se guardan como JSON array en la columna `image_urls`

## 🚀 Siguiente Paso

Después de verificar que todo funciona, el siguiente paso natural es:
1. Crear el endpoint `get_posts.php` para obtener las publicaciones
2. Mostrar las publicaciones en el dashboard con un RecyclerView
3. Implementar un ViewPager2 para mostrar múltiples imágenes por post
