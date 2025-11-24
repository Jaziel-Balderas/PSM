package com.example.psm.UI.Activity

import Model.data.Post
import Model.repository.PostRepository
import Model.repository.SessionManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.psm.R
import com.example.psm.UI.adapter.PostsAdapter
import kotlinx.coroutines.launch
import android.content.Intent

class FavoritesActivity : AppCompatActivity() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var spinnerOrder: Spinner
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvTitle: TextView
    
    private lateinit var postsAdapter: PostsAdapter
    private val repository = PostRepository()
    
    private var userId: Int = 0
    private var currentQuery: String = ""
    private var currentOrderBy: String = "date" // date, title, username

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        
        SessionManager.init(this)
        
        val userIdString = SessionManager.getUserId()
        userId = userIdString?.toIntOrNull() ?: 0
        
        if (userId <= 0) {
            Toast.makeText(this, "Error: Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeViews()
        setupSpinner()
        setupRecyclerView()
        setupSearchView()
        setupListeners()
        loadFavorites()
    }
    
    private fun initializeViews() {
        rvFavorites = findViewById(R.id.rvFavorites)
        searchView = findViewById(R.id.searchView)
        spinnerOrder = findViewById(R.id.spinnerOrder)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        btnBack = findViewById(R.id.btnBack)
        tvTitle = findViewById(R.id.tvTitle)
        
        tvTitle.text = "Favoritos"
    }
    
    private fun setupSpinner() {
        val orderOptions = arrayOf("Fecha", "Título", "Usuario")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrder.adapter = adapter
        
        spinnerOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentOrderBy = when (position) {
                    1 -> "title"
                    2 -> "username"
                    else -> "date"
                }
                loadFavorites()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupRecyclerView() {
        postsAdapter = PostsAdapter(
            posts = mutableListOf(),
            onLikeClick = { post, position ->
                handleLikeClick(post, position)
            },
            onDislikeClick = { post, position ->
                handleDislikeClick(post, position)
            },
            onCommentClick = { post, position ->
                handleCommentClick(post, position)
            },
            onFavoriteClick = { post, position ->
                handleFavoriteClick(post, position)
            },
            onDeletePost = { post, position ->
                handleDeletePost(post, position)
            }
        )
        
        rvFavorites.apply {
            layoutManager = LinearLayoutManager(this@FavoritesActivity)
            adapter = postsAdapter
        }
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query ?: ""
                loadFavorites()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText ?: ""
                if (currentQuery.isEmpty() || currentQuery.length >= 2) {
                    loadFavorites()
                }
                return true
            }
        })
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        swipeRefresh.setOnRefreshListener {
            loadFavorites()
        }
    }
    
    private fun loadFavorites() {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        rvFavorites.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        
        Log.d("FavoritesActivity", "Loading favorites: userId=$userId, query='$currentQuery', orderBy=$currentOrderBy")
        
        lifecycleScope.launch {
            try {
                val response = repository.getFavorites(userId, currentQuery, currentOrderBy, 50, 0)
                
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
                
                if (response != null && response.success) {
                    if (response.posts.isNotEmpty()) {
                        Log.d("FavoritesActivity", "Showing ${response.posts.size} favorites")
                        postsAdapter.updatePosts(response.posts)
                        rvFavorites.visibility = View.VISIBLE
                        tvEmptyState.visibility = View.GONE
                    } else {
                        Log.d("FavoritesActivity", "No favorites found")
                        rvFavorites.visibility = View.GONE
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = if (currentQuery.isNotEmpty()) {
                            "No se encontraron favoritos con '$currentQuery'"
                        } else {
                            "No tienes publicaciones favoritas aún"
                        }
                    }
                } else {
                    Toast.makeText(this@FavoritesActivity, response?.message ?: "Error al cargar favoritos", Toast.LENGTH_SHORT).show()
                    rvFavorites.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Error al cargar favoritos"
                }
            } catch (e: Exception) {
                Log.e("FavoritesActivity", "Exception loading favorites", e)
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
                Toast.makeText(this@FavoritesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                rvFavorites.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Error al cargar favoritos"
            }
        }
    }
    
    private fun handleLikeClick(post: Post, position: Int) {
        val postId = post.postId?.toIntOrNull() ?: return
        
        lifecycleScope.launch {
            try {
                val response = repository.votePost(postId, userId, 1)
                if (response != null && response.success && response.post != null) {
                    val p = response.post
                    postsAdapter.updatePostVote(position, p.user_vote, p.likes_count, p.dislikes_count)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleDislikeClick(post: Post, position: Int) {
        val postId = post.postId?.toIntOrNull() ?: return
        
        lifecycleScope.launch {
            try {
                val response = repository.votePost(postId, userId, -1)
                if (response != null && response.success && response.post != null) {
                    val p = response.post
                    postsAdapter.updatePostVote(position, p.user_vote, p.likes_count, p.dislikes_count)
                }
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleCommentClick(post: Post, position: Int) {
        val intent = Intent(this, CommentsActivity::class.java)
        intent.putExtra("post_id", post.postId)
        intent.putExtra("post_title", post.title)
        startActivity(intent)
    }
    
    private fun handleFavoriteClick(post: Post, position: Int) {
        val postId = post.postId?.toIntOrNull() ?: return
        
        lifecycleScope.launch {
            try {
                val response = repository.toggleFavorite(postId, userId)
                if (response != null && response.success) {
                    // Si se eliminó de favoritos, recargar la lista
                    if (!response.isFavorite) {
                        loadFavorites()
                        Toast.makeText(this@FavoritesActivity, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                    } else {
                        postsAdapter.updatePostFavorite(position, response.isFavorite)
                        Toast.makeText(this@FavoritesActivity, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@FavoritesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadFavorites() // Recargar al volver de comentarios
    }
    
    private fun handleDeletePost(post: Post, position: Int) {
        val postId = post.postId?.toIntOrNull()
        if (postId == null) {
            Toast.makeText(this, "Error al procesar la publicación", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = repository.deletePost(postId, userId)
                
                if (response != null && response.success) {
                    Toast.makeText(this@FavoritesActivity, "Publicación eliminada", Toast.LENGTH_SHORT).show()
                    postsAdapter.removePost(position)
                } else {
                    Toast.makeText(
                        this@FavoritesActivity, 
                        response?.message ?: "Error al eliminar", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("FavoritesActivity", "Error deleting post", e)
                Toast.makeText(this@FavoritesActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
