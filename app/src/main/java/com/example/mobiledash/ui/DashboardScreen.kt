package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.ApiResult
import com.example.mobiledash.data.ChairmanCatalogItem
import com.example.mobiledash.data.DashboardPayload
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.HierarchyNode
import com.example.mobiledash.data.LoginSession
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private enum class DashboardSection(val label: String) {
    Tiles("Плитки"),
    Charts("Графики"),
    Tables("Таблицы"),
    Info("Инфо"),
    Admin("Админ"),
    Raw("JSON"),
}

@Composable
fun DashboardScreen(
    repository: DashboardRepository,
    session: LoginSession,
    onSessionExpired: () -> Unit,
    onLogout: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val now = remember { LocalDate.now() }
    var selectedSection by remember { mutableStateOf(DashboardSection.Tiles) }
    var selectedDepartment by remember { mutableStateOf(session.user.department) }
    var month by remember { mutableIntStateOf(now.monthValue) }
    var year by remember { mutableIntStateOf(now.year) }
    var aggregation by remember { mutableStateOf("current") }
    var chairmanFor by remember { mutableStateOf<String?>(null) }
    var payload by remember { mutableStateOf<DashboardPayload?>(null) }
    var commercialPayload by remember { mutableStateOf<DashboardPayload?>(null) }
    var hierarchy by remember { mutableStateOf<List<HierarchyNode>>(emptyList()) }
    var chairmanCatalog by remember { mutableStateOf<List<ChairmanCatalogItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var commercialLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val isChairmanUser = session.user.nickname.equals("founder", ignoreCase = true) ||
        session.user.department.contains("председател", ignoreCase = true)

    fun reload() {
        loading = true
        payload = null
        commercialPayload = null
        if (isChairmanUser && chairmanFor != "commerce") commercialLoading = true
        error = ""
        scope.launch {
            when (val result = repository.fetchDashboard(session, selectedDepartment, month, year, aggregation, chairmanFor)) {
                is ApiResult.Success -> payload = result.value
                is ApiResult.Failure -> {
                    error = result.message
                    if (result.unauthorized) onSessionExpired()
                }
            }
            if (isChairmanUser && chairmanFor != "commerce") {
                when (
                    val result = repository.fetchDashboard(
                        session = session,
                        department = session.user.department.ifBlank { selectedDepartment },
                        month = month,
                        year = year,
                        aggregation = aggregation,
                        chairmanFor = "commerce",
                    )
                ) {
                    is ApiResult.Success -> commercialPayload = result.value
                    is ApiResult.Failure -> {
                        commercialPayload = null
                        if (result.unauthorized) onSessionExpired()
                    }
                }
                commercialLoading = false
            }
            when (val result = repository.fetchImmediateSubordinates(session, selectedDepartment)) {
                is ApiResult.Success -> hierarchy = result.value
                is ApiResult.Failure -> if (result.unauthorized) onSessionExpired()
            }
            when (val result = repository.fetchChairmanCatalog(session)) {
                is ApiResult.Success -> chairmanCatalog = result.value
                is ApiResult.Failure -> Unit
            }
            loading = false
        }
    }

    fun currentPeriod(): YearMonth = YearMonth.of(year, month)

    fun goPreviousMonth() {
        val previous = currentPeriod().minusMonths(1)
        month = previous.monthValue
        year = previous.year
    }

    fun goNextMonth() {
        val next = currentPeriod().plusMonths(1)
        val current = YearMonth.from(now)
        if (!next.isAfter(current)) {
            month = next.monthValue
            year = next.year
        }
    }

    LaunchedEffect(selectedDepartment, month, year, aggregation, chairmanFor) {
        reload()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                HierarchyBlock(
                    currentDepartment = selectedDepartment,
                    homeDepartment = session.user.department,
                    children = hierarchy,
                    onDepartmentSelected = {
                        selectedDepartment = it
                        chairmanFor = null
                        scope.launch { drawerState.close() }
                    },
                    onBackHome = {
                        selectedDepartment = session.user.department
                        chairmanFor = null
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DashboardDesign.Screen),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DashboardTopBar(
                    session = session,
                    selectedDepartment = selectedDepartment,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onLogout = onLogout,
                )
                var dragAmount = 0f
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                        .pointerInput(month, year) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragAmount = 0f },
                                onHorizontalDrag = { _, dragDelta -> dragAmount += dragDelta },
                                onDragEnd = {
                                    when {
                                        dragAmount < -90f -> goNextMonth()
                                        dragAmount > 90f -> goPreviousMonth()
                                    }
                                },
                            )
                        },
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item { SectionSpacer(height = 2) }
                    item {
                        PeriodControls(
                            month = month,
                            year = year,
                            aggregation = aggregation,
                            canGoNext = currentPeriod().isBefore(YearMonth.from(now)),
                            onPrevious = { goPreviousMonth() },
                            onNext = { goNextMonth() },
                            onAggregationChange = { aggregation = it },
                        )
                    }
                    if (error.isNotBlank()) {
                        item { Text(error, color = MaterialTheme.colorScheme.error) }
                    }
                    val currentPayload = payload
                    if (loading && currentPayload == null) {
                        item { DashboardLoadingState() }
                    } else if (currentPayload != null) {
                        if (selectedSection == DashboardSection.Tiles && isChairmanUser && chairmanFor != "commerce") {
                            item {
                                CommercialSummaryBlock(
                                    tiles = commercialPayload?.tiles.orEmpty(),
                                    loading = commercialLoading,
                                    onOpenCommercial = {
                                        selectedSection = DashboardSection.Tiles
                                        selectedDepartment = session.user.department.ifBlank { selectedDepartment }
                                        chairmanFor = "commerce"
                                    },
                                )
                            }
                        }
                        when (selectedSection) {
                            DashboardSection.Tiles -> item { KpiTilesBlock(currentPayload.tiles) }
                            DashboardSection.Charts -> item { ChartsBlock(currentPayload.charts) }
                            DashboardSection.Tables -> item { TablesBlock(currentPayload.tables) }
                            DashboardSection.Info -> item { InfoBlock() }
                            DashboardSection.Admin -> item {
                                AdminBlock(repository = repository, session = session, onSessionExpired = onSessionExpired)
                            }
                            DashboardSection.Raw -> item { RawSummaryBlock(currentPayload.rawSummary) }
                        }
                    }
                    item { SectionSpacer(height = 78) }
                }
            }
            DashboardBottomBar(
                selectedSection = selectedSection,
                isAdmin = session.user.isAdmin,
                onSelect = { selectedSection = it },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun DashboardLoadingState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DashboardDesign.Card,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 42.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(color = DashboardDesign.Accent)
            Text(
                "Загрузка данных...",
                color = DashboardDesign.MutedText,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DashboardBottomBar(
    selectedSection: DashboardSection,
    isAdmin: Boolean,
    onSelect: (DashboardSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = DashboardSection.entries.filter { it != DashboardSection.Admin || isAdmin }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp),
        color = Color.White,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            sections.filter { it != DashboardSection.Raw }.forEach { section ->
                TextButton(onClick = { onSelect(section) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TempImage(
                            asset = section.navIcon,
                            contentDescription = section.label,
                            modifier = Modifier
                                .size(22.dp)
                                .padding(bottom = 2.dp),
                            contentScale = ContentScale.Fit,
                        )
                        Text(
                            section.label,
                            color = if (section == selectedSection) DashboardDesign.Accent else DashboardDesign.MutedText,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private val DashboardSection.navIcon: TempImageAsset
    get() = when (this) {
        DashboardSection.Tiles -> TempImageAsset.Tiles
        DashboardSection.Charts -> TempImageAsset.Charts
        DashboardSection.Tables -> TempImageAsset.Tables
        DashboardSection.Info -> TempImageAsset.Info
        DashboardSection.Admin -> TempImageAsset.Info
        DashboardSection.Raw -> TempImageAsset.Info
    }

@Composable
private fun SectionSpacer(height: Int) {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(height.dp))
}
