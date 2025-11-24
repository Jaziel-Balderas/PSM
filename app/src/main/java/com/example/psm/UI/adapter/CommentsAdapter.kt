package com.example.psm.UI.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.R
import Model.data.Comment
import Model.data.Reply
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CommentsAdapter(
    private val comments: MutableList<Comment>,
    private val onLikeClick: (Comment, Int) -> Unit,
    private val onReplyClick: (Comment, Int) -> Unit,
    private val onSendReply: (Comment, String, Int) -> Unit,
    private val onLoadReplies: (Comment, Int) -> Unit
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserProfile: CircleImageView = view.findViewById(R.id.ivUserProfile)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvCommentText: TextView = view.findViewById(R.id.tvCommentText)
        val btnLikeComment: ImageButton = view.findViewById(R.id.btnLikeComment)
        val tvLikesCount: TextView = view.findViewById(R.id.tvLikesCount)
        val btnReply: TextView = view.findViewById(R.id.btnReply)
        val tvRepliesCount: TextView = view.findViewById(R.id.tvRepliesCount)
        val llReplyInput: LinearLayout = view.findViewById(R.id.llReplyInput)
        val etReply: EditText = view.findViewById(R.id.etReply)
        val btnSendReply: ImageButton = view.findViewById(R.id.btnSendReply)
        val rvReplies: RecyclerView = view.findViewById(R.id.rvReplies)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        // Usuario - mostrar nombre completo si está disponible
        val displayName = if (comment.nameuser.isNotEmpty()) {
            "${comment.nameuser} ${comment.lastnames}".trim()
        } else {
            comment.username.takeIf { it.isNotEmpty() } ?: "Usuario"
        }
        holder.tvUsername.text = displayName
        
        // Foto de perfil - cargar desde base64
        if (!comment.profileImageBase64.isNullOrEmpty()) {
            try {
                val bitmap = base64ToBitmap(comment.profileImageBase64)
                holder.ivUserProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivUserProfile.setImageResource(R.drawable.perfilejemplo)
            }
        } else {
            holder.ivUserProfile.setImageResource(R.drawable.perfilejemplo)
        }
        
        // Fecha y hora de creación
        holder.tvDateTime.text = formatDateTime(comment.createdAt)
        
        // Texto del comentario
        holder.tvCommentText.text = comment.commentText
        
        // Likes
        holder.tvLikesCount.text = comment.likesCount.toString()
        updateLikeButton(holder.btnLikeComment, comment.userLiked)
        
        holder.btnLikeComment.setOnClickListener {
            onLikeClick(comment, position)
        }
        
        // Mostrar cantidad de respuestas si hay
        if (comment.repliesCount > 0) {
            holder.tvRepliesCount.text = "${comment.repliesCount} respuesta${if (comment.repliesCount > 1) "s" else ""}"
            holder.tvRepliesCount.visibility = View.VISIBLE
        } else {
            holder.tvRepliesCount.visibility = View.GONE
        }
        
        // Click en "Responder"
        holder.btnReply.setOnClickListener {
            // Toggle del campo de respuesta
            if (holder.llReplyInput.visibility == View.VISIBLE) {
                holder.llReplyInput.visibility = View.GONE
            } else {
                holder.llReplyInput.visibility = View.VISIBLE
                holder.etReply.requestFocus()
            }
        }
        
        // Click en enviar respuesta
        holder.btnSendReply.setOnClickListener {
            val replyText = holder.etReply.text.toString().trim()
            if (replyText.isNotEmpty()) {
                onSendReply(comment, replyText, position)
                holder.etReply.text.clear()
                holder.llReplyInput.visibility = View.GONE
            }
        }
        
        // Click en el contador de respuestas para cargarlas
        holder.tvRepliesCount.setOnClickListener {
            if (holder.rvReplies.visibility == View.VISIBLE) {
                holder.rvReplies.visibility = View.GONE
            } else {
                onLoadReplies(comment, position)
            }
        }
        
        // Configurar RecyclerView de respuestas
        holder.rvReplies.layoutManager = LinearLayoutManager(holder.itemView.context)
    }

    override fun getItemCount(): Int = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
    
    fun addComment(comment: Comment) {
        comments.add(0, comment)
        notifyItemInserted(0)
    }
    
    fun updateCommentLike(position: Int, liked: Boolean, likesCount: Int) {
        if (position in comments.indices) {
            val current = comments[position]
            comments[position] = current.copy(userLiked = liked, likesCount = likesCount)
            notifyItemChanged(position)
        }
    }
    
    fun updateRepliesCount(position: Int, repliesCount: Int) {
        if (position in comments.indices) {
            val current = comments[position]
            comments[position] = current.copy(repliesCount = repliesCount)
            notifyItemChanged(position)
        }
    }
    
    fun showReplies(position: Int, replies: List<Reply>) {
        // Esta función será llamada por la Activity para actualizar las respuestas
        notifyItemChanged(position)
    }
    
    private fun updateLikeButton(button: ImageButton, isLiked: Boolean) {
        if (isLiked) {
            button.setImageResource(R.drawable.ic_heart_filled)
        } else {
            button.setImageResource(R.drawable.ic_heart_outline)
        }
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
