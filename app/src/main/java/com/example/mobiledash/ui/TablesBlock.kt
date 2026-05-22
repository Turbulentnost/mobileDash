package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.TableBlock

@Composable
fun TablesBlock(tables: List<TableBlock>) {
    var query by remember(tables) { mutableStateOf("") }
    val filteredTables = remember(tables, query) {
        if (query.isBlank()) {
            tables
        } else {
            val normalized = query.trim().lowercase()
            tables.mapNotNull { table ->
                val rows = table.rows.filter { row ->
                    table.title.lowercase().contains(normalized) ||
                        table.description.lowercase().contains(normalized) ||
                        row.any { it.lowercase().contains(normalized) }
                }
                if (rows.isEmpty()) null else table.copy(rows = rows)
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TablesHeader(query = query, onQueryChange = { query = it })
        if (tables.isEmpty()) {
            Text("Таблицы отсутствуют в ответе API", color = DashboardDesign.MutedText)
            return
        }
        if (filteredTables.isEmpty()) {
            Text("По запросу ничего не найдено", color = DashboardDesign.MutedText)
            return
        }
        filteredTables.forEach { table ->
            TableCard(table)
        }
    }
}

@Composable
private fun TablesHeader(query: String, onQueryChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Таблицы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            Surface(color = DashboardDesign.SoftAccent, shape = RoundedCornerShape(10.dp)) {
                Text(
                    "Как во фронте",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = DashboardDesign.Navy,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
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
private fun TableCard(table: TableBlock) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        "${table.rows.size} строк · ${table.headers.size} колонок · ${table.key}",
                        color = DashboardDesign.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            if (table.description.isNotBlank()) {
                Text(
                    table.description,
                    color = DashboardDesign.MutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            HeaderChips(headers = table.headers)
            HorizontalDivider(color = DashboardDesign.Border)
            table.rows.take(20).forEachIndexed { index, row ->
                TableDataRow(index = index + 1, headers = table.headers, row = row)
            }
            if (table.rows.size > 20) {
                Text(
                    "Еще ${table.rows.size - 20} строк",
                    color = DashboardDesign.Accent,
                    style = MaterialTheme.typography.labelSmall,
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
            Text("$index. $primary", color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold, maxLines = 2)
            headers.zip(row).drop(1).forEach { (header, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(header, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(max = 150.dp))
                    Text(value, color = DashboardDesign.Text, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
        }
    }
}
