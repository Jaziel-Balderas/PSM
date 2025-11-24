package com.example.psm.UI.Activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.R
import com.example.psm.UI.Fragments.publicar
import Model.data.Draft
import Model.repository.DraftManager
import java.text.SimpleDateFormat
import java.util.*

class DraftsActivity : AppCompatActivity() {

    private lateinit var rvDrafts: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var draftsAdapter: DraftsAdapter
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drafts)

        // Inicializar DraftManager
        DraftManager.init(this)

        userId = getSharedPreferences("UserSession", MODE_PRIVATE).getInt("user_id", -1)
        
        if (userId == -1) {
            Toast.makeText(this, "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadDrafts()
    }

    private fun initViews() {
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        rvDrafts = findViewById(R.id.rvDrafts)
        emptyView = findViewById(R.id.emptyView)

        btnBack.setOnClickListener { finish() }

        rvDrafts.layoutManager = LinearLayoutManager(this)
    }

    private fun loadDrafts() {
        val drafts = DraftManager.getInstance().getAllDrafts(userId)
        
        Log.d("DraftsActivity", "Borradores cargados: ${drafts.size}")
        
        if (drafts.isEmpty()) {
            rvDrafts.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            rvDrafts.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
            
            draftsAdapter = DraftsAdapter(drafts.toMutableList(),
                onDraftClick = { draft -> openDraft(draft) },
                onDeleteClick = { draft -> deleteDraft(draft) }
            )
            rvDrafts.adapter = draftsAdapter
        }
    }

    private fun openDraft(draft: Draft) {
        val intent = Intent(this, publicar::class.java)
        intent.putExtra("draft_id", draft.id)
        intent.putExtra("draft_title", draft.title)
        intent.putExtra("draft_content", draft.content)
        intent.putExtra("draft_location", draft.location)
        intent.putExtra("draft_image", draft.imageBase64)
        intent.putExtra("draft_is_public", draft.isPublic)
        startActivity(intent)
        finish()
    }

    private fun deleteDraft(draft: Draft) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar borrador")
            .setMessage("¿Estás seguro de que quieres eliminar este borrador?")
            .setPositiveButton("Eliminar") { _, _ ->
                val success = DraftManager.getInstance().deleteDraft(draft.id)
                if (success) {
                    Toast.makeText(this, "Borrador eliminado", Toast.LENGTH_SHORT).show()
                    loadDrafts() // Recargar la lista
                } else {
                    Toast.makeText(this, "Error al eliminar borrador", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class DraftsAdapter(
    private val drafts: MutableList<Draft>,
    private val onDraftClick: (Draft) -> Unit,
    private val onDeleteClick: (Draft) -> Unit
) : RecyclerView.Adapter<DraftsAdapter.DraftViewHolder>() {

    class DraftViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivDraftImage: ImageView = view.findViewById(R.id.ivDraftImage)
        val tvDraftTitle: TextView = view.findViewById(R.id.tvDraftTitle)
        val tvDraftContent: TextView = view.findViewById(R.id.tvDraftContent)
        val tvDraftDate: TextView = view.findViewById(R.id.tvDraftDate)
        val btnDeleteDraft: ImageButton = view.findViewById(R.id.btnDeleteDraft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_draft, parent, false)
        return DraftViewHolder(view)
    }

    override fun onBindViewHolder(holder: DraftViewHolder, position: Int) {
        val draft = drafts[position]

        // Título
        holder.tvDraftTitle.text = if (!draft.title.isNullOrBlank()) {
            draft.title
        } else {
            "Sin título"
        }

        // Contenido
        holder.tvDraftContent.text = if (!draft.content.isNullOrBlank()) {
            draft.content
        } else {
            "Sin contenido"
        }

        // Fecha
        holder.tvDraftDate.text = getTimeAgo(draft.updatedAt)

        // Imagen
        if (!draft.imageBase64.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(draft.imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                holder.ivDraftImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivDraftImage.setImageResource(R.drawable.olivia)
            }
        } else {
            holder.ivDraftImage.setImageResource(R.drawable.olivia)
        }

        // Click en el item para abrir el borrador
        holder.itemView.setOnClickListener {
            onDraftClick(draft)
        }

        // Click en eliminar
        holder.btnDeleteDraft.setOnClickListener {
            onDeleteClick(draft)
        }
    }

    override fun getItemCount() = drafts.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "Hace ${days}d"
            hours > 0 -> "Hace ${hours}h"
            minutes > 0 -> "Hace ${minutes}m"
            else -> "Ahora"
        }
    }
}
