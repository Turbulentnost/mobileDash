package com.example.mobiledash.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.DashboardApi
import com.example.mobiledash.data.DashboardCacheStore
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.LoginSession
import com.example.mobiledash.data.SessionStore
import com.example.mobiledash.data.AppUpdateInfo
import com.example.mobiledash.data.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun MobileDashApp(context: Context) {
    val appContext = context.applicationContext
    val repository = remember {
        DashboardRepository(
            api = DashboardApi(),
            sessionStore = SessionStore(appContext),
            dashboardCacheStore = DashboardCacheStore(appContext),
        )
    }
    val updateManager = remember { UpdateManager(appContext) }
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(true) }
    var session by remember { mutableStateOf<LoginSession?>(null) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateStatus by remember { mutableStateOf("") }
    var downloadingUpdate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        session = repository.restoreSession()
        restoring = false
    }

    LaunchedEffect(Unit) {
        availableUpdate = updateManager.checkForUpdate()
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                restoring -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                session == null -> LoginScreen(
                    repository = repository,
                    onLoggedIn = { session = it },
                )
                else -> DashboardScreen(
                    repository = repository,
                    session = requireNotNull(session),
                    onSessionExpired = {
                        scope.launch {
                            repository.logout()
                            session = null
                        }
                    },
                    onLogout = {
                        scope.launch {
                            repository.logout()
                            session = null
                        }
                    },
                )
            }
            availableUpdate?.let { update ->
                UpdateBanner(
                    update = update,
                    status = updateStatus,
                    downloading = downloadingUpdate,
                    onUpdateClick = {
                        scope.launch {
                            downloadingUpdate = true
                            updateStatus = "Скачиваем обновление..."
                            val started = updateManager.downloadAndInstall(update)
                            updateStatus = if (started) {
                                "Откройте установщик APK"
                            } else {
                                "Не удалось скачать обновление"
                            }
                            downloadingUpdate = false
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun UpdateBanner(
    update: AppUpdateInfo,
    status: String,
    downloading: Boolean,
    onUpdateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DashboardDesign.Navy,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Вышло новое обновление!", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    status.ifBlank { "Версия ${update.versionName}" },
                    color = Color.White.copy(alpha = 0.76f),
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                )
            }
            Surface(
                modifier = Modifier.clickable(enabled = !downloading, onClick = onUpdateClick),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (downloading) "..." else "Обновить",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = DashboardDesign.Navy,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
