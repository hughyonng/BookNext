package com.booknext.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录服务器") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp).imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text("BookNext", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("你的云端书库，随存随读", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onUrlChange,
                label = { Text("服务器地址") },
                placeholder = { Text("https://xxx.hf.space") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (state.serverUrl.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onUrlChange("") }) {
                            Icon(Icons.Default.Close, "清除地址")
                        }
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = viewModel::onKeyChange,
                label = { Text("访问密钥") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(24.dp))

            if (state.error.isNotEmpty()) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.login(onLoginSuccess) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !state.loading,
            ) {
                if (state.loading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("登录")
            }
        }
    }
}
