package com.booknext.app.ui.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onBack: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onFileSelected(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("上传书籍") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Icon(Icons.Default.AttachFile, null)
                Spacer(Modifier.width(8.dp))
                Text(state.fileName.ifEmpty { "选择文件（EPUB/PDF/TXT/MOBI）" })
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("书名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.author,
                onValueChange = viewModel::onAuthorChange,
                label = { Text("作者") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            if (state.uploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("上传中…", style = MaterialTheme.typography.bodySmall)
            }

            if (state.error.isNotEmpty()) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }
            if (state.success) {
                Text("✓ 上传成功", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.upload(context, onBack) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = state.fileUri != null && !state.uploading,
            ) {
                Text("上传")
            }
        }
    }
}