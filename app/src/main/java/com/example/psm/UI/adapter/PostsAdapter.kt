package com.example.psm.UI.adapter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
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
    private val onLikeClick: (Post, Int) -> Unit,
    private val onDislikeClick: (Post, Int) -> Unit,
    private val onCommentClick: (Post, Int) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserProfile: CircleImageView = view.findViewById(R.id.ivUserProfile)
        val tvUsername: TextView = view.findViewById(R.id.tvUsername)
        val tvLocation: TextView = view.findViewById(R.id.tvLocation)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val vpPostMedia: ViewPager2 = view.findViewById(R.id.vpPostMedia)
        val btnLike: ImageButton = view.findViewById(R.id.btnLike)
        val tvLikeCount: TextView = view.findViewById(R.id.tvLikeCount)
        val btnDislike: ImageButton = view.findViewById(R.id.btnDislike)
        val tvDislikeCount: TextView = view.findViewById(R.id.tvDislikeCount)
        val btnComment: ImageButton = view.findViewById(R.id.btnComment)
        val tvCommentCount: TextView = view.findViewById(R.id.tvCommentCount)
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
        Log.d("PostsAdapter", "onBindViewHolder position=$position, postId=${post.postId}, title=${post.title}")
        
        // Información del usuario - mostrar nombre completo si está disponible
        val displayName = if (!post.nameuser.isNullOrEmpty()) {
            "${post.nameuser} ${post.lastnames ?: ""}".trim()
        } else {
            post.username ?: "Usuario"
        }
        holder.tvUsername.text = displayName
        
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
        
        // Fecha y hora de creación
        holder.tvDateTime.text = formatDateTime(post.createdAt ?: "")
        
        // Título
        holder.tvTitle.text = post.title.takeIf { it.isNotEmpty() } ?: "Sin título"
        
        // Descripción
        if (!post.description.isNullOrEmpty()) {
            holder.tvDescription.text = post.description
            holder.tvDescription.visibility = View.VISIBLE
        } else {
            holder.tvDescription.visibility = View.GONE
        }
        
        // ViewPager para imágenes (base64)
        val mediaAdapter = PostMediaAdapter(post.images.map { it.base64 })
        holder.vpPostMedia.adapter = mediaAdapter
        
        // Likes
        holder.tvLikeCount.text = post.likesCount.toString()
        val liked = post.userVote == 1
        updateLikeButton(holder.btnLike, liked)
        
        holder.btnLike.setOnClickListener {
            onLikeClick(post, position)
        }
        
        // Dislikes
        holder.tvDislikeCount.text = post.dislikesCount.toString()
        val disliked = post.userVote == -1
        updateDislikeButton(holder.btnDislike, disliked)
        
        holder.btnDislike.setOnClickListener {
            onDislikeClick(post, position)
        }
        
        // Comentarios
        holder.tvCommentCount.text = post.commentsCount.toString()
        
        holder.btnComment.setOnClickListener {
            onCommentClick(post, position)
        }
        
        // Botón compartir
        holder.btnShare.setOnClickListener {
            sharePost(holder.itemView.context, post)
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        Log.d("PostsAdapter", "updatePosts called with ${newPosts.size} posts")
        posts.clear()
        posts.addAll(newPosts)
        Log.d("PostsAdapter", "Posts list now has ${posts.size} items")
        notifyDataSetChanged()
    }
    
    fun updatePostVote(position: Int, userVote: Int?, likesCount: Int, dislikesCount: Int) {
        if (position in posts.indices) {
            val current = posts[position]
            posts[position] = current.copy(userVote = userVote, likesCount = likesCount, dislikesCount = dislikesCount)
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
    
    private fun updateDislikeButton(button: ImageButton, isDisliked: Boolean) {
        if (isDisliked) {
            button.setImageResource(R.drawable.ic_heart_broken_filled)
        } else {
            button.setImageResource(R.drawable.ic_heart_broken)
        }
    }
    
    private fun sharePost(context: android.content.Context, post: Post) {
        val shareText = buildString {
            if (post.title.isNotEmpty()) {
                append(post.title)
            }
            if (!post.description.isNullOrEmpty()) {
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