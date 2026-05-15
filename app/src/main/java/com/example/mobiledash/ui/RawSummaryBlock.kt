package com.example.mobiledash.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RawSummaryBlock(summary: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Сводка API", style = MaterialTheme.typography.titleLarge)
            Text(summary.ifBlank { "Пустой ответ" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
