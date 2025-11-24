package com.example.psm.UI.Activity

import android.content.Intent // <--- ASEGÚRATE DE TENER ESTO IMPORTADO
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.psm.UI.Fragments.NotiLayout
import com.example.psm.UI.Fragments.ProfileLayout
import com.example.psm.R
import com.example.psm.UI.Fragments.dashlayout
import com.example.psm.UI.Fragments.publicar
import com.example.psm.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainDashboard) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navegación del menú inferior (Fragments)
        binding.btnMenu.setOnClickListener {
            goToFragment(dashlayout())
        }
        binding.btnNotis.setOnClickListener {
            goToFragment(NotiLayout())
        }
        binding.btnProfile.setOnClickListener {
            goToFragment(ProfileLayout())
        }

        binding.btnFabAgregar.setOnClickListener {
            // Se usa Intent porque 'publicar' es una ACTIVITY, no un Fragment
            val intent = Intent(this, publicar::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        // ---------------------------------------------

        if (savedInstanceState == null) {
            goToFragment(dashlayout())
        }
    }

    private fun goToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentcontainer, fragment)
            .commit()
    }
}