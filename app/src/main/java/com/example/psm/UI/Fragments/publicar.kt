package com.example.psm.UI.Fragments

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.psm.databinding.ActivityPublicarBinding
import com.example.psm.UI.controller.PostViewModel
import com.example.psm.UI.controller.PostViewModelFactory
import com.example.psm.UI.adapter.ImagePreviewAdapter
import Model.repository.PostRepository
import Model.repository.SessionManager
import java.io.File
import java.io.FileOutputStream

class publicar : AppCompatActivity() {

    private lateinit var binding: ActivityPublicarBinding
    private lateinit var postViewModel: PostViewModel
    private val PICK_IMAGE_REQUEST = 1
    private val PICK_MULTIPLE_IMAGES_REQUEST = 2
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPublicarBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Inicializar SessionManager
        SessionManager.init(this)

        // Inicializar ViewModel
        val repository = PostRepository()
        postViewModel = ViewModelProvider(this, PostViewModelFactory(repository))
            .get(PostViewModel::class.java)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        imageAdapter = ImagePreviewAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            imageAdapter.notifyItemRangeChanged(position, selectedImageUris.size)
            updateImageVisibility()
        }
        
        binding.rvSelectedImages.apply {
            layoutManager = LinearLayoutManager(this@publicar, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
    }

    private fun setupObservers() {
        postViewModel.postStatus.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "¡Publicación creada exitosamente!", Toast.LENGTH_SHORT).show()
                finish()
            }
        })

        postViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty() && postViewModel.postStatus.value == false) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        postViewModel.isLoading.observe(this, Observer { isLoading ->
            binding.btnPublicarFinal.isEnabled = !isLoading
            binding.btnPublicarFinal.text = if (isLoading) "PUBLICANDO..." else "PUBLICAR"
        })
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            finish()
        }

        // Botón de la barra superior - ahora selecciona múltiples imágenes
        binding.btnImagenPublicar.setOnClickListener {
            abrirGaleriaMultiple()
        }

        // Click en la imagen principal - selecciona una imagen
        binding.ivPostMedia.setOnClickListener {
            abrirGaleria()
        }

        // Botón para agregar más imágenes
        binding.tvAddMoreImages.setOnClickListener {
            abrirGaleriaMultiple()
        }

        // Botón publicar
        binding.btnPublicarFinal.setOnClickListener {
            subirPublicacion()
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun abrirGaleriaMultiple() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, PICK_MULTIPLE_IMAGES_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    // Una sola imagen o video
                    data.data?.let { uri ->
                        if (!selectedImageUris.contains(uri)) {
                            selectedImageUris.add(uri)
                            imageAdapter.notifyItemInserted(selectedImageUris.size - 1)
                            updateImageVisibility()
                        }
                    }
                }
                PICK_MULTIPLE_IMAGES_REQUEST -> {
                    // Múltiples imágenes o videos
                    if (data.clipData != null) {
                        // Múltiples archivos seleccionados
                        val count = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            val uri = data.clipData!!.getItemAt(i).uri
                            if (!selectedImageUris.contains(uri)) {
                                selectedImageUris.add(uri)
                            }
                        }
                        imageAdapter.notifyDataSetChanged()
                    } else if (data.data != null) {
                        // Un solo archivo seleccionado
                        val uri = data.data!!
                        if (!selectedImageUris.contains(uri)) {
                            selectedImageUris.add(uri)
                            imageAdapter.notifyItemInserted(selectedImageUris.size - 1)
                        }
                    }
                    updateImageVisibility()
                }
            }
        }
    }

    private fun updateMainImage() {
        // Ya no se usa - todas las imágenes se muestran en el RecyclerView
    }

    private fun updateImageVisibility() {
        if (selectedImageUris.isNotEmpty()) {
            // Ocultar placeholder y mostrar RecyclerView
            binding.ivPostMedia.visibility = View.GONE
            binding.rvSelectedImages.visibility = View.VISIBLE
        } else {
            // Mostrar placeholder y ocultar RecyclerView
            binding.ivPostMedia.visibility = View.VISIBLE
            binding.rvSelectedImages.visibility = View.GONE
        }
    }

    private fun subirPublicacion() {
        // Validar que haya al menos una imagen o video
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Debes seleccionar al menos una imagen o video", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener el título
        val titulo = binding.etTituloPublicar.text.toString().trim()
        if (titulo.isEmpty()) {
            Toast.makeText(this, "Debes agregar un título", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener descripción y ubicación
        val descripcion = binding.etDescPublicar.text.toString().trim()
        val ubicacion = binding.etLocationPublicar.text.toString().trim()
        val isPublic = true // Siempre público

        // Obtener userId desde SessionManager
        val userId = SessionManager.getUserId()

        if (userId == null) {
            Toast.makeText(this, "Error: Sesión no válida. Por favor inicia sesión nuevamente.", Toast.LENGTH_SHORT).show()
            return
        }

        // Convertir URIs a Files
        val imageFiles = selectedImageUris.mapNotNull { uri ->
            getFileFromUri(uri)
        }

        if (imageFiles.isEmpty()) {
            Toast.makeText(this, "Error al procesar los archivos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear la publicación
        postViewModel.createPost(
            userId = userId,
            title = titulo,
            description = descripcion,
            location = ubicacion,
            isPublic = isPublic,
            imageFiles = imageFiles
        )
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(cacheDir, "upload_${System.currentTimeMillis()}_${selectedImageUris.indexOf(uri)}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}