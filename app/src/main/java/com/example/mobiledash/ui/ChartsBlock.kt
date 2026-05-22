package com.example.mobiledash.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiledash.data.ChartBlock
import com.example.mobiledash.data.ChartPoint
import com.example.mobiledash.data.ChartSeries
import com.example.mobiledash.data.ChartType
import com.example.mobiledash.data.formatCompactNumber
import kotlin.math.max
import kotlin.math.pow

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
            if (chart.type == ChartType.Bar) {
                chart.points.take(6).forEach {
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
    var selectedPoint by remember(seriesList) { mutableStateOf<LinePointSelection?>(null) }
    val tapRadiusPx = with(LocalDensity.current) { 32.dp.toPx() }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(seriesList) {
                detectTapGestures { tap ->
                    val drawableSeries = seriesList.filter { it.points.size >= 2 }
                    if (drawableSeries.isEmpty()) return@detectTapGestures
                    val values = drawableSeries.flatMap { it.points.map(ChartPoint::value) }
                    val maxValue = max(values.maxOrNull() ?: 1.0, 1.0)
                    val axisHeight = 30f
                    val topPadding = 16f
                    val plotHeight = (size.height - axisHeight - topPadding).coerceAtLeast(1f)
                    var nearest: LinePointSelection? = null
                    var nearestDistance = Float.MAX_VALUE
                    drawableSeries.forEachIndexed { seriesIndex, series ->
                        val stepX = size.width.toFloat() / (series.points.size - 1)
                        series.points.forEachIndexed { pointIndex, point ->
                            val mapped = Offset(
                                x = stepX * pointIndex,
                                y = topPadding + plotHeight - ((point.value / maxValue).toFloat() * plotHeight),
                            )
                            val distance = (tap.x - mapped.x).pow(2) + (tap.y - mapped.y).pow(2)
                            if (distance < nearestDistance) {
                                nearestDistance = distance
                                nearest = LinePointSelection(seriesIndex, pointIndex)
                            }
                        }
                    }
                    selectedPoint = if (nearestDistance <= tapRadiusPx * tapRadiusPx) nearest else null
                }
            },
    ) {
        val drawableSeries = seriesList.filter { it.points.size >= 2 }
        if (drawableSeries.isEmpty()) return@Canvas
        val values = drawableSeries.flatMap { it.points.map(ChartPoint::value) }
        val maxValue = max(values.maxOrNull() ?: 1.0, 1.0)
        val axisHeight = 30f
        val topPadding = 16f
        val plotHeight = (size.height - axisHeight - topPadding).coerceAtLeast(1f)
        val axisY = topPadding + plotHeight
        val colors = listOf(
            DashboardDesign.Accent,
            Color(0xFF16A34A),
            Color(0xFFEAB308),
            Color(0xFFDC2626),
            Color(0xFF9333EA),
            Color(0xFF0891B2),
        )
        val lineGroups = drawableSeries.map(::lineSeriesBaseName).distinct()
        drawableSeries.forEachIndexed { seriesIndex, series ->
            val stepX = size.width / (series.points.size - 1)
            val color = lineSeriesColor(series, lineGroups, colors)
            val mapped = series.points.mapIndexed { index, point ->
                Offset(
                    x = stepX * index,
                    y = topPadding + plotHeight - ((point.value / maxValue).toFloat() * plotHeight),
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
        drawLine(Color(0xFFCBD5E1), Offset(0f, axisY), Offset(size.width, axisY), strokeWidth = 2f)
        drawTimeAxisLabels(drawableSeries.first().points, axisY)
        selectedPoint?.let { selection ->
            val series = drawableSeries.getOrNull(selection.seriesIndex) ?: return@let
            val point = series.points.getOrNull(selection.pointIndex) ?: return@let
            val stepX = size.width / (series.points.size - 1)
            val pointOffset = Offset(
                x = stepX * selection.pointIndex,
                y = topPadding + plotHeight - ((point.value / maxValue).toFloat() * plotHeight),
            )
            val color = lineSeriesColor(series, lineGroups, colors)
            val groupName = lineSeriesBaseName(series)
            val planSeries = drawableSeries.firstOrNull { lineSeriesBaseName(it) == groupName && isPlanLineSeries(it) }
            val factSeries = drawableSeries.firstOrNull { lineSeriesBaseName(it) == groupName && !isPlanLineSeries(it) }
            val planValue = planSeries?.points?.getOrNull(selection.pointIndex)?.value
            val factValue = factSeries?.points?.getOrNull(selection.pointIndex)?.value
            drawCircle(color, radius = 8f, center = pointOffset)
            drawLine(Color(0xFF94A3B8), Offset(pointOffset.x, topPadding), Offset(pointOffset.x, axisY), strokeWidth = 1.5f)
            drawLine(Color(0xFF94A3B8), Offset(0f, pointOffset.y), Offset(size.width, pointOffset.y), strokeWidth = 1.5f)
            drawLineTooltip(
                monthLabel = point.label,
                groupName = groupName,
                planValue = planValue,
                factValue = factValue,
                anchor = pointOffset,
                color = color,
            )
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

private data class LinePointSelection(
    val seriesIndex: Int,
    val pointIndex: Int,
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTimeAxisLabels(
    points: List<ChartPoint>,
    axisY: Float,
) {
    if (points.isEmpty()) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = DashboardDesign.MutedText.toArgb()
        textSize = 24f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val maxLabels = 6
    val step = (points.size - 1).coerceAtLeast(1)
    drawIntoCanvas { canvas ->
        points.forEachIndexed { index, point ->
            val shouldDraw = points.size <= maxLabels ||
                index == 0 ||
                index == points.lastIndex ||
                index % ((points.size + maxLabels - 1) / maxLabels) == 0
            if (!shouldDraw) return@forEachIndexed
            val x = size.width * index / step
            canvas.nativeCanvas.drawText(point.label, x, axisY + 24f, paint)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineTooltip(
    monthLabel: String,
    groupName: String,
    planValue: Double?,
    factValue: Double?,
    anchor: Offset,
    color: Color,
) {
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = DashboardDesign.Text.toArgb()
        textSize = 25f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = DashboardDesign.MutedText.toArgb()
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toArgb()
        textSize = 24f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val planText = planValue?.let(::formatCompactNumber) ?: "—"
    val factText = factValue?.let(::formatCompactNumber) ?: "—"
    val planLine = "План: $planText"
    val factLine = "Факт: $factText"
    val width = (
        listOf(
            titlePaint.measureText(monthLabel),
            bodyPaint.measureText(groupName),
            valuePaint.measureText(planLine),
            valuePaint.measureText(factLine),
        ).maxOrNull() ?: 120f
        ) + 28f
    val height = 116f
    val left = (anchor.x - width / 2f).coerceIn(0f, size.width - width)
    val top = (anchor.y - height - 14f).coerceAtLeast(0f)
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(16f, 16f),
    )
    drawRoundRect(
        color = DashboardDesign.Border,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 1.5f),
    )
    drawCircle(color, radius = 5f, center = Offset(left + 15f, top + 57f))
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(monthLabel, left + 14f, top + 26f, titlePaint)
        canvas.nativeCanvas.drawText(groupName, left + 28f, top + 61f, bodyPaint)
        canvas.nativeCanvas.drawText(planLine, left + 14f, top + 86f, valuePaint)
        canvas.nativeCanvas.drawText(factLine, left + 14f, top + 110f, valuePaint)
    }
}

private fun lineSeriesColor(
    series: ChartSeries,
    groupNames: List<String>,
    palette: List<Color>,
): Color {
    val groupIndex = groupNames.indexOf(lineSeriesBaseName(series)).coerceAtLeast(0)
    return palette[groupIndex % palette.size]
}

private fun isPlanLineSeries(series: ChartSeries): Boolean {
    val role = series.valueRole.lowercase()
    val name = series.name.lowercase()
    return role == "plan" || "план" in name || " plan" in name
}

private fun lineSeriesBaseName(series: ChartSeries): String {
    return series.name
        .replace(Regex("""(?i)\s*\((план|факт|plan|fact)\)\s*$"""), "")
        .replace(Regex("""(?i)\s*[·\-:/]\s*(п|ф|план|факт|plan|fact)\s*$"""), "")
        .replace(Regex("""(?i)\s+(план|факт|plan|fact)\s*$"""), "")
        .trim()
        .ifBlank { series.name }
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
