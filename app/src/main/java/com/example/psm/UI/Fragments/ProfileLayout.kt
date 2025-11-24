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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditarPerfil)
        val txtNombreUsuario = view.findViewById<TextView>(R.id.txtNombreUsuario)
        val fotoPerfil = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.fotoPerfil) // Captura aquí

        val userRepository = UserRepository()

        // 1. Inicializar el Controller (MVC)
        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)

        // 2. Patrón OBSERVER: Suscribirse al LiveData del perfil
        authViewModel.currentUserProfile.observe(viewLifecycleOwner, Observer { user ->

            if (user != null) {
                // El Observer actualiza la View
                txtNombreUsuario.text = "${user.nameuser} ${user.lastnames}"

                // Lógica de imagen
                if (!user.profile_image_url.isNullOrEmpty()) {
                    val bitmap = decodeBase64ToBitmap(user.profile_image_url)
                    if (bitmap != null) {
                        fotoPerfil.setImageBitmap(bitmap)
                    } else {
                        // Fallo en decodificación
                        fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                    }
                } else {
                    // No hay imagen guardada
                    fotoPerfil.setImageResource(R.drawable.perfilejemplo)
                }
            } else {
                txtNombreUsuario.text = "Error: Sesión no válida"
            }
        })

        // 3. Listener para navegar a Edición
        btnEdit.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

            override fun onResume() {
                super.onResume()
                if (::authViewModel.isInitialized) {
                    // Llama a la nueva función de recarga del Controller
                    authViewModel.loadUserProfile()
                }
            }


}

private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val cleaned = base64Str.replace("\\s".toRegex(), "")
        val imageBytes = Base64.decode(cleaned, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    } catch (e: Exception) {
        Log.e("DECODE_ERROR", "Error al decodificar imagen: ${e.message}")
        null
    }
}
