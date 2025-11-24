package com.example.psm.UI.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.psm.R
import com.example.psm.UI.controller.AuthVIewModel
import Model.repository.UserRepository
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.widget.Toast
import com.example.psm.UI.controller.AuthViewModelFactory



class LoginActivity : AppCompatActivity(){
    private lateinit var authViewModel: AuthVIewModel

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val btnBack: Button = findViewById(R.id.back_buttonLogin)
        val btnLogg: Button = findViewById(R.id.buttonLog)
        val usertext = findViewById<EditText>(R.id.editTextText)
        val passw = findViewById<EditText>(R.id.editTextTextPassword)

        val userRepository = UserRepository() // Instancia del Model
        // Usamos el Factory para inicializar el Controller con el Model

        authViewModel = ViewModelProvider(this, AuthViewModelFactory(userRepository))
            .get(AuthVIewModel::class.java)

        authViewModel.loginStatus.observe(this, Observer { isSuccess ->
            if (isSuccess) {
                // El login fue exitoso (notificado por el Model/API)
                Toast.makeText(this, "¡Inicio de sesión exitoso!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
        })
        authViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })

        btnBack.setOnClickListener {
            val intent: Intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        // 3. CONECTAR EL BOTÓN AL CONTROLLER (MVC)
        btnLogg.setOnClickListener {
            val username = usertext.text.toString()
            val password = passw.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            authViewModel.login(username, password)

        }

    }
}
