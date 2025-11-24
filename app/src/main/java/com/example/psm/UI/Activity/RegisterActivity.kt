package com.example.psm.UI.Activity


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.psm.R
import de.hdodenhof.circleimageview.CircleImageView
import com.example.psm.UI.controller.AuthVIewModel
import Model.repository.UserRepository
import Model.repository.SessionManager
import Model.data.RegisterRequest
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast
import android.widget.EditText
import com.example.psm.UI.controller.ProfileVIewModel
import com.example.psm.UI.controller.AuthViewModelFactory



class RegisterActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthVIewModel //Declaración del Controller
    private lateinit var profileImage: CircleImageView
    private val PICK_IMAGE = 100
    private var imageBase64: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val userRepository = UserRepository() //Inicializacion del controller
        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)

        val btnBack: Button = findViewById(R.id.back_buttonLoginReg)
        val registerbutton: Button = findViewById(R.id.register_button)

        btnBack.setOnClickListener {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        val etName = findViewById<EditText>(R.id.NameBox)
        val etLastnames = findViewById<EditText>(R.id.LastNameBox)
        val etUsername = findViewById<EditText>(R.id.NickNameBox)
        val etEmail = findViewById<EditText>(R.id.MailBox)
        val etPassword = findViewById<EditText>(R.id.PasswordBox)
        val etPhone = findViewById<EditText>(R.id.PhoneBox)
        val etDireccion = findViewById<EditText>(R.id.AddressBox)
        profileImage = findViewById(R.id.profileImage)

        authViewModel.registerStatus.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                // Si el registro fue exitoso y hay usuario en sesión, ir al Dashboard
                if (SessionManager.isLoggedIn) {
                    Toast.makeText(this, "¡Registro completo! Bienvenido.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                } else {
                    Toast.makeText(this, "¡Registro completo! Inicia sesión.", Toast.LENGTH_LONG).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                }
            }
        })
        authViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty() && !authViewModel.registerStatus.value!!) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE)
        }

        registerbutton.setOnClickListener {
            val request = RegisterRequest(
                nameuser = etName.text.toString(),
                lastnames = etLastnames.text.toString(),
                username = etUsername.text.toString(),
                password = etPassword.text.toString(),
                email = etEmail.text.toString(),
                phone = etPhone.text.toString(),
                direccion = etDireccion.text.toString().takeIf { it.isNotEmpty() },
                profile_image_url = imageBase64
            )

            // Validación mínima para campos esenciales
            if (request.username.isEmpty() || request.password.isEmpty() || request.email.isEmpty() || request.lastnames.isEmpty()
                || request.nameuser.isEmpty()) {
                Toast.makeText(this, "Completa campos obligatorios.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.register(request)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                profileImage.setImageURI(imageUri)

                // Lógica para BLOB
                val inputStream = contentResolver.openInputStream(imageUri)
                // Leer todos los bytes del InputStream
                val imageBytes = inputStream?.readBytes()
                // Codificar los bytes a cadena Base64
                imageBase64 = if (imageBytes != null) {
                    android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT)
                } else {
                    null
                }
            }
        }
    }
}