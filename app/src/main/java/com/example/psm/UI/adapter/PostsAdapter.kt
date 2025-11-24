package com.example.psm.UI.adapter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.psm.R
import Model.data.Post
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostsAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClick: (Post, Int) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserProfile: CircleImageView = view.findViewById(R.id.ivUserProfile)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvTimeAgo: TextView = view.findViewById(R.id.tvTimeAgo)
        val vpPostMedia: ViewPager2 = view.findViewById(R.id.vpPostMedia)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        // Información del usuario
        holder.tvUsername.text = post.username ?: "Usuario"
        
        // Foto de perfil - cargar desde base64
        if (!post.profileImageBase64.isNullOrEmpty()) {
            try {
                val bitmap = base64ToBitmap(post.profileImageBase64)
                holder.ivUserProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                holder.ivUserProfile.setImageResource(R.drawable.olivia)
            }
        } else {
            holder.ivUserProfile.setImageResource(R.drawable.olivia)
        }
        
        // Ubicación
        if (!post.location.isNullOrEmpty()) {
            holder.tvLocation.text = post.location
            holder.tvLocation.visibility = View.VISIBLE
        } else {
            holder.tvLocation.visibility = View.GONE
        }
        
        // Tiempo transcurrido
        holder.tvTimeAgo.text = getTimeAgo(post.createdAt ?: "")
        
        // Título
        holder.tvTitle.text = post.title
        
        // Descripción
        if (post.description.isNotEmpty()) {
            holder.tvDescription.text = post.description
            holder.tvDescription.visibility = View.VISIBLE
        } else {
            holder.tvDescription.visibility = View.GONE
        }
        
        // ViewPager para imágenes (base64)
        val mediaAdapter = PostMediaAdapter(post.imageUrls)
        holder.vpPostMedia.adapter = mediaAdapter
        
        // Likes
        holder.tvLikeCount.text = post.likeCount.toString()
        updateLikeButton(holder.btnLike, post.isLiked)
        
        holder.btnLike.setOnClickListener {
            onLikeClick(post, position)
        }
        
        // Botón compartir
        holder.btnShare.setOnClickListener {
            sharePost(holder.itemView.context, post)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
    
    fun updatePostLike(position: Int, isLiked: Boolean, likeCount: Int) {
        if (position in posts.indices) {
            posts[position].isLiked = isLiked
            posts[position].likeCount = likeCount
            notifyItemChanged(position)
        }
    }
    
    private fun updateLikeButton(button: ImageButton, isLiked: Boolean) {
        if (isLiked) {
            button.setImageResource(R.drawable.ic_heart_filled)
        } else {
            button.setImageResource(R.drawable.ic_heart_outline)
        }
    }
    
    private fun sharePost(context: android.content.Context, post: Post) {
        val shareText = buildString {
            append(post.title)
            if (post.description.isNotEmpty()) {
                append("\n\n")
                append(post.description)
            }
            if (!post.location.isNullOrEmpty()) {
                append("\n📍 ")
                append(post.location)
            }
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, post.title)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Compartir publicación"))
    }
    
    private fun base64ToBitmap(base64String: String): Bitmap {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }
    
    private fun getTimeAgo(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(dateString)
            val now = Date()
            val diff = now.time - (date?.time ?: 0)
            
            val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            
            when {
                seconds < 60 -> "Ahora"
                minutes < 60 -> "Hace ${minutes}m"
                hours < 24 -> "Hace ${hours}h"
                days < 7 -> "Hace ${days}d"
                else -> {
                    val outputFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                    outputFormat.format(date ?: Date())
                }
            }
        } catch (e: Exception) {
            "Reciente"
        }
    }
}