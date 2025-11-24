package di

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object AppModule {
    // 1. Define la URL base de tu servidor (donde está tu API)
    private const val BASE_URL = "http://10.0.2.2:8080/PSM/BD/"

    // 2. Crear el cliente HTTP con logging
    private val httpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 3. Provee el Singleton de Retrofit
    // Esta instancia se crea una sola vez cuando se accede por primera vez.
    val tolerantGson = GsonBuilder()
        .setLenient()
        .create()
        
    val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            // PASAMOS EL OBJETO GSON CREADO
            .addConverterFactory(GsonConverterFactory.create(tolerantGson))
            .build()
    }

}