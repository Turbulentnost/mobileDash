package com.example.mobiledash.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.KpiTile
import kotlin.math.abs

@Composable
fun KpiTilesBlock(tiles: List<KpiTile>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Дашборд", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            Text("Все KPI", color = DashboardDesign.Accent, style = MaterialTheme.typography.labelLarge)
        }
        if (tiles.isEmpty()) {
            Text("Плитки отсутствуют в ответе API", color = DashboardDesign.MutedText)
            return
        }
        tiles.forEach { tile ->
            KpiTileCard(tile)
        }
    }
}

@Composable
private fun KpiTileCard(tile: KpiTile) {
    val accent = ragColor(tile.rag)
    var flipped by remember(tile.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "kpi-card-flip",
    )
    val showBack = rotation > 90f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { flipped = !flipped }
            .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .graphicsLayer {
                    if (showBack) rotationY = 180f
                },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showBack) {
                KpiTileBack(tile = tile, accent = accent)
            } else {
                KpiTileFront(tile = tile, accent = accent)
            }
        }
    }
}

@Composable
private fun KpiTileFront(tile: KpiTile, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DashboardDesign.Text)
                    if (tile.period.isNotBlank()) {
                        Text(tile.period, color = DashboardDesign.SecondaryText, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Surface(
                    color = DashboardDesign.SoftAccent,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        tile.badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = DashboardDesign.Navy,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        tile.fact,
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(tile.units.ifBlank { "показатель" }, color = DashboardDesign.MutedText)
                }
                DeltaPill(tile.kpiPercent, accent)
            }
            Sparkline(tile.id + tile.fact, accent)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricMini("План", tile.plan)
                MetricMini("KPI", tile.kpiPercent)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            if (!tile.hasData) Text("Данные были сгенерированы", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun KpiTileBack(tile: KpiTile, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Детальная информация", color = DashboardDesign.Text, fontWeight = FontWeight.Bold)
                Text(tile.title, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelMedium)
            }
            Surface(color = DashboardDesign.SoftAccent, shape = RoundedCornerShape(8.dp)) {
                Text(
                    tile.badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = DashboardDesign.Navy,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        DetailBlock("Формула расчета", "KPI = факт / план × 100%")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailBlock("Факт", tile.fact, modifier = Modifier.weight(1f))
            DetailBlock("План", tile.plan, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            DetailBlock("KPI", tile.kpiPercent, modifier = Modifier.weight(1f))
            DetailBlock("Период", tile.period.ifBlank { "—" }, modifier = Modifier.weight(1f))
        }
        DetailBlock("Единицы", tile.units.ifBlank { "—" })
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Статус", color = DashboardDesign.MutedText)
            Text(if (tile.hasData) "Данные из API" else "Данные были сгенерированы", color = accent, fontWeight = FontWeight.SemiBold)
        }
        Text("Нажмите ещё раз, чтобы вернуться", color = DashboardDesign.SecondaryText, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = DashboardDesign.Screen,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DashboardDesign.Border),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelSmall)
            Text(value.ifBlank { "—" }, color = DashboardDesign.Text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DeltaPill(value: String, color: Color) {
    Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(10.dp)) {
        Text(
            value.ifBlank { "—" },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetricMini(label: String, value: String) {
    Column {
        Text(label, color = DashboardDesign.MutedText, style = MaterialTheme.typography.labelSmall)
        Text(value.ifBlank { "—" }, fontWeight = FontWeight.SemiBold, color = DashboardDesign.Text)
    }
}

@Composable
private fun Sparkline(seed: String, color: Color) {
    val points = rememberSparkline(seed)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
    ) {
        if (points.size < 2) return@Canvas
        val step = size.width / (points.size - 1)
        val mapped = points.mapIndexed { index, value ->
            Offset(index * step, size.height - value * size.height)
        }
        mapped.zipWithNext().forEach { (start, end) ->
            drawLine(color, start, end, strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
    Spacer(Modifier.height(2.dp))
}

private fun rememberSparkline(seed: String): List<Float> {
    val base = abs(seed.hashCode()).coerceAtLeast(1)
    return List(10) { index ->
        (((base / (index + 3)) % 70) / 100f + 0.18f).coerceIn(0.10f, 0.95f)
    }
}

private fun ragColor(rag: String): Color = when {
    "green" in rag || "success" in rag || "good" in rag -> DashboardDesign.Positive
    "yellow" in rag || "warning" in rag -> Color(0xFFEAB308)
    "red" in rag || "danger" in rag || "bad" in rag -> DashboardDesign.Negative
    else -> DashboardDesign.Positive
}
