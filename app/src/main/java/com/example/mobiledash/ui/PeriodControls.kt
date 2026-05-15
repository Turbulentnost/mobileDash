package com.example.mobiledash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PeriodControls(
    month: Int,
    year: Int,
    aggregation: String,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAggregationChange: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val aggregationLabel = when (aggregation) {
        "quarter" -> "Квартал"
        "ytd" -> "Год"
        else -> "Месяц"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = onPrevious) { Text("‹") }
                Text(
                    "${monthName(month)} $year",
                    fontWeight = FontWeight.Bold,
                    color = DashboardDesign.Text,
                    modifier = Modifier.padding(top = 12.dp),
                )
                OutlinedButton(onClick = onNext, enabled = canGoNext) { Text("›") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { expanded = true }) {
                    Text("Разбивка: $aggregationLabel")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("current" to "Месяц", "quarter" to "Квартал", "ytd" to "Год").forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                expanded = false
                                onAggregationChange(value)
                            },
                        )
                    }
                }
            }
        }
    }
}

fun monthName(month: Int): String = when (month) {
    1 -> "Январь"
    2 -> "Февраль"
    3 -> "Март"
    4 -> "Апрель"
    5 -> "Май"
    6 -> "Июнь"
    7 -> "Июль"
    8 -> "Август"
    9 -> "Сентябрь"
    10 -> "Октябрь"
    11 -> "Ноябрь"
    12 -> "Декабрь"
    else -> "Месяц"
}
