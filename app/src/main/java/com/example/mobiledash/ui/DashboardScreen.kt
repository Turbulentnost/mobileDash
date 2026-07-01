package com.example.mobiledash.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.ApiResult
import com.example.mobiledash.data.ChairmanCatalogItem
import com.example.mobiledash.data.DashboardPayload
import com.example.mobiledash.data.DashboardRepository
import com.example.mobiledash.data.HierarchyNode
import com.example.mobiledash.data.KpiTile
import com.example.mobiledash.data.LoginSession
import kotlinx.coroutines.delay
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
    var headerMenuExpanded by remember { mutableStateOf(false) }
    var selectedProductionShop by remember { mutableStateOf("pc1") }
    val isChairmanUser = session.user.nickname.equals("founder", ignoreCase = true) ||
        session.user.department.contains("председател", ignoreCase = true)

    fun reload() {
        val requestDepartment = selectedDepartment
        val requestMonth = month
        val requestYear = year
        val requestAggregation = aggregation
        val requestChairmanFor = chairmanFor
        val requestCommercialDepartment = session.user.department.ifBlank { requestDepartment }
        loading = true
        payload = null
        commercialPayload = null
        if (isChairmanUser && requestChairmanFor != "commerce") commercialLoading = true
        error = ""
        scope.launch {
            val cachedPayload = repository.readCachedDashboard(
                session = session,
                department = requestDepartment,
                month = requestMonth,
                year = requestYear,
                aggregation = requestAggregation,
                chairmanFor = requestChairmanFor,
            )
            if (cachedPayload != null) {
                payload = cachedPayload
            }
            if (isChairmanUser && requestChairmanFor != "commerce") {
                val cachedCommercialPayload = repository.readCachedDashboard(
                    session = session,
                    department = requestCommercialDepartment,
                    month = requestMonth,
                    year = requestYear,
                    aggregation = requestAggregation,
                    chairmanFor = "commerce",
                )
                commercialPayload = cachedCommercialPayload
            }
            var shouldPollPayload = false
            when (
                val result = repository.fetchDashboard(
                    session,
                    requestDepartment,
                    requestMonth,
                    requestYear,
                    requestAggregation,
                    requestChairmanFor,
                )
            ) {
                is ApiResult.Success -> {
                    payload = result.value
                    shouldPollPayload = result.value.hasActiveCacheRefresh()
                }
                is ApiResult.Failure -> {
                    if (cachedPayload == null) error = result.message
                    if (result.unauthorized) onSessionExpired()
                }
            }
            var shouldPollCommercialPayload = false
            if (isChairmanUser && requestChairmanFor != "commerce") {
                when (
                    val result = repository.fetchDashboard(
                        session = session,
                        department = requestCommercialDepartment,
                        month = requestMonth,
                        year = requestYear,
                        aggregation = requestAggregation,
                        chairmanFor = "commerce",
                    )
                ) {
                    is ApiResult.Success -> {
                        commercialPayload = result.value
                        shouldPollCommercialPayload = result.value.hasActiveCacheRefresh()
                    }
                    is ApiResult.Failure -> {
                        if (result.unauthorized) onSessionExpired()
                    }
                }
                commercialLoading = false
            }
            when (val result = repository.fetchImmediateSubordinates(session, requestDepartment)) {
                is ApiResult.Success -> hierarchy = result.value
                is ApiResult.Failure -> if (result.unauthorized) onSessionExpired()
            }
            when (val result = repository.fetchChairmanCatalog(session)) {
                is ApiResult.Success -> chairmanCatalog = result.value
                is ApiResult.Failure -> Unit
            }
            loading = false
            if (shouldPollPayload) {
                var keepPollingPayload = true
                repeat(6) {
                    if (!keepPollingPayload) return@repeat
                    delay(20_000)
                    if (
                        selectedDepartment != requestDepartment ||
                        month != requestMonth ||
                        year != requestYear ||
                        aggregation != requestAggregation ||
                        chairmanFor != requestChairmanFor
                    ) {
                        keepPollingPayload = false
                        return@repeat
                    }
                    when (
                        val result = repository.fetchDashboard(
                            session,
                            requestDepartment,
                            requestMonth,
                            requestYear,
                            requestAggregation,
                            requestChairmanFor,
                        )
                    ) {
                        is ApiResult.Success -> {
                            payload = result.value
                            if (!result.value.hasActiveCacheRefresh()) keepPollingPayload = false
                        }
                        is ApiResult.Failure -> {
                            if (result.unauthorized) onSessionExpired()
                            keepPollingPayload = false
                        }
                    }
                }
            }
            if (shouldPollCommercialPayload) {
                var keepPollingCommercialPayload = true
                repeat(6) {
                    if (!keepPollingCommercialPayload) return@repeat
                    delay(20_000)
                    if (
                        selectedDepartment != requestDepartment ||
                        month != requestMonth ||
                        year != requestYear ||
                        aggregation != requestAggregation ||
                        chairmanFor != requestChairmanFor
                    ) {
                        keepPollingCommercialPayload = false
                        return@repeat
                    }
                    when (
                        val result = repository.fetchDashboard(
                            session = session,
                            department = requestCommercialDepartment,
                            month = requestMonth,
                            year = requestYear,
                            aggregation = requestAggregation,
                            chairmanFor = "commerce",
                        )
                    ) {
                        is ApiResult.Success -> {
                            commercialPayload = result.value
                            if (!result.value.hasActiveCacheRefresh()) keepPollingCommercialPayload = false
                        }
                        is ApiResult.Failure -> {
                            if (result.unauthorized) onSessionExpired()
                            keepPollingCommercialPayload = false
                        }
                    }
                }
            }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardDesign.Screen),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
                var dragAmount = 0f
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp)
                        .padding(top = 104.dp)
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
                        val hasProductionShopSwitch = currentPayload.tiles.hasProductionShopTiles()
                        when (selectedSection) {
                            DashboardSection.Tiles -> {
                                if (hasProductionShopSwitch) {
                                    item {
                                        ProductionShopSwitch(
                                            selectedShop = selectedProductionShop,
                                            onSelect = { selectedProductionShop = it },
                                        )
                                    }
                                }
                                item {
                                    KpiTilesBlock(
                                        if (hasProductionShopSwitch) {
                                            currentPayload.tiles.filterProductionShopTiles(selectedProductionShop)
                                        } else {
                                            currentPayload.tiles
                                        },
                                    )
                                }
                            }
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
        PullDownDashboardHeader(
            expanded = headerMenuExpanded,
            session = session,
            selectedDepartment = selectedDepartment,
            hierarchy = hierarchy,
            onToggle = { headerMenuExpanded = !headerMenuExpanded },
            onExpand = { headerMenuExpanded = true },
            onCollapse = { headerMenuExpanded = false },
            onLogout = onLogout,
            onDepartmentSelected = {
                selectedDepartment = it
                chairmanFor = null
                headerMenuExpanded = false
            },
            onBackHome = {
                selectedDepartment = session.user.department
                chairmanFor = null
                headerMenuExpanded = false
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )
        DashboardBottomBar(
            selectedSection = selectedSection,
            isAdmin = session.user.isAdmin,
            onSelect = { selectedSection = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PullDownDashboardHeader(
    expanded: Boolean,
    session: LoginSession,
    selectedDepartment: String,
    hierarchy: List<HierarchyNode>,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onLogout: () -> Unit,
    onDepartmentSelected: (String) -> Unit,
    onBackHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val collapsedHeight = 96.dp
    val expandedHeight = screenHeight * 0.52f
    val height by animateDpAsState(
        targetValue = if (expanded) expandedHeight else collapsedHeight,
        animationSpec = tween(durationMillis = 360),
    )
    var dragAmount = 0f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(10.dp)
            .pointerInput(expanded) {
                detectVerticalDragGestures(
                    onDragStart = { dragAmount = 0f },
                    onVerticalDrag = { _, dragDelta -> dragAmount += dragDelta },
                    onDragEnd = {
                        when {
                            dragAmount > 48f -> onExpand()
                            dragAmount < -48f -> onCollapse()
                        }
                    },
                )
            },
        color = DashboardDesign.Navy,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            bottomStart = 24.dp,
            bottomEnd = 24.dp,
        ),
    ) {
        Column {
            DashboardTopBar(
                session = session,
                selectedDepartment = selectedDepartment,
                onMenuClick = onToggle,
                onLogout = onLogout,
            )
            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    HierarchyBlock(
                        currentDepartment = selectedDepartment,
                        homeDepartment = session.user.department,
                        children = hierarchy,
                        onDepartmentSelected = onDepartmentSelected,
                        onBackHome = onBackHome,
                    )
                }
            }
        }
    }
}

private fun DashboardPayload.hasActiveCacheRefresh(): Boolean {
    return tiles.any { it.cacheRefreshStatus.equals("running", ignoreCase = true) } ||
        tables.any { it.cacheRefreshStatus.equals("running", ignoreCase = true) }
}

@Composable
private fun ProductionShopSwitch(
    selectedShop: String,
    onSelect: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DashboardDesign.Card,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProductionShopButton(
                label = "Турбулентность-Дон",
                selected = selectedShop == "pc1",
                onClick = { onSelect("pc1") },
                modifier = Modifier.weight(1f),
            )
            ProductionShopButton(
                label = "Алмаз",
                selected = selectedShop == "pc2",
                onClick = { onSelect("pc2") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProductionShopButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) DashboardDesign.SoftAccent else Color.Transparent,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    ) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 9.dp),
            color = if (selected) DashboardDesign.Accent else DashboardDesign.MutedText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun List<KpiTile>.hasProductionShopTiles(): Boolean {
    return any { tile ->
        val id = tile.id.uppercase()
        id.startsWith("PD-M1.1") ||
            id.startsWith("PD-M1.2") ||
            id == "PD-M3.B1" ||
            id == "PD-M3.B2" ||
            id == "PD-M3.F1" ||
            id == "PD-M3.F2" ||
            id == "PD-Q2.1" ||
            id == "PD-Q2.2"
    }
}

private fun List<KpiTile>.filterProductionShopTiles(shop: String): List<KpiTile> {
    return filter { tile ->
        when (tile.productionShopKey()) {
            "pc1" -> shop == "pc1"
            "pc2" -> shop == "pc2"
            else -> true
        }
    }
}

private fun KpiTile.productionShopKey(): String? {
    val id = this.id.uppercase()
    return when {
        id.startsWith("PD-M1.1") || id == "PD-M3.B1" || id == "PD-M3.F1" || id == "PD-Q2.1" -> "pc1"
        id.startsWith("PD-M1.2") || id == "PD-M3.B2" || id == "PD-M3.F2" || id == "PD-Q2.2" -> "pc2"
        else -> null
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
