package com.example.psm.UI.controller

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
}
