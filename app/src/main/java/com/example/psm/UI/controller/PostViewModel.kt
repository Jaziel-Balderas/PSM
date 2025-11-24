package com.example.psm.UI.controller

import Model.data.Post
import Model.repository.PostRepository
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.File

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    private val _postStatus = MutableLiveData<Boolean>()
    val postStatus: LiveData<Boolean> = _postStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    fun createPost(
        userId: String,
        title: String,
        description: String,
        location: String,
        isPublic: Boolean,
        imageFiles: List<File>
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            val response = repository.createPost(
                userId,
                title,
                description,
                location,
                isPublic,
                imageFiles
            )

            _isLoading.value = false

            if (response != null && response.success) {
                _postStatus.value = true
                _errorMessage.value = ""
            } else {
                _postStatus.value = false
                _errorMessage.value = response?.message ?: "Error desconocido"
            }
        }
    }

    fun resetStatus() {
        _postStatus.value = false
        _errorMessage.value = ""
    }
    
    fun updatePost(
        postId: Int,
        userId: Int,
        title: String,
        content: String,
        location: String,
        isPublic: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val response = repository.updatePost(
                postId,
                userId,
                title,
                content,
                location,
                isPublic
            )
            
            _isLoading.value = false
            
            if (response != null && response.success) {
                _postStatus.value = true
                _errorMessage.value = "Publicación actualizada exitosamente"
            } else {
                _postStatus.value = false
                _errorMessage.value = response?.message ?: "Error al actualizar publicación"
            }
        }
    }
    
    fun deletePost(postId: Int, userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val response = repository.deletePost(postId, userId)
            
            _isLoading.value = false
            
            if (response != null && response.success) {
                _postStatus.value = true
                _errorMessage.value = "Publicación eliminada exitosamente"
            } else {
                _postStatus.value = false
                _errorMessage.value = response?.message ?: "Error al eliminar publicación"
            }
        }
    }
    
    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val response = repository.getUserPosts(userId)
            
            _isLoading.value = false
            
            if (response.success && response.posts.isNotEmpty()) {
                _userPosts.value = response.posts
                _errorMessage.value = ""
            } else {
                _userPosts.value = emptyList()
                _errorMessage.value = response.message
            }
        }
    }
}
