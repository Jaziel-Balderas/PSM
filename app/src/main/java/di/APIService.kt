package di
import Model.dao.AuthApi
import Model.dao.UpdateApi


object APIService {

    val authService: AuthApi by lazy {
        AppModule.retrofitInstance.create(AuthApi::class.java)
    }

    val updateService: UpdateApi by lazy {
        AppModule.retrofitInstance.create(UpdateApi::class.java)
    }
}