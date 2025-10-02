package com.example.psm

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


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val FI: Animation = AnimationUtils.loadAnimation(this, R.anim.animarriba)
        val FO: Animation = AnimationUtils.loadAnimation(this, R.anim.animbajo)
        val mainLayout = findViewById<android.view.View>(R.id.main)

        val textTitle: TextView = findViewById(R.id.Title)
        val textSubtitle: TextView = findViewById(R.id.subtitle)

        val btn: Button = findViewById(R.id.buttonRegistro)
        btn.setOnClickListener {
            val intent: Intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        val btnLogin: Button = findViewById(R.id.buttonLogin)
        btnLogin.setOnClickListener {
            val intent: Intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        textTitle.setAnimation(FO);
        textSubtitle.setAnimation(FO);
        btn.setAnimation(FI);
        btnLogin.setAnimation(FI);

    }
}