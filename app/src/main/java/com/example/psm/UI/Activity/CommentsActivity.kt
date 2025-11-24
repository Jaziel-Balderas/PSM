package com.example.psm.UI.Activity

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.psm.R
import com.example.psm.UI.adapter.CommentsAdapter
import com.example.psm.UI.adapter.RepliesAdapter
import Model.data.Comment
import Model.repository.CommentRepository
import Model.repository.SessionManager
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    
    private lateinit var commentsAdapter: CommentsAdapter
    private val repository = CommentRepository()
    
    private var postId: Int = 0
    private var userId: Int = 0
    private var postTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)
        
        // Inicializar SessionManager
        SessionManager.init(this)
        
        // Obtener datos del intent
        postId = intent.getStringExtra("post_id")?.toIntOrNull() ?: 0
        postTitle = intent.getStringExtra("post_title") ?: "Publicación"
        
        val userIdString = SessionManager.getUserId()
        userId = userIdString?.toIntOrNull() ?: 0
        
        if (postId <= 0) {
            Toast.makeText(this, "Error: Publicación inválida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        if (userId <= 0) {
            Toast.makeText(this, "Error: Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        Log.d("CommentsActivity", "postId=$postId, userId=$userId, title=$postTitle")
        
        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadComments()
    }
    
    private fun initializeViews() {
        rvComments = findViewById(R.id.rvComments)
        etComment = findViewById(R.id.etComment)
        btnSendComment = findViewById(R.id.btnSendComment)
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        
        tvTitle.text = "Comentarios"
    }
    
    private fun setupRecyclerView() {
        commentsAdapter = CommentsAdapter(
            comments = mutableListOf(),
            onLikeClick = { comment, position ->
                handleLikeClick(comment, position)
            },
            onReplyClick = { comment, position ->
                handleReplyClick(comment, position)
            },
            onSendReply = { comment, replyText, position ->
                handleSendReply(comment, replyText, position)
            },
            onLoadReplies = { comment, position ->
                handleLoadReplies(comment, position)
            }
        )
        
        rvComments.apply {
            layoutManager = LinearLayoutManager(this@CommentsActivity)
            adapter = commentsAdapter
        }
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnSendComment.setOnClickListener {
            sendComment()
        }
    }
    
    private fun loadComments() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE
        
        Log.d("CommentsActivity", "Loading comments for postId=$postId, userId=$userId")
        
        lifecycleScope.launch {
            try {
                val response = repository.getComments(postId, userId)
                
                progressBar.visibility = View.GONE
                
                Log.d("CommentsActivity", "Response: success=${response?.success}, count=${response?.count}")
                
                if (response != null && response.success) {
                    if (response.comments.isNotEmpty()) {
                        Log.d("CommentsActivity", "Showing ${response.comments.size} comments")
                        commentsAdapter.updateComments(response.comments)
                        rvComments.visibility = View.VISIBLE
                        tvEmptyState.visibility = View.GONE
                    } else {
                        Log.d("CommentsActivity", "No comments found")
                        rvComments.visibility = View.GONE
                        tvEmptyState.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("CommentsActivity", "Error response: ${response?.message}")
                    Toast.makeText(this@CommentsActivity, response?.message ?: "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
                    rvComments.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Exception loading comments", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                rvComments.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
            }
        }
    }
    
    private fun sendComment() {
        val commentText = etComment.text.toString().trim()
        
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Escribe un comentario", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Deshabilitar botón mientras se envía
        btnSendComment.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        Log.d("CommentsActivity", "Sending comment: postId=$postId, userId=$userId, text=$commentText")
        
        lifecycleScope.launch {
            try {
                val response = repository.createComment(postId, userId, commentText)
                
                progressBar.visibility = View.GONE
                btnSendComment.isEnabled = true
                
                Log.d("CommentsActivity", "Comment response: success=${response?.success}")
                
                if (response != null && response.success && response.comment != null) {
                    Log.d("CommentsActivity", "Comment created successfully")
                    
                    // Limpiar el campo de texto
                    etComment.text.clear()
                    
                    // Agregar el comentario al adapter
                    commentsAdapter.addComment(response.comment)
                    
                    // Scroll al inicio
                    rvComments.scrollToPosition(0)
                    
                    // Ocultar mensaje de vacío
                    tvEmptyState.visibility = View.GONE
                    rvComments.visibility = View.VISIBLE
                    
                    Toast.makeText(this@CommentsActivity, "Comentario publicado", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("CommentsActivity", "Error creating comment: ${response?.message}")
                    Toast.makeText(this@CommentsActivity, response?.message ?: "Error al publicar comentario", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Exception creating comment", e)
                progressBar.visibility = View.GONE
                btnSendComment.isEnabled = true
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleLikeClick(comment: Comment, position: Int) {
        Log.d("CommentsActivity", "Like comment: commentId=${comment.commentId}, position=$position")
        
        lifecycleScope.launch {
            try {
                val response = repository.likeComment(comment.commentId, userId)
                
                if (response != null && response.success) {
                    Log.d("CommentsActivity", "Like updated: liked=${response.liked}, count=${response.likesCount}")
                    commentsAdapter.updateCommentLike(position, response.liked, response.likesCount)
                } else {
                    Toast.makeText(this@CommentsActivity, response?.message ?: "Error al dar me gusta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Exception liking comment", e)
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleReplyClick(comment: Comment, position: Int) {
        // Esta función ahora solo se usa si queremos hacer algo adicional
        // El toggle del campo de respuesta ya se maneja en el adapter
        Log.d("CommentsActivity", "Reply to ${comment.username}")
    }
    
    private fun handleSendReply(comment: Comment, replyText: String, position: Int) {
        Log.d("CommentsActivity", "Sending reply to commentId=${comment.commentId}: $replyText")
        
        lifecycleScope.launch {
            try {
                val response = repository.createReply(comment.commentId, userId, replyText)
                
                if (response != null && response.success && response.reply != null) {
                    Log.d("CommentsActivity", "Reply created successfully")
                    
                    // Actualizar el contador de respuestas
                    commentsAdapter.updateRepliesCount(position, comment.repliesCount + 1)
                    
                    // Recargar las respuestas para mostrar la nueva
                    handleLoadReplies(comment, position)
                    
                    Toast.makeText(this@CommentsActivity, "Respuesta publicada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CommentsActivity, response?.message ?: "Error al publicar respuesta", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Exception creating reply", e)
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleLoadReplies(comment: Comment, position: Int) {
        Log.d("CommentsActivity", "Loading replies for commentId=${comment.commentId}")
        
        lifecycleScope.launch {
            try {
                val response = repository.getReplies(comment.commentId)
                
                if (response != null && response.success) {
                    Log.d("CommentsActivity", "Loaded ${response.replies.size} replies")
                    
                    // Actualizar el RecyclerView de respuestas en el ViewHolder
                    val viewHolder = rvComments.findViewHolderForAdapterPosition(position) as? CommentsAdapter.CommentViewHolder
                    if (viewHolder != null) {
                        val repliesAdapter = RepliesAdapter(response.replies.toMutableList())
                        viewHolder.rvReplies.adapter = repliesAdapter
                        viewHolder.rvReplies.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(this@CommentsActivity, "Error al cargar respuestas", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Exception loading replies", e)
                Toast.makeText(this@CommentsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
