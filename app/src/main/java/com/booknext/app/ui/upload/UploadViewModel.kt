package com.booknext.app.ui.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.booknext.app.data.remote.ApiClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class UploadState(
    val fileUri: Uri? = null,
    val fileName: String = "",
    val title: String = "",
    val author: String = "",
    val uploading: Boolean = false,
    val error: String = "",
    val success: Boolean = false,
)

@HiltViewModel
class UploadViewModel @Inject constructor(
    private val apiClient: ApiClient,
) : ViewModel() {

    private val _state = MutableStateFlow(UploadState())
    val state: StateFlow<UploadState> = _state

    fun onFileSelected(context: Context, uri: Uri) {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(idx)
        } ?: "未知文件"

        val titleGuess = name.substringBeforeLast('.')

        _state.value = _state.value.copy(
            fileUri = uri,
            fileName = name,
            title = titleGuess,
        )
    }

    fun onTitleChange(v: String) { _state.value = _state.value.copy(title = v) }
    fun onAuthorChange(v: String) { _state.value = _state.value.copy(author = v) }

    fun upload(context: Context, onSuccess: () -> Unit) {
        val uri = _state.value.fileUri ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(uploading = true, error = "", success = false)
            val ext = _state.value.fileName.substringAfterLast('.', "bin")
            val tmpFile = File(context.cacheDir, "${java.util.UUID.randomUUID()}.$ext")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmpFile.outputStream().use { output -> input.copyTo(output) }
                }

                val filePart = MultipartBody.Part.createFormData(
                    "file",
                    tmpFile.name,
                    tmpFile.asRequestBody("application/octet-stream".toMediaType()),
                )
                val titleBody = _state.value.title.toRequestBody("text/plain".toMediaType())
                val authorBody = (_state.value.author.ifEmpty { "未知" }).toRequestBody("text/plain".toMediaType())
                val ocrBody = "false".toRequestBody("text/plain".toMediaType())

                apiClient.api().uploadBook(filePart, titleBody, authorBody, ocrBody)
                _state.value = _state.value.copy(uploading = false, success = true)
                delay(1500)
                onSuccess()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    uploading = false,
                    error = "上传失败：${e.message}",
                )
            } finally {
                tmpFile.delete()
            }
        }
    }
}