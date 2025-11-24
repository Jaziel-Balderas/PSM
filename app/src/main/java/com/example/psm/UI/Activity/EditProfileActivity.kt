package com.example.psm.UI.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
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

class EditProfileActivity : AppCompatActivity() {

    // Declaración a nivel de clase (Acceso en todos los métodos)
    private lateinit var authViewModel: AuthVIewModel
    private lateinit var userRepository: UserRepository

    // Captura de EditTexts (Ajustar IDs si son diferentes de los que usaste antes)
    private lateinit var etName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var etNickname: EditText
    private lateinit var etPassword: EditText // Añadir la contraseña para el UPDATE
    private lateinit var btnConfirm: Button

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

        // 2. Captura de Vistas (CRÍTICO: Deben ser inicializadas antes de ser usadas)
        etName = findViewById(R.id.NameBoxUP)
        etLastName = findViewById(R.id.LastNameBoxUP)
        etEmail = findViewById(R.id.MailBoxUP)
        etPhone = findViewById(R.id.PhoneBoxUP)
        etAddress = findViewById(R.id.AddressBoxUP)
        etNickname = findViewById(R.id.NickNameBoxUP)
        etPassword = findViewById(R.id.PasswordBoxUP) // Necesario para enviar el campo NEW_PASSWORD
        btnConfirm = findViewById(R.id.register_buttonUP)

        // 3. Cargar Datos y Configurar Patrones
        loadUserDataFromSingleton() // Llenar los campos con datos del Singleton
        setupObservers()           // Configurar la reacción a la actualización

        // 4. Listeners (Conexión Final de la Vista)
        btnConfirm.setOnClickListener {
            attemptUpdateProfile() // Llama a la lógica del Model/Controller
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<ImageButton>(R.id.back_buttonLoginReg3)
        btnBack.setOnClickListener {
            finish() // Simplemente vuelve al Fragment anterior (ProfileLayout)
        }
    }

    // Patrón SINGLETON: Carga los datos del perfil actual desde SQLite
    private fun loadUserDataFromSingleton() {
        // Acceder a la única instancia de la sesión (Singleton)
        var user = SessionManager.currentUser
        
        // Si no hay usuario en memoria, intentar cargar desde SQLite
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
            // La contraseña se deja vacía por seguridad (solo se actualiza si el usuario ingresa una nueva)
        } else {
            Toast.makeText(this, "Error: No hay sesión activa.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    // Patrón OBSERVER: Configura la reacción a los resultados del Controller
    private fun setupObservers() {
        authViewModel.updateStatus.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                Toast.makeText(this, "✓ Perfil actualizado exitosamente", Toast.LENGTH_SHORT).show()
                // Dar tiempo para que se actualice el SessionManager y SQLite
                finish() // Vuelve al perfil para que se recargue la vista
            }
        })
        
        authViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty() && authViewModel.updateStatus.value == false) {
                Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
            }
        })
    }

    // Patrón MVC: Función que recolecta y llama al Controller
    private fun attemptUpdateProfile() {
        // Validación básica
        val name = etName.text.toString().trim()
        val lastname = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val username = etNickname.text.toString().trim()
        
        if (name.isEmpty() || lastname.isEmpty() || email.isEmpty() || 
            phone.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Por favor completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 1. Recolectar datos del formulario
        val request = UpdateProfileRequest(
            userId = SessionManager.currentUser?.userId ?: run {
                Toast.makeText(this, "Error de sesión.", Toast.LENGTH_SHORT).show()
                return
            },
            nameuser = name,
            lastnames = lastname,
            email = email,
            phone = phone,
            direccion = etAddress.text.toString().trim().takeIf { it.isNotEmpty() },
            username = username,
            newPassword = etPassword.text.toString().trim().takeIf { it.isNotEmpty() }
        )

        // 2. Llamar al Controller
        authViewModel.updateProfile(request)
    }
}