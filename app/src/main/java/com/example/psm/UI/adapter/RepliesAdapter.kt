package com.example.psm.UI.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.R
import Model.data.Reply
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class RepliesAdapter(
    private val replies: MutableList<Reply>
) : RecyclerView.Adapter<RepliesAdapter.ReplyViewHolder>() {

    class ReplyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserProfile: CircleImageView = view.findViewById(R.id.ivUserProfile)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvReplyText: TextView = view.findViewById(R.id.tvReplyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        
        // Usuario - mostrar nombre completo si está disponible
        val displayName = if (reply.nameuser.isNotEmpty()) {
            "${reply.nameuser} ${reply.lastnames}".trim()
        } else {
            reply.username.takeIf { it.isNotEmpty() } ?: "Usuario"
        }
        holder.tvUsername.text = displayName
        
        // Foto de perfil - cargar desde base64
        if (!reply.profileImageBase64.isNullOrEmpty()) {
            try {
                val bitmap = base64ToBitmap(reply.profileImageBase64)
                holder.ivUserProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivUserProfile.setImageResource(R.drawable.perfilejemplo)
            }
        } else {
            holder.ivUserProfile.setImageResource(R.drawable.perfilejemplo)
        }
        
        // Fecha y hora de creación
        holder.tvDateTime.text = formatDateTime(reply.createdAt)
        
        // Texto de la respuesta
        holder.tvReplyText.text = reply.replyText
    }

    override fun getItemCount(): Int = replies.size

    fun updateReplies(newReplies: List<Reply>) {
        replies.clear()
        replies.addAll(newReplies)
        notifyDataSetChanged()
    }
    
    fun addReply(reply: Reply) {
        replies.add(reply)
        notifyItemInserted(replies.size - 1)
    }
    
    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
    
    private fun formatDateTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }
}
