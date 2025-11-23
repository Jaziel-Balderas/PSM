package di
import Model.dao.AuthApi


object APIService {


    val authService: AuthApi by lazy {
        AppModule.retrofitInstance.create(AuthApi::class.java)
    }

    // Aquí puedes añadir más servicios después:
    // val postService: PostApi by lazy { ... }
}