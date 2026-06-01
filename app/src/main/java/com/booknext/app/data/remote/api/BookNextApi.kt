package com.booknext.app.data.remote.api

import com.booknext.app.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface BookNextApi {

    @POST("api/auth")
    suspend fun auth(@Body req: AuthRequest): AuthResponse

    @GET("api/health")
    suspend fun health(): HealthResponse

    @GET("api/books")
    suspend fun listBooks(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
        @Query("query") query: String? = null,
        @Query("fmt") fmt: String? = null,
    ): BookListResponse

    @GET("api/books/{id}")
    suspend fun getBook(@Path("id") id: String): BookDto

    @Multipart
    @PATCH("api/books/{id}")
    suspend fun updateBook(
        @Path("id") id: String,
        @Part("progress") progress: RequestBody? = null,
        @Part("category") category: RequestBody? = null,
    ): UpdateResponse

    @Streaming
    @GET("api/stream/{id}")
    suspend fun streamBook(@Path("id") id: String): ResponseBody

    @Multipart
    @POST("api/upload")
    suspend fun uploadBook(
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("author") author: RequestBody,
        @Part("run_ocr") runOcr: RequestBody,
    ): UploadResponse

    @DELETE("api/books/{id}")
    suspend fun deleteBook(@Path("id") id: String): Response<Unit>

    @Streaming
    @GET("api/convert/{id}")
    suspend fun convertBook(
        @Path("id") id: String,
        @Query("target_fmt") targetFmt: String = "epub",
    ): ResponseBody

    @Streaming
    @POST("api/tts")
    suspend fun tts(@Body req: TtsRequest): ResponseBody
}
