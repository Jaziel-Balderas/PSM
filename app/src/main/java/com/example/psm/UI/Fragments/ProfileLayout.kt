package com.example.psm.UI.Fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import com.example.psm.R
import com.example.psm.UI.Activity.RegisterActivity
import androidx.lifecycle.ViewModel
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.psm.UI.controller.ProfileVIewModel
import Model.repository.UserRepository
import com.example.psm.UI.controller.AuthViewModelFactory
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.fragment.app.viewModels
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.UI.Activity.EditProfileActivity
import com.example.psm.UI.adapter.PostsAdapter
import com.example.psm.UI.controller.AuthVIewModel
import com.example.psm.UI.controller.PostViewModel
import com.example.psm.UI.controller.PostViewModelFactory
import Model.repository.SessionManager
import Model.repository.PostRepository
import Model.data.User // Tu Modelo de Datos



private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileLayout : Fragment() {

    private lateinit var authViewModel: AuthVIewModel
    private lateinit var postViewModel: PostViewModel
    private lateinit var txtNombreUsuario: TextView
    private lateinit var fotoPerfil: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var recyclerViewUserPosts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var postsAdapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar SessionManager
        SessionManager.init(requireContext())

        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditarPerfil)
        txtNombreUsuario = view.findViewById<TextView>(R.id.txtNombreUsuario)
        fotoPerfil = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.fotoPerfil)
        recyclerViewUserPosts = view.findViewById(R.id.recyclerViewUserPosts)
        progressBar = view.findViewById(R.id.progressBarProfile)
        
        Log.d("ProfileLayout", "onViewCreated: Views initialized")
        
        // Asegurarse de que la imagen esté limpia al inicio
        fotoPerfil.setImageDrawable(null)
        
        // Configurar RecyclerView
        recyclerViewUserPosts.layoutManager = LinearLayoutManager(requireContext())
        postsAdapter = PostsAdapter(
            posts = mutableListOf(),
            onLikeClick = { _, _ -> }, // No necesario en perfil
            onDislikeClick = { _, _ -> }, // No necesario en perfil
            onCommentClick = { _, _ -> }, // No necesario en perfil
            onFavoriteClick = { _, _ -> }, // No necesario en perfil
            onDeletePost = { _, _ -> } // No permitir eliminar desde perfil
        )
        recyclerViewUserPosts.adapter = postsAdapter

        // PRIMERO: Cargar datos desde SessionManager (SQLite - modo offline)
        Log.d("ProfileLayout", "Loading profile from cache...")
        loadProfileFromCache()

        val userRepository = UserRepository()
        val postRepository = PostRepository()

        // 1. Inicializar el Controller (MVC)
        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)
            
        postViewModel = ViewModelProvider(this, PostViewModelFactory(postRepository))
            .get(PostViewModel::class.java)

        // 2. Patrón OBSERVER: Suscribirse al LiveData del perfil (modo online)
        authViewModel.currentUserProfile.observe(viewLifecycleOwner, Observer { user ->

            Log.d("ProfileLayout", "Observer triggered: user=${user != null}")
            
            if (user != null) {
                // El Observer actualiza la View
                txtNombreUsuario.text = "${user.nameuser} ${user.lastnames}"

                Log.d("ProfileLayout", "User from server: ${user.nameuser}, hasImage: ${!user.profile_image_url.isNullOrEmpty()}")
                
                // Lógica de imagen
                if (!user.profile_image_url.isNullOrEmpty()) {
                    Log.d("ProfileLayout", "Decoding profile image from server")
                    val bitmap = decodeBase64ToBitmap(user.profile_image_url)
                    if (bitmap != null) {
                        fotoPerfil.setImageBitmap(bitmap)
                        Log.d("ProfileLayout", "Profile image from server set successfully")
                    } else {
                        // Fallo en decodificación
                        Log.w("ProfileLayout", "Failed to decode bitmap from server, using default")
                        fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                    }
                } else {
                    // No hay imagen guardada
                    Log.w("ProfileLayout", "No profile image URL from server, using default")
                    fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                }
            }
        })
        
        // Observer para las publicaciones del usuario
        postViewModel.userPosts.observe(viewLifecycleOwner, Observer { posts ->
            if (posts.isNotEmpty()) {
                postsAdapter.updatePosts(posts)
            }
        })
        
        // Observer para loading
        postViewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
        
        // Cargar publicaciones del usuario actual
        val userId = SessionManager.getUserId()
        if (!userId.isNullOrEmpty()) {
            postViewModel.loadUserPosts(userId)
        }
        
        // Cargar perfil del usuario actual desde el servidor
        Log.d("ProfileLayout", "Loading profile from server...")
        authViewModel.loadUserProfile()

        // 3. Listener para navegar a Edición
        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // Función para cargar el perfil desde SQLite (modo offline)
    private fun loadProfileFromCache() {
        val currentUser = SessionManager.currentUser
        
        Log.d("ProfileLayout", "=== loadProfileFromCache START ===")
        Log.d("ProfileLayout", "currentUser=${currentUser != null}")
        
        if (currentUser != null) {
            // Mostrar datos desde cache inmediatamente
            txtNombreUsuario.text = "${currentUser.nameuser} ${currentUser.lastnames}"
            
            Log.d("ProfileLayout", "User: ${currentUser.nameuser}")
            Log.d("ProfileLayout", "profile_image_url isNull: ${currentUser.profile_image_url == null}")
            Log.d("ProfileLayout", "profile_image_url isEmpty: ${currentUser.profile_image_url?.isEmpty()}")
            Log.d("ProfileLayout", "profile_image_url length: ${currentUser.profile_image_url?.length}")
            
            // Cargar foto de perfil
            if (!currentUser.profile_image_url.isNullOrEmpty()) {
                Log.d("ProfileLayout", ">>> Attempting to decode profile image from cache")
                val bitmap = decodeBase64ToBitmap(currentUser.profile_image_url)
                if (bitmap != null) {
                    Log.d("ProfileLayout", ">>> Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")
                    fotoPerfil.setImageBitmap(bitmap)
                    Log.d("ProfileLayout", ">>> Profile image SET to ImageView")
                } else {
                    Log.w("ProfileLayout", ">>> Failed to decode bitmap, using default")
                    fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                }
            } else {
                Log.w("ProfileLayout", ">>> No profile image URL, using default")
                fotoPerfil.setImageResource(R.drawable.perfilejemplo)
            }
        } else {
            Log.w("ProfileLayout", "currentUser is NULL, trying to load from cache")
            // Si no hay usuario en memoria, intentar cargar desde SQLite
            if (SessionManager.loadSessionFromCache()) {
                Log.d("ProfileLayout", "Session loaded from cache, retrying...")
                loadProfileFromCache()
            } else {
                Log.e("ProfileLayout", "Failed to load session from cache")
                txtNombreUsuario.text = "Error: Sesión no válida"
                fotoPerfil.setImageResource(R.drawable.perfilejemplo)
            }
        }
        Log.d("ProfileLayout", "=== loadProfileFromCache END ===")
    }

    override fun onResume() {
        super.onResume()
        
        // Recargar datos desde SQLite cuando volvemos a este Fragment
        SessionManager.loadSessionFromCache()
        
        // Recargar UI con datos actualizados del cache
        if (::txtNombreUsuario.isInitialized && ::fotoPerfil.isInitialized) {
            loadProfileFromCache()
        }
        
        // También recargar desde el ViewModel si está inicializado
        if (::authViewModel.isInitialized) {
            authViewModel.loadUserProfile()
        }
        
        // Recargar publicaciones del usuario
        if (::postViewModel.isInitialized) {
            val userId = SessionManager.getUserId()
            if (!userId.isNullOrEmpty()) {
                postViewModel.loadUserPosts(userId)
            }
        }
    }


}

private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        Log.e("ProfileLayout", "Error decodificando imagen: ${e.message}")
        null
    }
}
