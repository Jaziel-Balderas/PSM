package di

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
object AppModule {
    // 1. Define la URL base de tu servidor (donde está tu API)
    private const val BASE_URL = "http://10.0.2.2/PSM/"

    // 2. Provee el Singleton de Retrofit
    // Esta instancia se crea una sola vez cuando se accede por primera vez.
    val tolerantGson = GsonBuilder()
        .setLenient() // <--- ESTA ES LA CLAVE
        .create()
    val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // PASAMOS EL OBJETO GSON CREADO
            .addConverterFactory(GsonConverterFactory.create(tolerantGson))
            .build()
    }

}