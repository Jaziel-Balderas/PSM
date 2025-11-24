package Model.dao

import Model.data.LikeResponse
import Model.data.PostResponse
import Model.data.PostsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PostApi {
    @Multipart
    @POST("create_post.php")
    suspend fun createPost(
        @Part("user_id") userId: RequestBody,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody,
        @Part("location") location: RequestBody,
        @Part("is_public") isPublic: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<PostResponse>
    
    @GET("get_posts.php")
    suspend fun getPosts(
        @Query("current_user_id") currentUserId: Int = 0,
        @Query("user_id") userId: Int? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<PostsResponse>
    
    @FormUrlEncoded
    @POST("like_post.php")
    suspend fun likePost(
        @Field("post_id") postId: Int,
        @Field("user_id") userId: Int
    ): Response<LikeResponse>
}
