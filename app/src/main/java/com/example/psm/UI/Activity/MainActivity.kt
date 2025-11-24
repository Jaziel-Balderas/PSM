package com.example.psm.UI.Activity

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.view.animation.Animation
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.animation.AnimationUtils
import android.widget.TextView
import com.example.psm.R
import Model.repository.SessionManager


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar SessionManager
        SessionManager.init(this)
        
        // Verificar si hay sesión activa
        if (SessionManager.isLoggedIn && SessionManager.loadSessionFromCache()) {
            // Hay sesión activa, ir directamente al Dashboard
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
        
        enableEdgeToEdge()
        // Configura la ventana para pantalla completa
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        // Manejo de barras del sistema (para enableEdgeToEdge)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Carga las animaciones
        val FI: Animation = AnimationUtils.loadAnimation(this, R.anim.animarriba)
        val FO: Animation = AnimationUtils.loadAnimation(this, R.anim.animbajo)

        // Obtiene referencias a las vistas
        val textTitle: TextView = findViewById(R.id.Title)
        val textSubtitle: TextView = findViewById(R.id.subtitle)
        val btnRegistro: Button = findViewById(R.id.buttonRegistro)
        val btnLogin: Button = findViewById(R.id.buttonLogin)

        // **CORRECCIÓN CLAVE:** Inicia la animación en las vistas
        textTitle.startAnimation(FO)
        textSubtitle.startAnimation(FO)
        btnRegistro.startAnimation(FI)
        btnLogin.startAnimation(FI)


        // Configura el Listener para el botón de Registro
        btnRegistro.setOnClickListener {
            val intent: Intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        // Configura el Listener para el botón de Login
        btnLogin.setOnClickListener {
            val intent: Intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }
}