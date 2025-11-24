# Sistema de Publicaciones con Múltiples Imágenes

## Características Implementadas

### Frontend (Android - Kotlin)
✅ **Campos de la publicación:**
- Título (obligatorio)
- Descripción (opcional)
- Ubicación (opcional)
- Múltiples imágenes (mínimo 1)
- Switch de público/privado

✅ **Funcionalidades:**
- Selección de múltiples imágenes desde la galería
- Vista previa de imágenes en RecyclerView horizontal
- Eliminar imágenes individuales antes de publicar
- Validación de campos obligatorios
- Indicador de carga durante la publicación
- Mensajes de éxito/error

### Backend (PHP)
✅ **API Create Post:**
- Endpoint: `BD/create_post.php`
- Método: POST (Multipart)
- Soporte para múltiples imágenes
- Almacenamiento de URLs en formato JSON
- Validación de tipos de archivo

### Arquitectura
✅ **Patrón MVVM:**
- `PostViewModel` - Gestión del estado
- `PostRepository` - Lógica de negocio
- `PostApi` - Interface de Retrofit
- `Post` y `PostResponse` - Modelos de datos

## Estructura de la Base de Datos

```sql
CREATE TABLE posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    location VARCHAR(255) DEFAULT NULL,
    image_urls TEXT NOT NULL,  -- JSON array de URLs
    is_public TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## Cómo Usar

### 1. Actualizar la Base de Datos
Ejecuta el script SQL en `BD/update_posts_table.sql` para actualizar tu tabla.

### 2. Verificar Configuración
- Asegúrate que la carpeta `BD/uploads/` tenga permisos de escritura
- Verifica la URL base en `di/AppModule.kt` (actualmente: `http://10.0.2.2:8080/PSM/`)
- Verifica la URL base en `BD/create_post.php` (línea de imagePath)

### 3. Crear una Publicación
1. Abre la pantalla de publicar
2. Agrega un título (obligatorio)
3. Selecciona una o más imágenes:
   - Click en el botón superior (múltiples)
   - Click en la imagen principal (una sola)
   - Click en "+ Agregar más imágenes"
4. Opcionalmente agrega descripción y ubicación
5. Activa/desactiva el switch de público
6. Presiona "PUBLICAR"

### 4. Flujo de Datos
```
Activity (publicar) 
    ↓
PostViewModel.createPost()
    ↓
PostRepository.createPost()
    ↓
PostApi (Retrofit)
    ↓
create_post.php
    ↓
Base de Datos MySQL
```

## Archivos Creados/Modificados

### Nuevos Archivos:
- `Model/data/Post.kt` - Modelo actualizado
- `Model/dao/PostApi.kt` - API interface
- `Model/repository/PostRepository.kt` - Repositorio implementado
- `UI/controller/PostViewModel.kt` - ViewModel
- `UI/controller/PostViewModelFactory.kt` - Factory
- `UI/adapter/ImagePreviewAdapter.kt` - Adapter para imágenes
- `res/layout/item_image_preview.xml` - Layout de cada imagen
- `BD/update_posts_table.sql` - Script SQL

### Archivos Modificados:
- `UI/Fragments/publicar.kt` - Lógica completa
- `res/layout/activity_publicar.xml` - UI mejorada
- `BD/create_post.php` - Backend actualizado

## Próximos Pasos Sugeridos

1. **Mostrar publicaciones en el feed:**
   - Crear `get_posts.php` para obtener las publicaciones
   - Implementar un RecyclerView en el dashboard
   - Crear un adapter para mostrar posts con múltiples imágenes (ViewPager2)

2. **Funcionalidades adicionales:**
   - Editar/eliminar publicaciones
   - Sistema de likes y comentarios
   - Búsqueda por ubicación
   - Filtrar por público/privado

3. **Mejoras de UX:**
   - Compresión de imágenes antes de subir
   - Progress bar por cada imagen
   - Límite máximo de imágenes (ej: 10)
   - Reordenar imágenes con drag & drop

## Notas Importantes

- Las imágenes se almacenan en `BD/uploads/`
- El formato en BD es JSON: `["url1", "url2", "url3"]`
- Se requiere tener el `userId` guardado en SharedPreferences
- El emulador Android usa `10.0.2.2` para localhost
- Para dispositivos físicos, usa la IP local de tu PC
