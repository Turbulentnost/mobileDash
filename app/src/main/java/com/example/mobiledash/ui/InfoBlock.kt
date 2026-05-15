package com.example.mobiledash.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.AppConfig

@Composable
fun InfoBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Инфо", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
            colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
            shape = RoundedCornerShape(DashboardDesign.CardRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TempImage(
                        asset = TempImageAsset.Logo,
                        contentDescription = "Логотип MobileDash",
                        modifier = Modifier.size(72.dp),
                    )
                    Column {
                        Text("MobileDash", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = DashboardDesign.Navy)
                        Text("Корпоративный KPI dashboard", color = DashboardDesign.MutedText)
                    }
                }
                Text(
                    "Мобильная версия корпоративного KPI-дашборда. Приложение показывает те же показатели, графики, таблицы и иерархию подразделений, что и фронт, но в формате, удобном для телефона.",
                    color = DashboardDesign.Text,
                )
                InfoRow("Данные", "загружаются из существующего Django API")
                InfoRow("Авторизация", "через JWT Bearer, как во фронте")
                InfoRow("Навигация", "плитки, графики, таблицы и иерархия подразделений")
                InfoRow("API", AppConfig.API_BASE_URL)
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardDesign.SoftAccent),
            shape = RoundedCornerShape(DashboardDesign.CardRadius),
        ) {
            Text(
                "Совет: свайпайте dashboard влево/вправо для перехода между месяцами, нажимайте KPI-плитки для подробностей.",
                modifier = Modifier.padding(16.dp),
                color = DashboardDesign.Navy,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = DashboardDesign.MutedText)
        Surface(color = DashboardDesign.Screen, shape = RoundedCornerShape(10.dp)) {
            Text(
                value,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                color = DashboardDesign.Text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
