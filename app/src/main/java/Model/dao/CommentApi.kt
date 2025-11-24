package Model.dao

import Model.data.*
import retrofit2.Response
import retrofit2.http.*

interface CommentApi {
    @GET("get_comments.php")
    suspend fun getComments(
        @Query("post_id") postId: Int,
        @Query("user_id") userId: Int
    ): Response<CommentsResponse>
    
    @FormUrlEncoded
    @POST("create_comment.php")
    suspend fun createComment(
        @Field("post_id") postId: Int,
        @Field("user_id") userId: Int,
        @Field("comment_text") commentText: String
    ): Response<CommentResponse>
    
    @FormUrlEncoded
    @POST("like_comment.php")
    suspend fun likeComment(
        @Field("comment_id") commentId: Int,
        @Field("user_id") userId: Int
    ): Response<CommentLikeResponse>
    
    @GET("get_replies.php")
    suspend fun getReplies(
        @Query("comment_id") commentId: Int
    ): Response<RepliesResponse>
    
    @FormUrlEncoded
    @POST("create_reply.php")
    suspend fun createReply(
        @Field("comment_id") commentId: Int,
        @Field("user_id") userId: Int,
        @Field("reply_text") replyText: String
    ): Response<ReplyResponse>
}
