package Model.dao

import Model.data.PostResponse
import Model.data.PostsResponse
import Model.data.VoteResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PostApi {
    // Versión original (usando PHP directo)
    @Multipart
    @POST("create_post.php")
    suspend fun createPost(
        @Part("user_id") userId: RequestBody,
        @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody,
        @Part("location") location: RequestBody?,
        @Part("is_public") isPublic: RequestBody,
        @Part images: List<MultipartBody.Part>?
    ): Response<PostResponse>
    
    // Versión con Stored Procedure (más eficiente)
    @Multipart
    @POST("create_post_sp.php")
    suspend fun createPostSP(
        @Part("user_id") userId: RequestBody,
        @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody,
        @Part("location") location: RequestBody?,
        @Part("is_public") isPublic: RequestBody,
        @Part images: List<MultipartBody.Part>?
    ): Response<PostResponse>
    
    @GET("get_posts_v2.php")
    suspend fun getPosts(
        @Query("userId") userId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PostsResponse>
    
    @FormUrlEncoded
    @POST("vote_post.php")
    suspend fun votePost(
        @Field("postId") postId: Int,
        @Field("userId") userId: Int,
        @Field("vote") vote: Int
    ): Response<VoteResponse>
    
    @GET("search_posts.php")
    suspend fun searchPosts(
        @Query("userId") userId: Int,
        @Query("query") query: String,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PostsResponse>
}
