# Arquitectura Offline-First - PSM

## Descripción General

La aplicación PSM ahora cuenta con funcionalidad offline completa que permite a los usuarios ver y crear contenido sin conexión a internet. Cuando se recupera la conectividad, los cambios se sincronizan automáticamente con el servidor.

## Componentes Implementados

### 1. Room Database (`Model/database/`)

#### **PostEntity** (posts_cache)
Almacena posts en caché local con todos sus datos:
- `post_id` (PK): ID del post
- `user_id`: ID del usuario autor
- `title`, `content`, `location`: Datos del post
- `is_public`: Visibilidad (1=público, 0=privado)
- `likes_count`, `dislikes_count`: Contadores de votos
- `user_vote`: Voto del usuario actual (1=like, -1=dislike, null=sin voto)
- `images_json`: Array JSON con imágenes en base64
- `created_at`: Timestamp de creación
- `cached_at`: Timestamp del caché (para expiración)

#### **PendingActionEntity** (pending_actions)
Cola de acciones pendientes de sincronización:
- `id` (PK autoincrement): Identificador único
- `action_type`: Tipo de acción ("CREATE_POST", "VOTE_POST", etc.)
- `json_payload`: Datos de la acción en JSON
- `created_at`: Timestamp de creación
- `retry_count`: Contador de reintentos (máximo 3)
- `status`: Estado ("PENDING", "SYNCING", "FAILED")

#### **DAOs**
- **PostDao**: CRUD completo, queries con Flow para observación reactiva
- **PendingActionDao**: Gestión de cola de sincronización

### 2. ConnectivityObserver (`Model/repository/`)

Singleton que monitorea el estado de conectividad en tiempo real:
- Usa `ConnectivityManager.NetworkCallback` para detectar cambios
- Expone `StateFlow<Boolean>` para observar estado de conectividad
- Se inicializa automáticamente en `PSMApplication.onCreate()`

```kotlin
// Observar conectividad desde cualquier parte
ConnectivityObserver.isConnected.collect { isOnline ->
    if (isOnline) {
        // Hay internet, sincronizar
    } else {
        // Sin internet, usar caché
    }
}
```

### 3. OfflineFirstPostRepository (`Model/repository/`)

Repositorio que implementa el patrón cache-first:

#### **Lectura de Posts (getPosts)**
1. Lee caché local primero (respuesta inmediata)
2. Si hay internet, actualiza desde servidor en segundo plano
3. Actualiza caché con datos frescos
4. Si falla la red, devuelve caché

#### **Escritura Offline (createPost, votePost)**
1. Guarda acción en `pending_actions`
2. Si hay internet, intenta sincronizar inmediatamente
3. Si no hay internet, queda pendiente para sincronización posterior

#### **Sincronización (syncPendingActions)**
- Procesa todas las acciones pendientes
- Llama a APIs correspondientes
- Borra acción si es exitosa
- Incrementa `retry_count` si falla (máximo 3 intentos)
- Marca como "FAILED" después de 3 intentos

#### **Limpieza de Caché (clearOldCache)**
- Elimina posts cacheados con más de 24 horas
- Elimina acciones fallidas antiguas

### 4. SyncWorker (`Model/worker/`)

Worker de WorkManager que sincroniza en segundo plano:
- Se ejecuta periódicamente cada 15 minutos (cuando hay conectividad)
- Se ejecuta inmediatamente cuando se restaura la conectividad
- Procesa cola de `pending_actions`
- Usa backoff exponencial en caso de errores
- Reintentos automáticos hasta 3 veces por acción

### 5. PSMApplication

Clase Application que inicializa el sistema offline:
- Inicializa `ConnectivityObserver`
- Configura WorkManager para sincronización periódica
- Configura sincronización inmediata al recuperar conectividad

## Flujo de Uso

### Escenario 1: Ver Posts Offline

1. Usuario abre la app sin internet
2. `OfflineFirstPostRepository.getPosts()` lee de Room
3. Posts cacheados se muestran inmediatamente
4. Mensaje indica "Mostrando datos locales"

### Escenario 2: Crear Post Offline

1. Usuario crea post sin internet
2. `OfflineFirstPostRepository.createPost()` guarda en `pending_actions`
3. Toast muestra "Publicación guardada, se enviará cuando haya conexión"
4. Cuando vuelve internet, `SyncWorker` envía post automáticamente
5. Post aparece con ID real del servidor

### Escenario 3: Votar Post Offline

1. Usuario da like/dislike sin internet
2. `OfflineFirstPostRepository.votePost()` actualiza caché local (UI optimista)
3. Acción se guarda en `pending_actions`
4. UI muestra cambio inmediatamente
5. Cuando vuelve internet, voto se sincroniza con servidor

### Escenario 4: Sincronización Automática

1. Usuario recupera conectividad
2. `ConnectivityObserver` detecta cambio de estado
3. WorkManager dispara `SyncWorker` inmediatamente
4. Worker procesa todas las acciones pendientes
5. Caché se actualiza con datos frescos del servidor
6. UI se actualiza automáticamente (gracias a Flow)

## Integración en UI (Próximos Pasos)

### Actualizar DashLayout para usar OfflineFirstPostRepository

```kotlin
class dashlayout : Fragment() {
    private lateinit var offlineRepository: OfflineFirstPostRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar repositorio offline
        val database = AppDatabase.getInstance(requireContext())
        val retrofit = // ... tu instancia de Retrofit
        val postApi = retrofit.create(PostApi::class.java)
        offlineRepository = OfflineFirstPostRepository(requireContext(), postApi, database)
    }
    
    private fun loadPosts() {
        lifecycleScope.launch {
            // Observar posts desde Flow (actualización automática)
            offlineRepository.getPostsFlow().collect { posts ->
                postsAdapter.updatePosts(posts)
            }
        }
        
        // Forzar actualización desde servidor
        lifecycleScope.launch {
            offlineRepository.getPosts(currentUserId, forceRefresh = true)
        }
    }
    
    private fun handleLikeClick(post: Post, position: Int) {
        lifecycleScope.launch {
            val result = offlineRepository.votePost(postId, userId, vote)
            if (result.isSuccess) {
                // Voto guardado (puede estar pendiente de sync)
                Toast.makeText(context, "Voto registrado", Toast.LENGTH_SHORT).show()
            } else {
                // Error grave (poco probable)
                Toast.makeText(context, "Error al votar", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### Mostrar Indicador de Estado de Conectividad

```kotlin
// En onViewCreated
lifecycleScope.launch {
    ConnectivityObserver.isConnected.collect { isOnline ->
        if (isOnline) {
            // Ocultar indicador offline
            tvOfflineIndicator.visibility = View.GONE
        } else {
            // Mostrar indicador offline
            tvOfflineIndicator.visibility = View.VISIBLE
            tvOfflineIndicator.text = "📴 Sin conexión - Mostrando datos locales"
        }
    }
}
```

### Actualizar Publicar para usar OfflineFirstPostRepository

```kotlin
class publicar : AppCompatActivity() {
    private lateinit var offlineRepository: OfflineFirstPostRepository
    
    private fun publishPost() {
        lifecycleScope.launch {
            val result = offlineRepository.createPost(
                userId = userId,
                title = title,
                content = content,
                location = location,
                isPublic = isPublic,
                imagesBase64 = imagesList
            )
            
            if (result.isSuccess) {
                if (ConnectivityObserver.checkConnectivity()) {
                    Toast.makeText(this@publicar, "Publicación creada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@publicar, 
                        "Publicación guardada, se enviará cuando haya conexión", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            } else {
                Toast.makeText(this@publicar, "Error al guardar publicación", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

## Configuración en AndroidManifest.xml

Ya está configurado:
- `android:name=".PSMApplication"` en `<application>`
- Permiso `ACCESS_NETWORK_STATE` agregado

## Dependencias Agregadas (build.gradle.kts)

```kotlin
// Room
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
annotationProcessor("androidx.room:room-compiler:2.6.1")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

## Base de Datos Room

- **Nombre**: `psm_app_database`
- **Versión**: 1
- **Estrategia de migración**: `fallbackToDestructiveMigration` (para desarrollo)
- **Ubicación**: `/data/data/com.example.psm/databases/psm_app_database`

## Testing Offline

### Probar Sin Internet

1. Desactivar WiFi y datos móviles en el emulador
2. Abrir la app y verificar que muestra posts cacheados
3. Crear un nuevo post → debe guardarse localmente
4. Votar un post → debe actualizarse UI localmente
5. Activar internet → WorkManager debe sincronizar automáticamente

### Verificar Base de Datos Room

Usando Android Studio Database Inspector:
1. View → Tool Windows → App Inspection
2. Seleccionar proceso `com.example.psm`
3. Ver tablas `posts_cache` y `pending_actions`

### Ver Logs de Sincronización

```
adb logcat -s SyncWorker:D OfflinePostRepo:D ConnectivityObserver:D
```

## Mejoras Futuras

1. **Manejo de Imágenes Offline**: Actualmente las imágenes se guardan como JSON en `images_json`, considerar almacenarlas en archivos locales para mejor performance
2. **Conflict Resolution**: Implementar estrategia de resolución de conflictos cuando servidor y local difieren
3. **Incremental Sync**: Sincronizar solo cambios desde última sincronización (usando timestamps)
4. **Notificación de Sincronización**: Mostrar notificación cuando se completa sincronización en segundo plano
5. **Manejo de Errores 409**: Si post ya existe en servidor (duplicado), marcar como exitoso en vez de reintentar
6. **UI para Acciones Pendientes**: Mostrar lista de acciones pendientes en configuración
7. **Limpieza Inteligente**: Mantener posts más vistos/importantes por más tiempo en caché

## Notas Técnicas

- **Expiración de Caché**: Posts se mantienen 24 horas por defecto (`CACHE_EXPIRY_MS`)
- **Reintentos**: Máximo 3 intentos por acción pendiente
- **Sincronización Periódica**: Cada 15 minutos (solo con conectividad)
- **Backoff Policy**: Exponencial para reintentos de WorkManager
- **Thread Safety**: Room garantiza operaciones thread-safe, Flow ejecuta en Main thread

## Troubleshooting

### Posts no se actualizan automáticamente
- Verificar que estás usando `getPostsFlow()` con `.collect()` en lugar de `getPosts()`
- Asegurarse de que el lifecycle scope no se cancele prematuramente

### Sincronización no ocurre
- Verificar logs de WorkManager: `adb logcat -s WM-WorkerWrapper:D`
- Verificar constraints de red en `PSMApplication`
- Asegurarse de que `ConnectivityObserver.init()` se llama en Application

### Acciones pendientes no se procesan
- Verificar que las acciones tienen status "PENDING"
- Revisar `retry_count` (máximo 3)
- Verificar logs de `SyncWorker` para ver errores específicos

### Base de datos corrupta
- Limpiar datos de app: Settings → Apps → PSM → Clear Data
- En desarrollo, cambiar versión de base de datos fuerza recreación
