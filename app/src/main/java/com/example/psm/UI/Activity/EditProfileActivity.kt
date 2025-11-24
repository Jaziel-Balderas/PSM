package com.example.psm.UI.Activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.psm.R
import com.example.psm.UI.Fragments.ProfileLayout
import com.example.psm.UI.controller.AuthVIewModel
import Model.repository.UserRepository
import Model.repository.SessionManager
import Model.data.User
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import com.example.psm.UI.controller.AuthViewModelFactory
import Model.data.UpdateProfileRequest
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    // Declaración a nivel de clase (Acceso en todos los métodos)
    private lateinit var authViewModel: AuthVIewModel
    private lateinit var userRepository: UserRepository

    // Captura de EditTexts
    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnConfirm: Button
    private lateinit var profileImageEdit: CircleImageView
    private lateinit var btnChangePhoto: ImageButton
    
    private var selectedImageBase64: String? = null
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            handleImageSelection(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        
        // Inicializar SessionManager
        SessionManager.init(this)

        // 1. Inicialización de Repositorios y ViewModels
        userRepository = UserRepository()
        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)

        // 2. Captura de Vistas
        etName = findViewById(R.id.NameBoxUP)
        etLastName = findViewById(R.id.LastNameBoxUP)
        etEmail = findViewById(R.id.MailBoxUP)
        etPhone = findViewById(R.id.PhoneBoxUP)
        etAddress = findViewById(R.id.AddressBoxUP)
        etNickname = findViewById(R.id.NickNameBoxUP)
        etPassword = findViewById(R.id.PasswordBoxUP)
        btnConfirm = findViewById(R.id.register_buttonUP)
        profileImageEdit = findViewById(R.id.profileImageEdit)
        btnChangePhoto = findViewById(R.id.btnChangePhoto)

        // 3. Cargar Datos y Configurar Patrones
        loadUserDataFromSingleton()
        setupObservers()

        // 4. Listeners
        btnConfirm.setOnClickListener {
            attemptUpdateProfile()
        }
        
        btnChangePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.back_buttonLoginReg3)
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun handleImageSelection(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Redimensionar si es muy grande
            val resizedBitmap = if (bitmap.width > 800 || bitmap.height > 800) {
                val ratio = Math.min(800.0 / bitmap.width, 800.0 / bitmap.height)
                val width = (bitmap.width * ratio).toInt()
                val height = (bitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }
            
            // Convertir a base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            
            // Mostrar en la vista
            profileImageEdit.setImageBitmap(resizedBitmap)
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error al cargar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserDataFromSingleton() {
        var user = SessionManager.currentUser
        
        if (user == null) {
            if (SessionManager.loadSessionFromCache()) {
                user = SessionManager.currentUser
            }
        }
        
        if (user != null) {
            etName.setText(user.nameuser)
            etLastName.setText(user.lastnames)
            etEmail.setText(user.email)
            etPhone.setText(user.phone)
            etAddress.setText(user.direccion ?: "")
            etNickname.setText(user.username)
            
            // Cargar foto de perfil si existe
            user.profile_image_url?.let { base64 ->
                if (base64.isNotEmpty()) {
                    try {
                        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        profileImageEdit.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Mantener imagen por defecto
                    }
                }
            }
        } else {
            Toast.makeText(this, "Error: No hay sesión activa.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupObservers() {
        authViewModel.updateStatus.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this, "✓ Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
        
        authViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty() && authViewModel.updateStatus.value == false) {
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun attemptUpdateProfile() {
        val currentUser = SessionManager.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Obtener valores de los campos
        val name = etName.text.toString().trim()
        val lastname = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val username = etNickname.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val password = etPassword.text.toString().trim()
        
        // Verificar que al menos un campo haya cambiado
        val hasChanges = name != currentUser.nameuser ||
                        lastname != currentUser.lastnames ||
                        email != currentUser.email ||
                        phone != currentUser.phone ||
                        address != (currentUser.direccion ?: "") ||
                        username != currentUser.username ||
                        password.isNotEmpty() ||
                        selectedImageBase64 != null
        
        if (!hasChanges) {
            Toast.makeText(this, "No hay cambios para guardar", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validar email solo si cambió y no está vacío
        if (email.isNotEmpty() && email != currentUser.email) {
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Correo electrónico inválido", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        // Validar contraseña solo si se proporciona
        if (password.isNotEmpty() && password.length < 8) {
            Toast.makeText(this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Usar valores actuales si los campos están vacíos
        val request = UpdateProfileRequest(
            userId = currentUser.userId,
            nameuser = name.ifEmpty { currentUser.nameuser },
            lastnames = lastname.ifEmpty { currentUser.lastnames },
            email = email.ifEmpty { currentUser.email },
            phone = phone.ifEmpty { currentUser.phone },
            direccion = address.ifEmpty { currentUser.direccion },
            username = username.ifEmpty { currentUser.username },
            newPassword = password.takeIf { it.isNotEmpty() },
            profileImageBase64 = selectedImageBase64
        )

        authViewModel.updateProfile(request)
    }
}