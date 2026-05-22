package com.example.mobiledash.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.ChartBlock
import com.example.mobiledash.data.ChartPoint
import com.example.mobiledash.data.ChartSeries
import com.example.mobiledash.data.ChartType
import com.example.mobiledash.data.formatCompactNumber
import kotlin.math.max

@Composable
fun ChartsBlock(charts: List<ChartBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Ключевые графики", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = DashboardDesign.Text)
            Text("Смотреть все", color = DashboardDesign.Accent, style = MaterialTheme.typography.labelLarge)
        }
        if (charts.isEmpty()) {
            Text("Графики отсутствуют в ответе API", color = DashboardDesign.MutedText)
            return
        }
        charts.forEach { chart ->
            ChartCard(chart)
        }
    }
}

@Composable
private fun ChartCard(chart: ChartBlock) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DashboardDesign.Border, RoundedCornerShape(DashboardDesign.CardRadius)),
        colors = CardDefaults.cardColors(containerColor = DashboardDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(DashboardDesign.CardRadius),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(chart.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = DashboardDesign.Text)
            when (chart.type) {
                ChartType.Line -> LineChart(chart.series.ifEmpty { listOf(ChartSeries(chart.title, chart.points)) })
                ChartType.Bar -> BarChart(chart.points)
                ChartType.Donut -> DonutGrid(chart.points)
            }
            if (chart.type != ChartType.Donut) {
                val legendPoints = if (chart.type == ChartType.Line) chart.series.mapNotNull { series ->
                    series.points.lastOrNull()?.let { ChartPoint(series.name, it.value) }
                } else {
                    chart.points
                }
                legendPoints.take(6).forEach {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it.label, maxLines = 1, color = DashboardDesign.MutedText)
                        Text(
                            formatCompactNumber(it.value),
                            fontWeight = FontWeight.SemiBold,
                            color = chartPointColor(it.color),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineChart(seriesList: List<ChartSeries>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        val drawableSeries = seriesList.filter { it.points.size >= 2 }
        if (drawableSeries.isEmpty()) return@Canvas
        val values = drawableSeries.flatMap { it.points.map(ChartPoint::value) }
        val maxValue = max(values.maxOrNull() ?: 1.0, 1.0)
        val colors = listOf(
            DashboardDesign.Accent,
            Color(0xFF16A34A),
            Color(0xFFEAB308),
            Color(0xFFDC2626),
            Color(0xFF9333EA),
            Color(0xFF0891B2),
        )
        drawableSeries.forEachIndexed { seriesIndex, series ->
            val stepX = size.width / (series.points.size - 1)
            val color = chartPointColor(series.color) { colors[seriesIndex % colors.size] }
            val mapped = series.points.mapIndexed { index, point ->
                Offset(
                    x = stepX * index,
                    y = size.height - ((point.value / maxValue).toFloat() * size.height),
                )
            }
            mapped.zipWithNext().forEach { (start, end) ->
                if (series.dashed) {
                    drawDashedLine(color, start, end)
                } else {
                    drawLine(color, start, end, strokeWidth = 4f, cap = StrokeCap.Round)
                }
            }
            if (!series.dashed) {
                mapped.forEach { point ->
                    drawCircle(color, radius = 5f, center = point)
                    drawCircle(Color.White, radius = 2.4f, center = point)
                }
            }
        }
    }
}

@Composable
private fun BarChart(points: List<ChartPoint>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        if (points.isEmpty()) return@Canvas
        val maxValue = max(points.maxOf { max(it.value, it.plan ?: 0.0) }, 1.0)
        val groupGap = 10f
        val innerGap = 3f
        val groupWidth = (size.width - groupGap * (points.size + 1)) / points.size
        val barWidth = (groupWidth - innerGap) / 2f
        points.forEachIndexed { index, point ->
            val height = ((point.value / maxValue).toFloat() * size.height).coerceAtLeast(2f)
            val left = groupGap + (groupWidth + groupGap) * index
            point.plan?.let { plan ->
                val planHeight = ((plan / maxValue).toFloat() * size.height).coerceAtLeast(2f)
                drawRect(
                    color = Color(0xFFC8D6EE),
                    topLeft = Offset(left, size.height - planHeight),
                    size = Size(barWidth, planHeight),
                )
            }
            drawRect(
                color = Color(0xFF2B5CA6),
                topLeft = Offset(left + barWidth + innerGap, size.height - height),
                size = Size(barWidth, height),
            )
        }
    }
}

@Composable
private fun DonutGrid(points: List<ChartPoint>) {
    if (points.isEmpty()) {
        Text("Нет KPI-процентов для круговых диаграмм", color = DashboardDesign.MutedText)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        points.chunked(2).forEach { rowPoints ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowPoints.forEach { point ->
                    DonutCell(point = point, modifier = Modifier.weight(1f))
                }
                if (rowPoints.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DonutCell(point: ChartPoint, modifier: Modifier = Modifier) {
    val hasPercent = point.percentLabel != "—"
    val fill = if (hasPercent) chartPointColor(point.color) else DashboardDesign.MutedText
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DashboardDesign.Screen)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
            SingleDonutChart(percent = point.value, color = fill, hasPercent = hasPercent)
            Text(
                point.percentLabel.ifBlank { "${formatCompactNumber(point.value)}%" },
                color = fill,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            point.label,
            maxLines = 2,
            color = DashboardDesign.Text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SingleDonutChart(percent: Double, color: Color, hasPercent: Boolean) {
    Canvas(modifier = Modifier.size(96.dp)) {
        val stroke = size.minDimension * 0.14f
        val side = size.minDimension - stroke
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        val displayPercent = if (hasPercent) percent.coerceAtLeast(0.0) else 0.0
        val sweep = (displayPercent.coerceAtMost(100.0) / 100.0 * 360.0).toFloat()
        drawArc(
            color = Color(0xFFE2E8F0),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(side, side),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (hasPercent && sweep > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(side, side),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDashedLine(
    color: Color,
    start: Offset,
    end: Offset,
) {
    val segments = 14
    for (i in 0 until segments step 2) {
        val segmentStart = Offset(
            x = start.x + (end.x - start.x) * i / segments,
            y = start.y + (end.y - start.y) * i / segments,
        )
        val segmentEnd = Offset(
            x = start.x + (end.x - start.x) * (i + 1) / segments,
            y = start.y + (end.y - start.y) * (i + 1) / segments,
        )
        drawLine(color, segmentStart, segmentEnd, strokeWidth = 4f, cap = StrokeCap.Round)
    }
}

private fun chartPointColor(raw: String?, fallback: () -> Color = { DashboardDesign.Accent }): Color {
    val value = raw?.trim().orEmpty()
    if (value.startsWith("#")) {
        return runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrElse { fallback() }
    }
    return when {
        "green" in value || "success" in value || "good" in value -> DashboardDesign.Positive
        "yellow" in value || "amber" in value || "warning" in value -> Color(0xFFEAB308)
        "red" in value || "danger" in value || "bad" in value -> DashboardDesign.Negative
        else -> fallback()
    }
}
