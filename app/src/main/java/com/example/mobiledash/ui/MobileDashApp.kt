package com.example.mobiledash.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mobiledash.data.DashboardApi
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.LoginSession
import com.example.mobiledash.data.SessionStore
import kotlinx.coroutines.launch

@Composable
fun MobileDashApp(context: Context) {
    val repository = remember {
        DashboardRepository(
            api = DashboardApi(),
            sessionStore = SessionStore(context.applicationContext),
        )
    }
    val scope = rememberCoroutineScope()
    var restoring by remember { mutableStateOf(true) }
    var session by remember { mutableStateOf<LoginSession?>(null) }

    LaunchedEffect(Unit) {
        session = repository.restoreSession()
        restoring = false
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
        }
    }
}
