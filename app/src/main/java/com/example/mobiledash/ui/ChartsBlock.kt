package com.example.mobiledash.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                ChartType.Donut -> DonutChart(chart.points)
            }
            val legendPoints = if (chart.type == ChartType.Line) chart.series.mapNotNull { series ->
                series.points.lastOrNull()?.let { ChartPoint(series.name, it.value) }
            } else chart.points
            legendPoints.take(6).forEach {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.label, maxLines = 1, color = DashboardDesign.MutedText)
                    Text(formatCompactNumber(it.value), fontWeight = FontWeight.SemiBold, color = DashboardDesign.Text)
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
            val mapped = series.points.mapIndexed { index, point ->
                Offset(
                    x = stepX * index,
                    y = size.height - ((point.value / maxValue).toFloat() * size.height),
                )
            }
            mapped.zipWithNext().forEach { (start, end) ->
                drawLine(colors[seriesIndex % colors.size], start, end, strokeWidth = 4f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun BarChart(points: List<ChartPoint>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        if (points.isEmpty()) return@Canvas
        val maxValue = max(points.maxOf { it.value }, 1.0)
        val gap = 8f
        val barWidth = (size.width - gap * (points.size + 1)) / points.size
        points.forEachIndexed { index, point ->
            val height = ((point.value / maxValue).toFloat() * size.height).coerceAtLeast(2f)
            point.plan?.let { plan ->
                val planHeight = ((plan / maxValue).toFloat() * size.height).coerceAtLeast(2f)
                drawRect(
                    color = Color(0xFFD7E2F4),
                    topLeft = Offset(gap + (barWidth + gap) * index, size.height - planHeight),
                    size = Size(barWidth, planHeight),
                )
            }
            drawRect(
                color = DashboardDesign.Accent,
                topLeft = Offset(gap + (barWidth + gap) * index, size.height - height),
                size = Size(barWidth, height),
            )
        }
    }
}

@Composable
private fun DonutChart(points: List<ChartPoint>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
        if (points.isEmpty()) return@Canvas
        val total = points.sumOf { it.value }.takeIf { it > 0 } ?: return@Canvas
        var start = -90f
        val colors = listOf(Color(0xFF2563EB), Color(0xFF16A34A), Color(0xFFEAB308), Color(0xFFDC2626), Color(0xFF9333EA))
        val side = minOf(size.width, size.height) * 0.78f
        val topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f)
        points.forEachIndexed { index, point ->
            val sweep = (point.value / total * 360.0).toFloat()
            drawArc(
                color = colors[index % colors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(side, side),
                style = Stroke(width = side * 0.18f, cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}
