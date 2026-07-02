package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.TableBlock
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun TablesBlock(tables: List<TableBlock>) {
    var query by remember(tables) { mutableStateOf("") }
    var commercialTableMode by remember(tables) { mutableStateOf("claims") }
    val isCommercialTables = tables.hasCommercialDirectorTables()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TablesHeader(query = query, onQueryChange = { query = it })
        if (tables.isEmpty()) {
            Text("Таблицы отсутствуют в ответе API", color = DashboardDesign.MutedText)
            return
        }
        if (isCommercialTables) {
            val claimsOrLawsuits = tables.firstOrNull {
                it.key.equals(if (commercialTableMode == "claims") "KD-T-CLAIMS" else "KD-T-LAWSUITS", ignoreCase = true)
            }
            if (claimsOrLawsuits != null) {
                TableCard(
                    table = claimsOrLawsuits,
                    globalQuery = query,
                    modeSwitch = {
                        ClaimsLawsuitsSwitch(
                            selected = commercialTableMode,
                            onSelect = { commercialTableMode = it },
                        )
                    },
                )
            }
            tables.firstOrNull { it.key.equals("KD-T-OVERDUE", ignoreCase = true) }?.let { overdueTable ->
                TableCard(table = overdueTable, globalQuery = query)
            }
        } else {
            tables.forEach { table ->
                TableCard(table = table, globalQuery = query)
            }
        }
    }
}

@Composable
private fun TablesHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Таблицы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Поиск по таблицам") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun ClaimsLawsuitsSwitch(selected: String, onSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DashboardDesign.Card,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
    ) {
        Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TableModeButton(
                label = "Претензии",
                selected = selected == "claims",
                onClick = { onSelect("claims") },
                modifier = Modifier.weight(1f),
            )
            TableModeButton(
                label = "Суды",
                selected = selected == "lawsuits",
                onClick = { onSelect("lawsuits") },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TableModeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) DashboardDesign.SoftAccent else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            color = if (selected) DashboardDesign.Accent else DashboardDesign.MutedText,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TableCard(
    table: TableBlock,
    globalQuery: String,
    modeSwitch: (@Composable () -> Unit)? = null,
) {
    var expanded by remember(table.key, table.title) { mutableStateOf(true) }
    var currentPage by remember(table.key, table.rows.size, globalQuery) { mutableStateOf(1) }
    var filtersExpanded by remember(table.key) { mutableStateOf(false) }
    var fieldFilters by remember(table.key, table.headers) { mutableStateOf(emptyMap<String, String>()) }
    val filteredRows = remember(table, globalQuery, fieldFilters) {
        table.rows.filterByQueryAndFields(table.headers, globalQuery, fieldFilters)
    }
    val totalPages = maxPageCount(filteredRows.size)
    if (currentPage > totalPages) currentPage = totalPages
    val pageRows = filteredRows.drop((currentPage - 1) * ROWS_PER_TABLE_PAGE).take(ROWS_PER_TABLE_PAGE)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(DashboardDesign.SoftAccent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                ) {
                    Text("T", color = DashboardDesign.Accent, fontWeight = FontWeight.Bold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
                    Text(
                        "${filteredRows.size} из ${table.rows.size} строк · ${table.headers.size} колонок",
                        color = DashboardDesign.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Surface(
                    color = if (expanded) DashboardDesign.Navy else DashboardDesign.SoftAccent,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text(
                        if (expanded) "Свернуть" else "Раскрыть",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = if (expanded) Color.White else DashboardDesign.Navy,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (expanded) {
                modeSwitch?.invoke()
                if (table.description.isNotBlank()) {
                    Text(
                        table.description,
                        color = DashboardDesign.MutedText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Фильтры по полям", color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (filtersExpanded) "Скрыть" else "Открыть",
                        color = DashboardDesign.Accent,
                        modifier = Modifier.clickable { filtersExpanded = !filtersExpanded },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (filtersExpanded) {
                    FieldFilters(
                        headers = table.headers,
                        filters = fieldFilters,
                        onFilterChange = { header, value ->
                            currentPage = 1
                            fieldFilters = if (value.isBlank()) {
                                fieldFilters - header
                            } else {
                                fieldFilters + (header to value)
                            }
                        },
                    )
                }
                HeaderChips(headers = table.headers)
                if (pageRows.isEmpty()) {
                    Text("По фильтрам ничего не найдено", color = DashboardDesign.MutedText)
                }
                pageRows.forEachIndexed { index, row ->
                    TableDataRow(
                        index = (currentPage - 1) * ROWS_PER_TABLE_PAGE + index + 1,
                        headers = table.headers,
                        row = row,
                    )
                }
                if (totalPages > 1) {
                    TablePagination(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { currentPage = it },
                    )
                } else if (filteredRows.size > ROWS_PER_TABLE_PAGE) {
                    Text(
                        "Показано ${min(filteredRows.size, ROWS_PER_TABLE_PAGE)} строк",
                        color = DashboardDesign.Accent,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun FieldFilters(
    headers: List<String>,
    filters: Map<String, String>,
    onFilterChange: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        headers.take(8).forEach { header ->
            OutlinedTextField(
                value = filters[header].orEmpty(),
                onValueChange = { onFilterChange(header, it) },
                label = { Text(header) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
        }
        if (headers.size > 8) {
            Text(
                "Фильтры показаны для первых 8 полей",
                color = DashboardDesign.MutedText,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TablePagination(currentPage: Int, totalPages: Int, onPageChange: (Int) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..totalPages).forEach { page ->
            Surface(
                modifier = Modifier.clickable { onPageChange(page) },
                color = if (page == currentPage) DashboardDesign.Navy else DashboardDesign.SoftAccent,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    page.toString(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = if (page == currentPage) Color.White else DashboardDesign.Navy,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HeaderChips(headers: List<String>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        headers.forEach { header ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(9.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
            ) {
                Text(
                    header,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    color = DashboardDesign.MutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private const val ROWS_PER_TABLE_PAGE = 5

private fun maxPageCount(rows: Int): Int = maxOf(1, ceil(rows / ROWS_PER_TABLE_PAGE.toDouble()).toInt())

private fun List<TableBlock>.hasCommercialDirectorTables(): Boolean {
    return any { it.key.equals("KD-T-CLAIMS", ignoreCase = true) } &&
        any { it.key.equals("KD-T-LAWSUITS", ignoreCase = true) } &&
        any { it.key.equals("KD-T-OVERDUE", ignoreCase = true) }
}

private fun List<List<String>>.filterByQueryAndFields(
    headers: List<String>,
    query: String,
    filters: Map<String, String>,
): List<List<String>> {
    val normalizedQuery = query.trim().lowercase()
    return filter { row ->
        val matchesQuery = normalizedQuery.isBlank() || row.any { it.lowercase().contains(normalizedQuery) }
        val matchesFields = filters.all { (header, value) ->
            val columnIndex = headers.indexOf(header)
            if (columnIndex < 0) {
                true
            } else {
                row.getOrNull(columnIndex).orEmpty().lowercase().contains(value.trim().lowercase())
            }
        }
        matchesQuery && matchesFields
    }
}

@Composable
private fun TableDataRow(index: Int, headers: List<String>, row: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DashboardDesign.Screen,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val primary = row.firstOrNull { it.isNotBlank() && it != "—" } ?: "Строка таблицы"
            Text("$index. $primary", color = DashboardDesign.Text, fontWeight = FontWeight.Bold)
            headers.zip(row).drop(1).forEach { (header, value) ->
                val isAmount = header.isAmountField()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        header,
                        color = DashboardDesign.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .weight(0.42f)
                            .widthIn(max = 150.dp),
                    )
                    Text(
                        value,
                        color = if (isAmount) DashboardDesign.Text else DashboardDesign.Text,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isAmount) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(0.58f),
                    )
                }
            }
        }
    }
}

private fun String.isAmountField(): Boolean {
    val normalized = lowercase()
    return "сумм" in normalized ||
        "руб" in normalized ||
        "amount" in normalized ||
        "требован" in normalized
}
