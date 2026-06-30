package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.mobiledash.data.ApiResult
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.LoginCandidate
import com.example.mobiledash.data.LoginSession
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: DashboardRepository,
    onLoggedIn: (LoginSession) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var candidates by remember { mutableStateOf<List<LoginCandidate>>(emptyList()) }
    var departments by remember { mutableStateOf<List<String>>(emptyList()) }
    var nickname by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var requestPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showRegistration by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var showLoginSuggestions by remember { mutableStateOf(false) }
    var rememberSession by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        when (val result = repository.fetchLoginCandidates()) {
            is ApiResult.Success -> candidates = result.value
            is ApiResult.Failure -> message = result.message
        }
        when (val result = repository.fetchDepartments()) {
            is ApiResult.Success -> departments = result.value
            is ApiResult.Failure -> Unit
        }
    }

    val loginSuggestions = remember(nickname, candidates) {
        val query = nickname.trim()
        if (query.length < 1) {
            emptyList()
        } else {
            candidates
                .filter {
                    it.nickname.contains(query, ignoreCase = true) ||
                        it.department.contains(query, ignoreCase = true)
                }
                .take(5)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardDesign.Screen)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LoginHeader()
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(1.dp, DashboardDesign.Border, RoundedCornerShape(24.dp)),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text("Войти в систему", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
                    Text("Выберите пользователя из подсказок или введите логин вручную", color = DashboardDesign.MutedText)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = {
                                nickname = it
                                showLoginSuggestions = it.isNotBlank()
                            },
                            label = { Text("Пользователь") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            colors = loginFieldColors(),
                        )
                        DropdownMenu(
                            expanded = showLoginSuggestions && loginSuggestions.isNotEmpty(),
                            onDismissRequest = { showLoginSuggestions = false },
                            offset = DpOffset(x = 0.dp, y = 4.dp),
                            modifier = Modifier.fillMaxWidth(0.92f),
                            properties = PopupProperties(focusable = false),
                        ) {
                            loginSuggestions.forEach { candidate ->
                                DropdownMenuItem(
                                    text = { LoginSuggestionContent(candidate) },
                                    onClick = {
                                        nickname = candidate.nickname
                                        department = candidate.department
                                        showLoginSuggestions = false
                                    },
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = loginFieldColors(),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberSession = !rememberSession },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = rememberSession,
                            onCheckedChange = { rememberSession = it },
                        )
                        Column {
                            Text("Запомнить сеанс", color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold)
                            Text(
                                "В следующий раз вход откроется без ввода пароля",
                                color = DashboardDesign.MutedText,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && nickname.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DashboardDesign.Navy,
                            contentColor = Color.White,
                            disabledContainerColor = DashboardDesign.Border,
                            disabledContentColor = DashboardDesign.MutedText,
                        ),
                        onClick = {
                            loading = true
                            message = ""
                            scope.launch {
                                when (val result = repository.login(nickname.trim(), password, rememberSession)) {
                                    is ApiResult.Success -> onLoggedIn(result.value)
                                    is ApiResult.Failure -> message = result.message
                                }
                                loading = false
                            }
                        },
                    ) {
                        if (loading) CircularProgressIndicator(color = Color.White) else Text("Войти", modifier = Modifier.padding(vertical = 6.dp))
                    }
                    if (message.isNotBlank()) {
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { showRegistration = !showRegistration }, label = { Text("Заявка на доступ") })
                AssistChip(onClick = { showReset = !showReset }, label = { Text("Сброс пароля") })
            }
        }
        if (showRegistration) {
            item {
                AccessRequestCard(
                    title = "Регистрация",
                    nickname = nickname,
                    onNicknameChange = { nickname = it },
                    password = requestPassword,
                    onPasswordChange = { requestPassword = it },
                    department = department,
                    onDepartmentChange = { department = it },
                    departments = departments,
                    actionText = "Отправить заявку",
                    onSubmit = {
                        loading = true
                        scope.launch {
                            message = when (val result = repository.submitRegistration(nickname, requestPassword, department)) {
                                is ApiResult.Success -> "Заявка отправлена администратору"
                                is ApiResult.Failure -> result.message
                            }
                            loading = false
                        }
                    },
                )
            }
        }
        if (showReset) {
            item {
                PasswordResetCard(
                    nickname = nickname,
                    onNicknameChange = { nickname = it },
                    password = requestPassword,
                    onPasswordChange = { requestPassword = it },
                    onSubmit = {
                        loading = true
                        scope.launch {
                            message = when (val result = repository.submitPasswordReset(nickname, requestPassword)) {
                                is ApiResult.Success -> "Заявка на смену пароля отправлена"
                                is ApiResult.Failure -> result.message
                            }
                            loading = false
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun LoginHeader() {
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Navy),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TempImage(
                    asset = TempImageAsset.Logo,
                    contentDescription = "Логотип MobileDash",
                    modifier = Modifier.size(58.dp),
                )
                Column {
                    Text("MobileDash", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("KPI dashboard", color = Color.White.copy(alpha = 0.78f))
                }
            }
            Text("Единый вход в дашборд KPI", color = Color.White.copy(alpha = 0.86f))
            Text("Мобильный доступ к корпоративным показателям", color = Color.White.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun LoginSuggestionContent(candidate: LoginCandidate) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .background(DashboardDesign.Accent, RoundedCornerShape(9.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp),
        ) {
            Text(candidate.nickname.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column {
            Text(candidate.nickname, color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold)
            Text(candidate.department, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = DashboardDesign.Text,
    unfocusedTextColor = DashboardDesign.Text,
    focusedLabelColor = DashboardDesign.Accent,
    unfocusedLabelColor = DashboardDesign.MutedText,
    focusedBorderColor = DashboardDesign.Accent,
    unfocusedBorderColor = DashboardDesign.Border,
    cursorColor = DashboardDesign.Accent,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

@Composable
private fun AccessRequestCard(
    title: String,
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    department: String,
    onDepartmentChange: (String) -> Unit,
    departments: List<String>,
    actionText: String,
    onSubmit: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(nickname, onNicknameChange, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                password,
                onPasswordChange,
                label = { Text("Новый пароль") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )
            OutlinedTextField(department, onDepartmentChange, label = { Text("Подразделение") }, modifier = Modifier.fillMaxWidth())
            if (departments.isNotEmpty()) {
                Text("Быстрый выбор подразделения", style = MaterialTheme.typography.labelLarge)
                departments.take(6).forEach {
                    TextButton(onClick = { onDepartmentChange(it) }) { Text(it) }
                }
            }
            Button(onClick = onSubmit, enabled = nickname.isNotBlank() && password.length >= 6 && department.isNotBlank()) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun PasswordResetCard(
    nickname: String,
    onNicknameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Сброс пароля", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(nickname, onNicknameChange, label = { Text("Логин") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                password,
                onPasswordChange,
                label = { Text("Новый пароль") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(onClick = onSubmit, enabled = nickname.isNotBlank() && password.length >= 6) {
                Text("Отправить заявку")
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
}
