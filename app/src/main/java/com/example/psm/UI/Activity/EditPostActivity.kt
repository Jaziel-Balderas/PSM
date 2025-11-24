package com.example.psm.UI.Activity

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.psm.R
import com.example.psm.UI.controller.PostViewModel
import com.example.psm.UI.controller.PostViewModelFactory
import Model.repository.SessionManager
import Model.repository.PostRepository

class EditPostActivity : AppCompatActivity() {

    private lateinit var postRepository: PostRepository
    private lateinit var postViewModel: PostViewModel
    
    private lateinit var etTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var etLocation: EditText
    private lateinit var switchPublic: Switch
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvImageWarning: TextView
    
    private var postId: String = ""
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)
        
        // Obtener datos del intent
        postId = intent.getStringExtra("post_id") ?: ""
        userId = intent.getStringExtra("post_user_id") ?: ""
        val title = intent.getStringExtra("post_title") ?: ""
        val content = intent.getStringExtra("post_content") ?: ""
        val location = intent.getStringExtra("post_location") ?: ""
        val isPublic = intent.getBooleanExtra("post_is_public", true)
        
        if (postId.isEmpty() || userId.isEmpty()) {
            Toast.makeText(this, "Error: Datos inválidos", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initializeViews()
        setupViewModel()
        setupObservers()
        loadPostData(title, content, location, isPublic)
        setupListeners()
    }
    
    private fun initializeViews() {
        etTitle = findViewById(R.id.etEditTitle)
        etContent = findViewById(R.id.etEditContent)
        etLocation = findViewById(R.id.etEditLocation)
        switchPublic = findViewById(R.id.switchEditPublic)
        btnSave = findViewById(R.id.btnSaveEdit)
        btnCancel = findViewById(R.id.btnCancelEdit)
        progressBar = findViewById(R.id.progressBarEdit)
        tvImageWarning = findViewById(R.id.tvImageWarning)
    }
    
    private fun setupViewModel() {
        postRepository = PostRepository()
        val factory = PostViewModelFactory(postRepository)
        postViewModel = ViewModelProvider(this, factory).get(PostViewModel::class.java)
    }
    
    private fun setupObservers() {
        postViewModel.postStatus.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Publicación actualizada exitosamente", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }
        })
        
        postViewModel.errorMessage.observe(this, Observer { message ->
            if (message.isNotEmpty() && postViewModel.postStatus.value == false) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        })
        
        postViewModel.isLoading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSave.isEnabled = !isLoading
            btnCancel.isEnabled = !isLoading
        })
    }
    
    private fun loadPostData(title: String, content: String, location: String, isPublic: Boolean) {
        etTitle.setText(title)
        etContent.setText(content)
        etLocation.setText(location)
        switchPublic.isChecked = isPublic
    }
    
    private fun setupListeners() {
        btnSave.setOnClickListener {
            saveChanges()
        }
        
        btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun saveChanges() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val isPublic = switchPublic.isChecked
        
        if (content.isEmpty()) {
            Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convertir a Int para el ViewModel
        val postIdInt = postId.toIntOrNull() ?: 0
        val userIdInt = userId.toIntOrNull() ?: 0
        
        if (postIdInt <= 0 || userIdInt <= 0) {
            Toast.makeText(this, "Error: IDs inválidos", Toast.LENGTH_SHORT).show()
            return
        }
        
        postViewModel.updatePost(postIdInt, userIdInt, title, content, location, isPublic)
    }
}
