package com.example.psm.UI.controller
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import Model.data.User
import Model.repository.UserRepository

class ProfileVIewModel(private val userRepository: UserRepository) : ViewModel() {
    val currentUserProfile: LiveData<User?> = userRepository.getCurrentUserProfile()


}