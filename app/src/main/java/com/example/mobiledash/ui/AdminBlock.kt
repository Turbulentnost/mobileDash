package com.example.mobiledash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.AccessRequestItem
import com.example.mobiledash.data.ApiResult
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.LoginSession
import kotlinx.coroutines.launch

@Composable
fun AdminBlock(
    repository: DashboardRepository,
    session: LoginSession,
    onSessionExpired: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<AccessRequestItem>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    fun reload() {
        scope.launch {
            when (val result = repository.fetchAccessRequests(session)) {
                is ApiResult.Success -> requests = result.value
                is ApiResult.Failure -> {
                    message = result.message
                    if (result.unauthorized) onSessionExpired()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Заявки доступа", style = MaterialTheme.typography.titleLarge)
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        if (requests.isEmpty()) Text("Нет ожидающих заявок")
        requests.forEach { request ->
            AccessRequestCard(
                request = request,
                onApprove = {
                    scope.launch {
                        message = when (val result = repository.approveAccessRequest(session, request.id)) {
                            is ApiResult.Success -> "Заявка ${request.id} одобрена"
                            is ApiResult.Failure -> result.message
                        }
                        reload()
                    }
                },
                onReject = { comment ->
                    scope.launch {
                        message = when (val result = repository.rejectAccessRequest(session, request.id, comment)) {
                            is ApiResult.Success -> "Заявка ${request.id} отклонена"
                            is ApiResult.Failure -> result.message
                        }
                        reload()
                    }
                },
            )
        }
    }
}

@Composable
private fun AccessRequestCard(
    request: AccessRequestItem,
    onApprove: () -> Unit,
    onReject: (String) -> Unit,
) {
    var comment by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${request.nickname} · ${request.typeLabel}", style = MaterialTheme.typography.titleMedium)
            Text(request.department)
            Text("${request.statusLabel} · ${request.createdAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Комментарий для отказа") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("Одобрить") }
                OutlinedButton(onClick = { onReject(comment) }) { Text("Отклонить") }
            }
        }
    }
}
