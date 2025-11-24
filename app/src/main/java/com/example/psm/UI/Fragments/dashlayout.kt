package com.example.psm.UI.Fragments

import Model.data.Post
import Model.data.VoteResponse
import Model.repository.PostRepository
import Model.repository.SessionManager
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.psm.R
import com.example.psm.UI.adapter.PostsAdapter
import kotlinx.coroutines.launch
import android.util.Log
import com.example.psm.UI.Activity.RegisterActivity

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
class dashlayout : Fragment() {
    
    private lateinit var rvPosts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private lateinit var postsAdapter: PostsAdapter
    private val repository = PostRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dashlayout, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnSett = view.findViewById<ImageButton>(R.id.btnSettings)
        rvPosts = view.findViewById(R.id.rvPosts)
        progressBar = view.findViewById(R.id.progressBar)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        searchView = view.findViewById(R.id.searchView)

        btnSett?.setOnClickListener{
            val intent = Intent(requireContext(), fragment_settings::class.java)
            startActivity(intent)
        }
        
        setupSwipeRefresh()
        setupRecyclerView()
        setupSearchView()
        loadPosts()
    }
    
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { 
                    if (it.isNotEmpty()) {
                        performSearch(it)
                    } else {
                        loadPosts()
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    loadPosts()
                } else if (newText.length >= 2) {
                    performSearch(newText)
                }
                return true
            }
        })
    }
    
    private fun performSearch(query: String) {
        val userIdString = SessionManager.getUserId()
        if (userIdString == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error con el ID de usuario", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        rvPosts.visibility = View.GONE
        tvEmptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                Log.d("dashlayout", "Searching posts with query: $query")
                val response = repository.searchPosts(userId, query, 50, 0)
                
                if (response != null && response.posts.isNotEmpty()) {
                    postsAdapter.updatePosts(response.posts)
                    rvPosts.visibility = View.VISIBLE
                    tvEmptyState.visibility = View.GONE
                    Log.d("dashlayout", "Search found ${response.posts.size} posts")
                } else {
                    rvPosts.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "No se encontraron publicaciones"
                    Log.d("dashlayout", "No posts found for query: $query")
                }
            } catch (e: Exception) {
                Log.e("dashlayout", "Error searching posts", e)
                Toast.makeText(requireContext(), "Error al buscar: ${e.message}", Toast.LENGTH_SHORT).show()
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Error al buscar publicaciones"
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            loadPosts()
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
        rvPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postsAdapter
        }
    }
    
    private fun handleLikeClick(post: Post, position: Int) {
        val userIdString = SessionManager.getUserId()
        if (userIdString == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: ID de usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postId = post.postId?.toIntOrNull()
        if (postId == null) {
            Toast.makeText(requireContext(), "Error al procesar la publicación", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Siempre enviamos vote = 1 para acción de like (SP quita si ya existe)
                val response: VoteResponse? = repository.votePost(postId, userId, 1)
                if (response != null && response.success) {
                    val p = response.post
                    if (p != null) {
                        postsAdapter.updatePostVote(position, p.user_vote, p.likes_count, p.dislikes_count)
                    }
                } else {
                    Toast.makeText(requireContext(), response?.message ?: "Error al votar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleDislikeClick(post: Post, position: Int) {
        val userIdString = SessionManager.getUserId()
        if (userIdString == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: ID de usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postId = post.postId?.toIntOrNull()
        if (postId == null) {
            Toast.makeText(requireContext(), "Error al procesar la publicación", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Enviamos vote = -1 para acción de dislike (SP quita si ya existe)
                val response: VoteResponse? = repository.votePost(postId, userId, -1)
                if (response != null && response.success) {
                    val p = response.post
                    if (p != null) {
                        postsAdapter.updatePostVote(position, p.user_vote, p.likes_count, p.dislikes_count)
                    }
                } else {
                    Toast.makeText(requireContext(), response?.message ?: "Error al votar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun handleCommentClick(post: Post, position: Int) {
        val intent = Intent(requireContext(), com.example.psm.UI.Activity.CommentsActivity::class.java)
        intent.putExtra("post_id", post.postId)
        intent.putExtra("post_title", post.title)
        startActivity(intent)
    }
    
    private fun handleFavoriteClick(post: Post, position: Int) {
        val userIdString = SessionManager.getUserId()
        if (userIdString == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: ID de usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postId = post.postId?.toIntOrNull()
        if (postId == null) {
            Toast.makeText(requireContext(), "Error al procesar la publicación", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = repository.toggleFavorite(postId, userId)
                if (response != null && response.success) {
                    postsAdapter.updatePostFavorite(position, response.isFavorite)
                    val message = if (response.isFavorite) "Agregado a favoritos" else "Eliminado de favoritos"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), response?.message ?: "Error al guardar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error de conexión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadPosts() {
        if (!swipeRefresh.isRefreshing) {
            progressBar.visibility = View.VISIBLE
        }
        tvEmptyState.visibility = View.GONE
        
        val currentUserId = SessionManager.getUserId()?.toIntOrNull() ?: 0
        Log.d("DashLayout", "Loading posts with userId: $currentUserId")
        
        lifecycleScope.launch {
            try {
                val response = repository.getPosts(userId = currentUserId)
                
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                
                Log.d("DashLayout", "Response: success=${response?.success}, posts count=${response?.posts?.size}")
                
                if (response != null && response.success) {
                    if (response.posts.isNotEmpty()) {
                        Log.d("DashLayout", "Showing ${response.posts.size} posts")
                        response.posts.forEachIndexed { index, post ->
                            Log.d("DashLayout", "Post $index: id=${post.postId}, title=${post.title}, userVote=${post.userVote}")
                        }
                        postsAdapter.updatePosts(response.posts)
                        rvPosts.visibility = View.VISIBLE
                        tvEmptyState.visibility = View.GONE
                    } else {
                        Log.d("DashLayout", "No posts found")
                        rvPosts.visibility = View.GONE
                        tvEmptyState.text = "No hay publicaciones aún"
                        tvEmptyState.visibility = View.VISIBLE
                    }
                } else {
                    Log.e("DashLayout", "Error response: ${response?.message}")
                    rvPosts.visibility = View.GONE
                    tvEmptyState.text = response?.message ?: "Error al cargar publicaciones"
                    tvEmptyState.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), response?.message ?: "Error", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("DashLayout", "Exception loading posts", e)
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                rvPosts.visibility = View.GONE
                tvEmptyState.text = "Error: ${e.message}"
                tvEmptyState.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Recargar posts cuando se vuelve al fragment
        loadPosts()
    }
    
    private fun handleDeletePost(post: Post, position: Int) {
        val userIdString = SessionManager.getUserId()
        if (userIdString == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión", Toast.LENGTH_SHORT).show()
            return
        }
        
        val userId = userIdString.toIntOrNull()
        if (userId == null) {
            Toast.makeText(requireContext(), "Error: ID de usuario inválido", Toast.LENGTH_SHORT).show()
            return
        }
        
        val postId = post.postId?.toIntOrNull()
        if (postId == null) {
            Toast.makeText(requireContext(), "Error al procesar la publicación", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                val response = repository.deletePost(postId, userId)
                
                if (response != null && response.success) {
                    Toast.makeText(requireContext(), "Publicación eliminada", Toast.LENGTH_SHORT).show()
                    postsAdapter.removePost(position)
                } else {
                    Toast.makeText(
                        requireContext(), 
                        response?.message ?: "Error al eliminar", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("dashlayout", "Error deleting post", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
