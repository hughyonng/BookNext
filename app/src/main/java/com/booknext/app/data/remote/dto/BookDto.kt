package com.booknext.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BookListResponse(
    val books: List<BookDto>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
)

data class BookDto(
    @SerializedName("book_id")    val bookId: String,
    val title: String,
    val author: String,
    val format: String,
    val size: Long,
    @SerializedName("upload_time") val uploadTime: String,
    @SerializedName("has_cover")   val hasCover: Boolean = false,
    val status: String = "ready",
    @SerializedName("ocr_processed") val ocrProcessed: Boolean = false,
    @SerializedName("page_count") val pageCount: Int? = null,
    val category: String = "",
    val progress: String = "",
)

data class AuthRequest(val api_key: String)
data class AuthResponse(val valid: Boolean, val message: String)
data class HealthResponse(val status: String, val version: String)
data class UpdateResponse(val message: String)
data class UploadResponse(
    @SerializedName("book_id") val bookId: String,
    val title: String,
    val message: String,
    val duplicate: Boolean = false,
)
