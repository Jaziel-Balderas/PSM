package com.example.psm.UI.Fragments

import Model.data.Post
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.psm.R
import com.example.psm.UI.adapter.PostsAdapter
import kotlinx.coroutines.launch
import android.util.Log


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class dashlayout : Fragment() {
    
    private lateinit var rvPosts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var swipeRefresh: SwipeRefreshLayout
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

        btnSett?.setOnClickListener{
            val intent = Intent(requireContext(), fragment_settings::class.java)
            startActivity(intent)
        }
        
        setupSwipeRefresh()
        setupRecyclerView()
        loadPosts()
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
                val response = repository.likePost(postId, userId)
                
                if (response != null && response.success) {
                    // Actualizar UI
                    post.isLiked = response.liked
                    postsAdapter.updatePostLike(position, response.liked, response.likeCount)
                    postsAdapter.notifyItemChanged(position)
                } else {
                    Toast.makeText(
                        requireContext(),
                        response?.message ?: "Error al dar like",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error de conexión: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                val response = repository.getPosts(currentUserId = currentUserId)
                
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                
                Log.d("DashLayout", "Response: success=${response?.success}, posts count=${response?.posts?.size}")
                
                if (response != null && response.success) {
                    if (response.posts.isNotEmpty()) {
                        Log.d("DashLayout", "Showing ${response.posts.size} posts")
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
}
