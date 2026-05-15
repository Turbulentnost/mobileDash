package com.example.mobiledash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.TableBlock

@Composable
fun TablesBlock(tables: List<TableBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TablesHeader()
        if (tables.isEmpty()) {
            Text("Таблицы отсутствуют в ответе API")
            return
        }
        groupedTables(tables).forEach { (group, groupTables) ->
            TableGroupCard(group, groupTables)
        }
    }
}

@Composable
private fun TablesHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Таблицы", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            Surface(color = DashboardDesign.SoftAccent, shape = RoundedCornerShape(10.dp)) {
                Text(
                    "Фильтры",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = DashboardDesign.Navy,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Все", "Избранное", "Недавние").forEachIndexed { index, label ->
                Surface(
                    color = if (index == 0) DashboardDesign.Navy else Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        color = if (index == 0) Color.White else DashboardDesign.MutedText,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        OutlinedTextField(
            value = "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Поиск по таблицам") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun TableGroupCard(group: String, tables: List<TableBlock>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            tables.take(4).forEach { table ->
                TablePreviewCard(table)
                HorizontalDivider(color = DashboardDesign.Border)
            }
            if (tables.size > 4) {
                Text(
                    "Показать все (${tables.size})",
                    color = DashboardDesign.Accent,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TablePreviewCard(table: TableBlock) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .background(DashboardDesign.SoftAccent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 6.dp),
            ) {
                Text("T", color = DashboardDesign.Accent, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(table.title, fontWeight = FontWeight.SemiBold, color = DashboardDesign.Text)
                Text(
                    "${table.rows.size} строк · ${table.headers.size} колонок",
                    color = DashboardDesign.MutedText,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text("☆", color = DashboardDesign.MutedText)
        }
        table.rows.take(5).forEach { row ->
            TableDataRow(headers = table.headers, row = row)
        }
        if (table.rows.size > 5) {
            Text(
                "Еще ${table.rows.size - 5} строк",
                color = DashboardDesign.Accent,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun TableDataRow(headers: List<String>, row: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DashboardDesign.Screen,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val primary = row.firstOrNull { it.isNotBlank() && it != "—" } ?: "Строка таблицы"
            Text(primary, color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold, maxLines = 2)
            headers.zip(row).drop(1).take(5).forEach { (header, value) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(header, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelSmall, modifier = Modifier.widthIn(max = 150.dp))
                    Text(value, color = DashboardDesign.Text, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
        }
    }
}

private fun groupedTables(tables: List<TableBlock>): Map<String, List<TableBlock>> {
    val fallbackGroups = listOf("Логистика", "Финансы", "Персонал", "Закупки")
    return tables.mapIndexed { index, table ->
        val lower = table.title.lowercase()
        val group = when {
            "лог" in lower || "claim" in lower || "lawsuit" in lower || "debt" in lower -> "Логистика"
            "фин" in lower || "budget" in lower || "дебитор" in lower -> "Финансы"
            "персон" in lower || "штат" in lower -> "Персонал"
            "закуп" in lower || "постав" in lower -> "Закупки"
            else -> fallbackGroups[index % fallbackGroups.size]
        }
        group to table
    }.groupBy({ it.first }, { it.second })
}
