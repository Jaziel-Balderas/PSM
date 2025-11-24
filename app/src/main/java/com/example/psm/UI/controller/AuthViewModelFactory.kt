package com.example.psm.UI.controller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import Model.repository.UserRepository

class AuthViewModelFactory(private val repository: UserRepository):ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // El Factory sabe cómo crear cada tipo de Controller (ViewModel)

        // 1. Caso para el Controller de Autenticación
        if (modelClass.isAssignableFrom(AuthVIewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthVIewModel(repository) as T
        }

        // 2. Caso para el Controller del Perfil
        if (modelClass.isAssignableFrom(ProfileVIewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileVIewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}