package com.example.mobiledash.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.HierarchyNode

@Composable
fun HierarchyBlock(
    currentDepartment: String,
    homeDepartment: String,
    children: List<HierarchyNode>,
    onDepartmentSelected: (String) -> Unit,
    onBackHome: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text("Иерархия", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBackHome) { Text("К себе") }
                AssistChip(onClick = {}, label = { Text("${children.size} подразделений") })
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(children) { node ->
                    TextButton(
                        onClick = { onDepartmentSelected(node.department) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(node.department, color = DashboardDesign.Text, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
