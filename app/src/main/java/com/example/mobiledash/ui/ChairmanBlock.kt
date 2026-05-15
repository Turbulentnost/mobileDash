package com.example.mobiledash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.ChairmanCatalogItem

@Composable
fun ChairmanBlock(
    catalog: List<ChairmanCatalogItem>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Разделы ПСД", style = MaterialTheme.typography.titleLarge)
            Text(
                "Выбор раздела перезагружает dashboard с параметром for=, как во фронте.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AssistChip(onClick = { onSelect(null) }, label = { Text(if (selectedId == null) "• Обзор" else "Обзор") })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(catalog) { item ->
                    AssistChip(
                        onClick = { onSelect(item.id) },
                        label = { Text(if (selectedId == item.id) "• ${item.title}" else item.title) },
                    )
                }
            }
            catalog.take(6).forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        if (item.subtitle.isNotBlank()) Text(item.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
