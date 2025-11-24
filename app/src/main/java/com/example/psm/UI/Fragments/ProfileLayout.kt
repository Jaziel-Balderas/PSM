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
import com.example.psm.UI.Activity.EditProfileActivity
import com.example.psm.UI.controller.AuthVIewModel
import Model.repository.SessionManager
import Model.data.User // Tu Modelo de Datos



private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileLayout : Fragment() {

    private lateinit var authViewModel: AuthVIewModel
    private lateinit var txtNombreUsuario: TextView
    private lateinit var fotoPerfil: de.hdodenhof.circleimageview.CircleImageView

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

        // Cargar datos desde SessionManager (SQLite - modo offline)
        loadProfileFromCache()

        val userRepository = UserRepository()

        // 1. Inicializar el Controller (MVC)
        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)

        // 2. Patrón OBSERVER: Suscribirse al LiveData del perfil (modo online)
        authViewModel.currentUserProfile.observe(viewLifecycleOwner, Observer { user ->

            if (user != null) {
                // El Observer actualiza la View
                txtNombreUsuario.text = "${user.nameuser} ${user.lastnames}"

                // Lógica de imagen
                if (!user.profile_image_url.isNullOrEmpty()) {
                    val bitmap = decodeBase64ToBitmap(user.profile_image_url)
                    if (bitmap != null) {
                        // Crear un bitmap sin transparencia forzando ARGB_8888
                        val bitmapWithoutAlpha = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                        fotoPerfil.setImageBitmap(bitmapWithoutAlpha)
                    } else {
                        // Fallo en decodificación
                        fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                    }
                } else {
                    // No hay imagen guardada
                    fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                }
            }
        })

        // 3. Listener para navegar a Edición
        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    // Función para cargar el perfil desde SQLite (modo offline)
    private fun loadProfileFromCache() {
        val currentUser = SessionManager.currentUser
        
        if (currentUser != null) {
            // Mostrar datos desde cache inmediatamente
            txtNombreUsuario.text = "${currentUser.nameuser} ${currentUser.lastnames}"
            
            // Cargar foto de perfil
            if (!currentUser.profile_image_url.isNullOrEmpty()) {
                val bitmap = decodeBase64ToBitmap(currentUser.profile_image_url)
                if (bitmap != null) {
                    // Crear un bitmap sin transparencia forzando ARGB_8888
                    val bitmapWithoutAlpha = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    fotoPerfil.setImageBitmap(bitmapWithoutAlpha)
                } else {
                    fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                }
            } else {
                fotoPerfil.setImageResource(R.drawable.perfilejemplo)
            }
        } else {
            // Si no hay usuario en memoria, intentar cargar desde SQLite
            if (SessionManager.loadSessionFromCache()) {
                loadProfileFromCache()
            } else {
                txtNombreUsuario.text = "Error: Sesión no válida"
                fotoPerfil.setImageResource(R.drawable.perfilejemplo)
            }
        }
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
    }


}

private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        // Limpiar espacios, saltos de línea y caracteres no válidos
        val cleaned = base64Str.trim().replace("\\s+".toRegex(), "")
        
        if (cleaned.isEmpty()) {
            Log.w("DECODE_ERROR", "String Base64 vacío")
            return null
        }
        
        val imageBytes = Base64.decode(cleaned, Base64.DEFAULT)
        
        if (imageBytes.isEmpty()) {
            Log.w("DECODE_ERROR", "Bytes decodificados vacíos")
            return null
        }
        
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        if (bitmap == null) {
            Log.e("DECODE_ERROR", "BitmapFactory retornó null. Tamaño de bytes: ${imageBytes.size}")
        } else {
            Log.d("DECODE_SUCCESS", "Bitmap decodificado: ${bitmap.width}x${bitmap.height}")
        }
        
        bitmap
    } catch (e: IllegalArgumentException) {
        Log.e("DECODE_ERROR", "Base64 inválido: ${e.message}")
        null
    } catch (e: Exception) {
        Log.e("DECODE_ERROR", "Error al decodificar imagen: ${e.message}")
        null
    }
}
