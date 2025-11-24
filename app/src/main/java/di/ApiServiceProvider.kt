package di
import Model.dao.UpdateApi

object ApiServiceProvider {
    val updateService: UpdateApi by lazy {
        AppModule.retrofitInstance.create(UpdateApi::class.java)
    }
}