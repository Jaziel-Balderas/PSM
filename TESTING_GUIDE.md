# Ejemplos de Testing para el API de Publicaciones

## Usando Postman

### Endpoint: Create Post
**URL:** `http://localhost:8080/PSM/BD/create_post.php`
**Método:** POST
**Tipo:** multipart/form-data

#### Body (form-data):
| Key | Type | Value |
|-----|------|-------|
| user_id | text | 1 |
| title | text | Mi primera publicación |
| description | text | Esta es una descripción de prueba |
| location | text | Ciudad de México |
| is_public | text | 1 |
| images[] | file | (seleccionar imagen 1) |
| images[] | file | (seleccionar imagen 2) |
| images[] | file | (seleccionar imagen 3) |

**Nota:** En Postman, para subir múltiples archivos con el mismo nombre, debes agregar múltiples filas con la key `images[]`

#### Respuesta Esperada (Success):
```json
{
  "success": true,
  "message": "Publicación creada exitosamente",
  "postId": "1"
}
```

#### Respuesta de Error:
```json
{
  "success": false,
  "message": "Faltan datos obligatorios (user_id o título)."
}
```

## Usando cURL (Terminal)

### Test con múltiples imágenes:
```bash
curl -X POST http://localhost:8080/PSM/BD/create_post.php \
  -F "user_id=1" \
  -F "title=Publicación de prueba" \
  -F "description=Esta es una descripción" \
  -F "location=CDMX" \
  -F "is_public=1" \
  -F "images[]=@/ruta/a/imagen1.jpg" \
  -F "images[]=@/ruta/a/imagen2.jpg" \
  -F "images[]=@/ruta/a/imagen3.jpg"
```

### Test con una sola imagen:
```bash
curl -X POST http://localhost:8080/PSM/BD/create_post.php \
  -F "user_id=1" \
  -F "title=Solo una imagen" \
  -F "description=Prueba con una imagen" \
  -F "location=Guadalajara" \
  -F "is_public=1" \
  -F "images[]=@/ruta/a/imagen.jpg"
```

### Test sin imágenes (debe fallar):
```bash
curl -X POST http://localhost:8080/PSM/BD/create_post.php \
  -F "user_id=1" \
  -F "title=Sin imágenes" \
  -F "description=Esto debe fallar"
```

### Test sin título (debe fallar):
```bash
curl -X POST http://localhost:8080/PSM/BD/create_post.php \
  -F "user_id=1" \
  -F "description=Sin título" \
  -F "images[]=@/ruta/a/imagen.jpg"
```

## Verificar en la Base de Datos

### SQL para ver las publicaciones:
```sql
SELECT * FROM posts ORDER BY created_at DESC LIMIT 5;
```

### SQL para ver las URLs de imágenes de un post:
```sql
SELECT id, title, image_urls FROM posts WHERE id = 1;
```

### SQL para parsear el JSON de imágenes:
```sql
SELECT 
    id, 
    title, 
    JSON_EXTRACT(image_urls, '$') as imagenes
FROM posts 
WHERE id = 1;
```

## Verificar los Archivos Subidos

### En Windows (PowerShell):
```powershell
Get-ChildItem -Path "C:\xampp\htdocs\PSM\BD\uploads\" | Sort-Object LastWriteTime -Descending | Select-Object -First 10
```

### En Linux/Mac:
```bash
ls -lht /path/to/PSM/BD/uploads/ | head -10
```

## Testing desde Android Studio

### Logcat para debug:
Filtro: `tag:PostViewModel|PostRepository|Retrofit`

### Breakpoints importantes:
1. `publicar.kt` línea: `postViewModel.createPost(...)`
2. `PostRepository.kt` línea: `val response = api.createPost(...)`
3. `PostViewModel.kt` línea: `if (response != null && response.success)`

### SharedPreferences - Verificar userId:
```kotlin
// En Android Studio Debugger o Logcat
val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
val userId = sharedPref.getString("userId", null)
Log.d("DEBUG", "UserId guardado: $userId")
```

## Casos de Prueba Recomendados

### ✅ Happy Path
1. Usuario inicia sesión
2. Va a crear publicación
3. Selecciona 3 imágenes
4. Completa todos los campos
5. Presiona PUBLICAR
6. ✓ Se crea exitosamente

### ❌ Error: Sin sesión
1. Usuario no ha iniciado sesión (no hay userId en SharedPreferences)
2. Intenta publicar
3. ✓ Debe mostrar: "Error: Sesión no válida"

### ❌ Error: Sin imágenes
1. Usuario no selecciona ninguna imagen
2. Completa los demás campos
3. Presiona PUBLICAR
4. ✓ Debe mostrar: "Debes seleccionar al menos una imagen"

### ❌ Error: Sin título
1. Usuario selecciona imágenes
2. No completa el título
3. Presiona PUBLICAR
4. ✓ Debe mostrar: "Debes agregar un título"

### ✅ Edge Case: Solo campos obligatorios
1. Usuario selecciona 1 imagen
2. Solo completa el título
3. Deja descripción y ubicación vacías
4. Presiona PUBLICAR
5. ✓ Se crea exitosamente con campos opcionales vacíos

### ✅ Multiple Images
1. Usuario selecciona 10 imágenes
2. Completa todos los campos
3. Presiona PUBLICAR
4. ✓ Se crean las 10 imágenes en uploads/
5. ✓ La columna image_urls contiene 10 URLs

## Monitoreo de Performance

### Tiempo de respuesta esperado:
- 1 imagen (~500KB): ~2-3 segundos
- 5 imágenes (~2.5MB): ~5-8 segundos
- 10 imágenes (~5MB): ~10-15 segundos

### Network Traffic (desde Android Studio Profiler):
- Request size: Tamaño de todas las imágenes + form data
- Response size: ~100-200 bytes (JSON response)

## Troubleshooting

### Si el upload es muy lento:
1. Verificar conexión de red
2. Considerar compresión de imágenes
3. Limitar cantidad máxima de imágenes

### Si las imágenes no se guardan:
1. Verificar permisos de carpeta uploads/
2. Verificar espacio en disco
3. Verificar configuración de PHP (upload_max_filesize, post_max_size)

### Si el JSON no se parsea:
1. Verificar que `image_urls` sea tipo TEXT en MySQL
2. Verificar que PHP esté usando `json_encode()` correctamente
3. Verificar charset de la tabla (utf8mb4)
