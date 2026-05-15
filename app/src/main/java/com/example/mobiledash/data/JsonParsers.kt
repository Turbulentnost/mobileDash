package com.example.mobiledash.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

internal fun JSONObject.stringOrEmpty(vararg keys: String): String {
    for (key in keys) {
        val value = opt(key)
        if (has(key) && !isNull(key) && value != null) return value.toString().trim()
    }
    return ""
}

internal fun JSONObject.doubleOrNull(vararg keys: String): Double? {
    for (key in keys) {
        if (!has(key) || isNull(key)) continue
        val raw = opt(key)
        val value = when (raw) {
            is Number -> raw.toDouble()
            null -> null
            else -> raw.toString()
                .replace("%", "")
                .replace(" ", "")
                .replace(",", ".")
                .toDoubleOrNull()
        }
        if (value != null) return value
    }
    return null
}

internal fun JSONObject.objectOrNull(key: String): JSONObject? = optJSONObject(key)

internal fun JSONObject.arrayOrNull(key: String): JSONArray? = optJSONArray(key)

internal fun JSONArray.objects(): List<JSONObject> = buildList {
    for (i in 0 until length()) optJSONObject(i)?.let(::add)
}

internal fun JSONObject.keyList(): List<String> = buildList {
    val iterator = keys()
    while (iterator.hasNext()) add(iterator.next())
}

internal fun parseUser(obj: JSONObject): DashboardUser = DashboardUser(
    id = obj.optInt("id").takeIf { obj.has("id") },
    nickname = obj.stringOrEmpty("nickname", "username", "name"),
    role = obj.stringOrEmpty("role"),
    department = obj.stringOrEmpty("department"),
)

internal fun parseDashboardPayload(body: JSONObject): DashboardPayload {
    val tilesNode = body.objectOrNull("Плитки")
    val chartsNode = body.objectOrNull("Графики")
    val tablesNode = body.objectOrNull("Таблицы")
    val tiles = parseTiles(tilesNode)
    return DashboardPayload(
        department = body.stringOrEmpty("department", "подразделение"),
        kpiCount = body.optInt("kpi_count", tiles.size),
        tiles = tiles,
        charts = parseCharts(chartsNode, tiles),
        tables = parseTables(tablesNode),
        rawSummary = summarizeTopLevel(body),
    )
}

private fun parseTiles(node: JSONObject?): List<KpiTile> {
    if (node == null) return emptyList()
    val array = node.arrayOrNull("items")
        ?: node.arrayOrNull("data")
        ?: node.arrayOrNull("tiles")
        ?: return emptyList()
    return array.objects().mapIndexed { index, item ->
        val id = item.stringOrEmpty("kpi_id", "id", "code").ifBlank { "KPI-${index + 1}" }
        val title = item.stringOrEmpty("title", "name", "Наименование").ifBlank { id }
        KpiTile(
            id = id,
            badge = item.stringOrEmpty("badge", "kpi_id", "id").ifBlank { id },
            title = title,
            period = item.stringOrEmpty("period", "pf_period", "month_name", "Период"),
            fact = formatMetric(item.opt("display_fact") ?: item.opt("fact") ?: item.opt("Факт")),
            plan = formatMetric(item.opt("display_plan") ?: item.opt("plan") ?: item.opt("План")),
            kpiPercent = formatMetric(item.opt("kpi_pct") ?: item.opt("kpi_percent") ?: item.opt("KPI")),
            units = item.stringOrEmpty("units", "unit", "Ед. изм."),
            rag = item.stringOrEmpty("rag", "status", "color").lowercase(Locale.ROOT),
            hasData = !item.has("has_data") || item.optBoolean("has_data", true),
        )
    }
}

private fun parseCharts(node: JSONObject?, tiles: List<KpiTile>): List<ChartBlock> {
    val lineSeries = mutableListOf<ChartSeries>()
    val barPoints = mutableListOf<ChartPoint>()
    if (node != null) {
        node.keyList().forEach { key ->
            val chart = node.optJSONObject(key) ?: return@forEach
            when (classifyChartType(chart.stringOrEmpty("chart_type", "chartType").ifBlank { key })) {
                ChartType.Line -> lineSeries += lineSeriesFromFrontendChart(key, chart)
                ChartType.Bar -> barPoints += barPointsFromFrontendChart(key, chart)
                ChartType.Donut -> Unit
            }
        }
    }

    val donutPoints = tiles.mapNotNull { tile ->
        tile.kpiPercent.replace("%", "").replace(",", ".").toDoubleOrNull()?.let {
            ChartPoint(tile.title.ifBlank { tile.badge }, it)
        }
    }
    val fallbackLine = if (lineSeries.isEmpty() && donutPoints.isNotEmpty()) {
        listOf(ChartSeries("KPI", donutPoints.take(12)))
    } else {
        lineSeries
    }
    val fallbackBar = if (barPoints.isEmpty()) donutPoints.take(10) else barPoints

    return listOf(
        ChartBlock(
            title = "Линейный график",
            type = ChartType.Line,
            points = fallbackLine.firstOrNull()?.points.orEmpty(),
            series = fallbackLine.take(8),
        ),
        ChartBlock(
            title = "Столбчатый график",
            type = ChartType.Bar,
            points = fallbackBar.take(12),
        ),
        ChartBlock(
            title = "Круговая диаграмма",
            type = ChartType.Donut,
            points = donutPoints.take(10),
        ),
    )
}

private fun lineSeriesFromFrontendChart(key: String, chart: JSONObject): List<ChartSeries> {
    val seriesList = chart.arrayOrNull("series") ?: return emptyList()
    return buildList {
        seriesList.objects().forEach { series ->
            val title = series.stringOrEmpty("name", "title", "kpi_id", "option_label").ifBlank { key }
            val lineSeries = series.arrayOrNull("line_series")
            if (lineSeries != null && lineSeries.length() > 0) {
                lineSeries.objects().forEachIndexed { lineIndex, line ->
                    val points = line.arrayOrNull("data")?.numbers()?.mapIndexed { index, value ->
                        ChartPoint(label = monthShort(index + 1), value = value)
                    }.orEmpty()
                    if (points.isNotEmpty()) {
                        add(ChartSeries(line.stringOrEmpty("name", "legend_label").ifBlank { "$title ${lineIndex + 1}" }, points))
                    }
                }
                return@forEach
            }
            val pointsArray = series.arrayOrNull("points") ?: return@forEach
            val sorted = pointsArray.objects().sortedBy { it.optInt("month", 0) }
            val points = sorted.mapIndexedNotNull { index, point ->
                val month = point.optInt("month", index + 1)
                val label = point.stringOrEmpty("month_name", "label", "period").ifBlank { monthShort(month) }
                val fact = point.doubleOrNull("fact", "value", "amount", "kpi_pct")
                fact?.let { ChartPoint(label.replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru-RU")) }, it) }
            }
            if (points.isNotEmpty()) add(ChartSeries(title, points))
            val planPoints = sorted.mapIndexedNotNull { index, point ->
                point.doubleOrNull("plan")?.let { ChartPoint(monthShort(point.optInt("month", index + 1)), it) }
            }
            if (planPoints.isNotEmpty()) add(ChartSeries("$title план", planPoints))
        }
    }
}

private fun barPointsFromFrontendChart(key: String, chart: JSONObject): List<ChartPoint> {
    val seriesList = chart.arrayOrNull("series") ?: return emptyList()
    return buildList {
        seriesList.objects().forEach { series ->
            val categories = series.arrayOrNull("categories")?.strings().orEmpty()
            val explicitFact = series.arrayOrNull("fact")?.numbers().orEmpty()
            val explicitPlan = series.arrayOrNull("plan")?.numbers().orEmpty()
            if (categories.isNotEmpty() && (explicitFact.isNotEmpty() || explicitPlan.isNotEmpty())) {
                val maxCount = maxOf(categories.size, explicitFact.size, explicitPlan.size)
                repeat(maxCount) { index ->
                    val label = categories.getOrNull(index) ?: "${index + 1}"
                    val fact = explicitFact.getOrNull(index)
                    val plan = explicitPlan.getOrNull(index)
                    val value = fact ?: plan
                    if (value != null) add(ChartPoint(label, value, plan))
                }
                return@forEach
            }
            val points = series.arrayOrNull("points")?.objects().orEmpty().sortedBy { it.optInt("quarter", 0) }
            points.forEachIndexed { index, point ->
                val label = point.stringOrEmpty("name", "label").ifBlank {
                    quarterLabel(point.optInt("quarter", index + 1))
                }
                val fact = point.doubleOrNull("fact", "value", "amount")
                val plan = point.doubleOrNull("plan")
                val value = fact ?: plan
                if (value != null) add(ChartPoint(label, value, plan))
            }
        }
    }
}

private fun classifyChartType(value: String): ChartType {
    val normalized = value.lowercase(Locale.ROOT)
    return when {
        "donut" in normalized || "pie" in normalized || "доля" in normalized -> ChartType.Donut
        "column" in normalized || "bar" in normalized || "waterfall" in normalized || "quarter" in normalized -> ChartType.Bar
        else -> ChartType.Line
    }
}

private fun parseTables(node: JSONObject?): List<TableBlock> {
    if (node == null) return emptyList()
    return buildList {
        node.keyList().forEach { key ->
            collectTableBlocks(key, node.opt(key), this)
        }
    }.take(16)
}

private fun collectTableBlocks(title: String, value: Any?, out: MutableList<TableBlock>) {
    when (value) {
        is JSONArray -> tableFromArray(title, value)?.let(out::add)
        is JSONObject -> {
            val direct = value.arrayOrNull("items") ?: value.arrayOrNull("rows") ?: value.arrayOrNull("data")
            if (direct != null) {
                tableFromArray(title, direct)?.let(out::add)
                return
            }
            value.keyList().forEach { nestedKey ->
                collectTableBlocks("$title / $nestedKey", value.opt(nestedKey), out)
            }
        }
    }
}

private fun tableFromArray(title: String, rowsArray: JSONArray): TableBlock? {
    val objects = rowsArray.objects()
    if (objects.isEmpty()) return null
    val headers = objects.flatMap { it.keyList() }.distinct().take(12)
    val rows = objects.take(50).map { row ->
        headers.map { header -> formatMetric(row.opt(header)) }
    }
    return TableBlock(title = title, headers = headers, rows = rows)
}

private fun summarizeTopLevel(body: JSONObject): String {
    return body.keyList().joinToString(", ") { key ->
        val value = body.opt(key)
        when (value) {
            is JSONObject -> "$key: ${value.keyList().size} блоков"
            is JSONArray -> "$key: ${value.length()} элементов"
            else -> "$key: ${formatMetric(value)}"
        }
    }
}

internal fun formatMetric(value: Any?): String {
    if (value == null || value == JSONObject.NULL) return "—"
    return when (value) {
        is Double -> formatNumber(value)
        is Float -> formatNumber(value.toDouble())
        is Number -> formatNumber(value.toDouble())
        else -> {
            val text = value.toString().trim()
            val numeric = text
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".")
                .toDoubleOrNull()
            if (numeric != null && abs(numeric) >= 1_000_000.0) formatNumber(numeric) else text.ifBlank { "—" }
        }
    }
}

internal fun formatCompactNumber(value: Double): String = formatNumber(value)

private fun formatNumber(value: Double): String {
    val absolute = abs(value)
    return when {
        absolute >= 1_000_000_000.0 -> "${formatScaled(value / 1_000_000_000.0)} млрд."
        absolute >= 1_000_000.0 -> "${formatScaled(value / 1_000_000.0)} млн."
        value % 1.0 == 0.0 -> value.roundToLong().toString()
        else -> String.format(Locale.forLanguageTag("ru-RU"), "%.2f", value)
    }
}

private fun formatScaled(value: Double): String {
    val locale = Locale.forLanguageTag("ru-RU")
    val roundedTenths = (value * 10.0).roundToLong() / 10.0
    return if (roundedTenths % 1.0 == 0.0) {
        roundedTenths.roundToLong().toString()
    } else {
        String.format(locale, "%.1f", roundedTenths)
    }
}

private fun JSONArray.numbers(): List<Double> = buildList {
    for (i in 0 until length()) {
        when (val value = opt(i)) {
            is Number -> add(value.toDouble())
            else -> value?.toString()?.replace(",", ".")?.toDoubleOrNull()?.let(::add)
        }
    }
}

private fun JSONArray.strings(): List<String> = buildList {
    for (i in 0 until length()) {
        val value = opt(i)?.toString()?.trim().orEmpty()
        if (value.isNotBlank()) add(value)
    }
}

private fun quarterLabel(quarter: Int): String = when (quarter) {
    1 -> "I кв."
    2 -> "II кв."
    3 -> "III кв."
    4 -> "IV кв."
    else -> "$quarter кв."
}

private fun monthShort(month: Int): String = when (month) {
    1 -> "Янв"
    2 -> "Фев"
    3 -> "Мар"
    4 -> "Апр"
    5 -> "Май"
    6 -> "Июн"
    7 -> "Июл"
    8 -> "Авг"
    9 -> "Сен"
    10 -> "Окт"
    11 -> "Ноя"
    12 -> "Дек"
    else -> month.toString()
}
