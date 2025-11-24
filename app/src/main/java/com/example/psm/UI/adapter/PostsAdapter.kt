package com.example.psm.UI.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.psm.R
import com.example.psm.UI.Activity.EditPostActivity
import Model.data.Post
import Model.repository.SessionManager
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostsAdapter(
    private val posts: MutableList<Post>,
    private val onLikeClick: (Post, Int) -> Unit,
    private val onDislikeClick: (Post, Int) -> Unit,
    private val onCommentClick: (Post, Int) -> Unit,
    private val onFavoriteClick: (Post, Int) -> Unit,
    private val onDeletePost: (Post, Int) -> Unit
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
        val btnFavorite: ImageButton = view.findViewById(R.id.btnFavorite)
        val btnShare: ImageButton = view.findViewById(R.id.btnShare)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val btnPostOptions: ImageButton = view.findViewById(R.id.btnPostOptions)
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
        
        // Favorito
        updateFavoriteButton(holder.btnFavorite, post.isFavorite)
        
        holder.btnFavorite.setOnClickListener {
            onFavoriteClick(post, position)
        }
        
        // Botón compartir
        holder.btnShare.setOnClickListener {
            sharePost(holder.itemView.context, post)
        }
        
        // Botón de opciones (solo visible si es el propietario del post)
        val currentUserId = SessionManager.getUserId()
        if (currentUserId != null && currentUserId == post.userId) {
            holder.btnPostOptions.visibility = View.VISIBLE
            holder.btnPostOptions.setOnClickListener { view ->
                showOptionsMenu(view, post, position)
            }
        } else {
            holder.btnPostOptions.visibility = View.GONE
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
    
    private fun updateFavoriteButton(button: ImageButton, isFavorite: Boolean) {
        if (isFavorite) {
            button.setImageResource(R.drawable.ic_bookmark)
        } else {
            button.setImageResource(R.drawable.ic_bookmark_border)
        }
    }
    
    fun updatePostFavorite(position: Int, isFavorite: Boolean) {
        if (position in posts.indices) {
            val current = posts[position]
            posts[position] = current.copy(isFavorite = isFavorite)
            notifyItemChanged(position)
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
    
    private fun showOptionsMenu(view: View, post: Post, position: Int) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.post_options_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    val intent = Intent(view.context, EditPostActivity::class.java)
                    intent.putExtra("post_id", post.postId)
                    intent.putExtra("post_user_id", post.userId)
                    intent.putExtra("post_title", post.title)
                    intent.putExtra("post_content", post.description ?: "")
                    intent.putExtra("post_location", post.location ?: "")
                    intent.putExtra("post_is_public", post.isPublic)
                    view.context.startActivity(intent)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation(view.context, post, position)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
    
    private fun showDeleteConfirmation(context: Context, post: Post, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar publicación")
            .setMessage("¿Estás seguro de que quieres eliminar esta publicación? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                onDeletePost(post, position)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    fun removePost(position: Int) {
        if (position in posts.indices) {
            posts.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, posts.size)
        }
    }
}