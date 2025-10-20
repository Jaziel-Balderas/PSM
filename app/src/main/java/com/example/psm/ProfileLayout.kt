package com.example.psm

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ProfileLayout : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_layout, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSett = view.findViewById<ImageButton>(R.id.btnSettings)
        val btnEdit = view.findViewById<ImageButton>(R.id.btnEditarPerfil)
        btnSett?.setOnClickListener{
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
        }
        btnEdit?.setOnClickListener{
            val intent = Intent(requireContext(), RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}