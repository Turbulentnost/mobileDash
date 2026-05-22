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

    val donutPoints = tiles.map { tile ->
        val percent = tile.kpiPercent.percentNumberOrNull()
        ChartPoint(
            label = tile.title.ifBlank { tile.badge },
            value = percent?.coerceAtLeast(0.0) ?: 0.0,
            color = tile.rag,
            percentLabel = if (percent == null) "—" else tile.kpiPercent,
        )
    }
    return buildList {
        add(ChartBlock(
            title = "Линейный график",
            type = ChartType.Line,
            points = lineSeries.firstOrNull()?.points.orEmpty(),
            series = lineSeries.take(8),
        ))
        if (barPoints.isNotEmpty()) {
            add(ChartBlock(
                title = "Столбчатый график",
                type = ChartType.Bar,
                points = barPoints.take(12),
            ))
        }
        add(ChartBlock(
            title = "Круговые диаграммы",
            type = ChartType.Donut,
            points = donutPoints,
        ))
    }
}

private fun lineSeriesFromFrontendChart(key: String, chart: JSONObject): List<ChartSeries> {
    val seriesList = chart.arrayOrNull("series") ?: return emptyList()
    return buildList {
        seriesList.objects().forEach { series ->
            val title = series.stringOrEmpty("name", "title", "kpi_id", "option_label").ifBlank { key }
            val basePoints = series.arrayOrNull("points")?.objects().orEmpty().sortedBy { point ->
                point.optInt("year", 0) * 100 + point.optInt("month", 0)
            }
            val lineSeries = series.arrayOrNull("line_series")
            if (lineSeries != null && lineSeries.length() > 0) {
                lineSeries.objects().forEachIndexed { lineIndex, line ->
                    val values = line.arrayOrNull("data")?.nullableNumbers().orEmpty()
                    val points = values.mapIndexedNotNull { index, value ->
                        value?.let {
                            val source = basePoints.getOrNull(index)
                            ChartPoint(
                                label = source?.monthLabel() ?: monthShort(index + 1),
                                value = it,
                            )
                        }
                    }.orEmpty()
                    if (points.isNotEmpty()) {
                        val role = line.stringOrEmpty("value_role", "valueRole")
                        add(
                            ChartSeries(
                                name = line.stringOrEmpty("name", "legend_label").ifBlank { "$title ${lineIndex + 1}" },
                                points = points,
                                color = line.stringOrEmpty("color").takeIf { it.isNotBlank() },
                                valueRole = role,
                                dashed = role.equals("plan", ignoreCase = true) ||
                                    line.stringOrEmpty("dashStyle", "dash_style").isNotBlank(),
                            ),
                        )
                    }
                }
                return@forEach
            }
            val sorted = basePoints
            val points = sorted.mapIndexedNotNull { index, point ->
                val fact = point.doubleOrNull("fact", "value", "amount", "kpi_pct")
                fact?.let { ChartPoint(point.monthLabel(index + 1), it) }
            }
            if (points.isNotEmpty()) {
                add(ChartSeries(name = title, points = points, color = "#2b5ca6", valueRole = "fact"))
            }
            val planPoints = sorted.mapIndexedNotNull { index, point ->
                point.doubleOrNull("plan")?.let { ChartPoint(point.monthLabel(index + 1), it) }
            }
            if (planPoints.isNotEmpty()) {
                add(ChartSeries(name = "$title план", points = planPoints, color = "#c8d6ee", valueRole = "plan", dashed = true))
            }
        }
    }
}

private fun barPointsFromFrontendChart(key: String, chart: JSONObject): List<ChartPoint> {
    val seriesList = chart.arrayOrNull("series") ?: return emptyList()
    return buildList {
        seriesList.objects().forEach { series ->
            val categories = series.arrayOrNull("categories")?.strings().orEmpty()
            val explicitFact = series.arrayOrNull("fact")?.nullableNumbers().orEmpty()
            val explicitPlan = series.arrayOrNull("plan")?.nullableNumbers().orEmpty()
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

private fun JSONObject.monthLabel(fallbackMonth: Int = optInt("month", 1)): String {
    val label = stringOrEmpty("month_name", "label", "period")
    return label.ifBlank { monthShort(optInt("month", fallbackMonth)) }
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru-RU")) }
}

private fun classifyChartType(value: String): ChartType {
    val normalized = value.lowercase(Locale.ROOT)
    return when {
        "line" in normalized || "combo" in normalized -> ChartType.Line
        "donut" in normalized || "pie" in normalized || "доля" in normalized -> ChartType.Donut
        "column" in normalized || "bar" in normalized || "waterfall" in normalized || "quarter" in normalized -> ChartType.Bar
        else -> ChartType.Line
    }
}

private fun parseTables(node: JSONObject?): List<TableBlock> {
    if (node == null) return emptyList()
    return buildList {
        node.keyList().forEach { key ->
            tableFromTopLevel(key, node.opt(key))?.let(::add)
        }
    }
}

private fun tableFromTopLevel(key: String, value: Any?): TableBlock? {
    val tableObject = value as? JSONObject
    val rowsArray = when (value) {
        is JSONArray -> value
        is JSONObject -> value.arrayOrNull("rows") ?: value.arrayOrNull("items") ?: value.arrayOrNull("data")
        else -> null
    } ?: return null
    val rows = rowsArray.objects()
    if (rows.isEmpty()) return null

    val spec = tableSpecFor(key)
    val headers = spec?.headers
        ?: tableObject?.arrayOrNull("columns")?.strings()?.takeIf { it.isNotEmpty() }
        ?: rows.flatMap { it.keyList() }.distinct().take(12)
    val rowValues = rows.take(200).map { row ->
        if (spec != null) {
            spec.fields.map { field -> formatTableField(row, field) }
        } else {
            headers.map { header -> formatMetric(row.opt(header)) }
        }
    }
    return TableBlock(
        key = key,
        title = tableObject?.stringOrEmpty("name", "title").orEmpty().ifBlank { key },
        description = tableObject?.stringOrEmpty("description").orEmpty(),
        headers = headers,
        rows = rowValues,
    )
}

private data class TableSpec(
    val headers: List<String>,
    val fields: List<TableField>,
)

private data class TableField(
    val keys: List<String>,
    val formatter: TableValueFormatter = TableValueFormatter.Default,
    val fallback: String = "",
)

private enum class TableValueFormatter {
    Default,
    Money,
    Date,
    Percent,
    Posted,
}

private fun tableSpecFor(key: String): TableSpec? {
    val normalized = key.trim().uppercase(Locale.ROOT)
    return when {
        normalized == "LOG-T-CLAIMS" -> tableSpec(
            "Номер" to field("code"),
            "Дата" to field("date_reg", formatter = TableValueFormatter.Date),
            "Поставщик" to field("supplier"),
            "Номер заказа поставщика" to field("supplier_order_number", "order_num"),
            "Статус" to field("status"),
            "Состояние проведения" to field("posted", formatter = TableValueFormatter.Posted),
            "Номенклатура" to field("nomenclature"),
            "Категория по причине" to field("reason_category"),
            "Возможность устранения" to field("resolution"),
            "Расчетное кол-во брака" to field("calculated_defect_qty"),
        )
        normalized == "PD-T-PROD-CLAIMS" -> tableSpec(
            "Номер" to field("code"),
            "Дата" to field("date_reg", formatter = TableValueFormatter.Date),
            "Подразделение-виновник" to field("order_dept", "culprit_dept"),
            "Статус" to field("status"),
            "Номенклатура" to field("nomenclature"),
            "Описание" to field("description"),
            "Расчетное кол-во брака" to field("calculated_defect_qty"),
        )
        normalized == "KD-T-CLAIMS" -> tableSpec(
            "Код" to field("code"),
            "Наименование" to field("name"),
            "Партнер/Клиент" to field("partner"),
            "Дата обращения" to field("date_reg", formatter = TableValueFormatter.Date),
            "Дата окончания (план)" to field("date_plan", formatter = TableValueFormatter.Date),
            "Заказ клиента" to field("order_num"),
            "Подразделение заказа" to field("order_dept"),
            "Номенклатура" to field("nomenclature"),
            "Описание претензии" to field("description"),
            "Статус" to field("status"),
            "Сумма документа заказа, руб." to field("order_sum", formatter = TableValueFormatter.Money),
        )
        normalized == "KD-T-LAWSUITS" || normalized == "KD-T-COURTS" || normalized == "KD-T-SUITS" -> tableSpec(
            "Тип документа" to field("doc_type", "document_type", "documentType"),
            "Контрагент" to field("counterparty", "partner", "contragent"),
            "Предмет спора" to field("subject", "dispute_subject", "dispute", "topic"),
            "Роль ГК в споре" to field("gc_role", "gk_role", "role", "company_role"),
            "Юр. лицо" to field("gc_entity", "legal_entity", "entity", "company", "jur_entity", "ur_entity"),
            "Подразделение" to field("initiator_dept", "department", "subdivision", "unit", "division"),
            "Сумма требований, руб." to field("claim_amount", "amount", "sum", "requirement_sum", "requirements_sum", formatter = TableValueFormatter.Money),
        )
        normalized == "KD-T-OVERDUE" -> tableSpec(
            "№ Заказа клиента" to field("order_num", "order_number"),
            "Контрагент" to field("counterparty", "partner_name"),
            "Просрочка, дн." to field("days_overdue"),
            "Ликвидированное подразделение" to field("liquidated_dept_name", "Ликвидированное подразделение"),
            "Причина" to field("reason"),
            "Действие" to field("action"),
            "Сумма, руб" to field("amount", formatter = TableValueFormatter.Money),
        )
        normalized == "LOG-T-SUPPLIER-DZ" -> tableSpec(
            "№ объекта расчетов" to field("order_num", "order_key"),
            "Дата" to field("order_date", formatter = TableValueFormatter.Date),
            "Объект расчетов" to field("object_name"),
            "Поставщик" to field("supplier"),
            "Сумма" to field("amount", formatter = TableValueFormatter.Money),
        )
        normalized == "TD-T-M1-DEVIATIONS" || normalized == "TD-T-Q1-DEVIATIONS" -> technicalProjectTableSpec()
        normalized == "OD-T-Q1-DEVIATIONS" ||
            normalized == "PD-T-Q1-DEVIATIONS" ||
            normalized == "GK-T-M1-DEVIATIONS" ||
            normalized == "METD-T-Q1-DEVIATIONS" -> projectDeviationTableSpec()
        normalized == "PD-T-Q3-IMPROVEMENTS" -> tableSpec(
            "№ 1С" to field("project_code", "number"),
            "Название" to field("project_name"),
            "РП" to field("project_manager"),
            "Куратор" to field("kurator"),
            "Сроки" to field("timeline"),
            "Статус" to field("status"),
            "Прогресс" to field("progress_pct", formatter = TableValueFormatter.Percent),
        )
        else -> null
    }
}

private fun technicalProjectTableSpec(): TableSpec = tableSpec(
    "Название проекта" to field("project_name"),
    "Руководитель проекта" to field("project_manager"),
    "Название вехи" to field("milestone_name"),
    "Плановая дата вехи" to field("milestone_planned_finish_date", formatter = TableValueFormatter.Date),
    "Дата отклонения" to field("deviation_date", formatter = TableValueFormatter.Date),
    "Дней отклонения" to field("delay_days"),
    "Процент выполнения" to field("percent_complete", formatter = TableValueFormatter.Percent),
)

private fun projectDeviationTableSpec(): TableSpec = tableSpec(
    "№ 1С" to field("project_code", "number"),
    "Название" to field("project_name"),
    "РП" to field("project_manager"),
    "Сроки" to field("timeline"),
    "Отклонение" to field("deviation", "delay_days"),
    "Статус" to field("status"),
    "Прогресс" to field("progress_pct", formatter = TableValueFormatter.Percent),
)

private fun tableSpec(vararg pairs: Pair<String, TableField>): TableSpec = TableSpec(
    headers = pairs.map { it.first },
    fields = pairs.map { it.second },
)

private fun field(
    vararg keys: String,
    formatter: TableValueFormatter = TableValueFormatter.Default,
    fallback: String = "",
): TableField = TableField(keys = keys.toList(), formatter = formatter, fallback = fallback)

private fun formatTableField(row: JSONObject, field: TableField): String {
    val raw = field.keys.firstNotNullOfOrNull { key ->
        if (row.has(key) && !row.isNull(key)) row.opt(key) else null
    }
    if (raw == null) return field.fallback.ifBlank { "—" }
    return when (field.formatter) {
        TableValueFormatter.Default -> formatMetric(raw)
        TableValueFormatter.Money -> formatMoney(raw)
        TableValueFormatter.Date -> formatDateValue(raw)
        TableValueFormatter.Percent -> formatPercentValue(raw)
        TableValueFormatter.Posted -> if (raw == true || raw.toString().equals("true", ignoreCase = true)) "Проведен" else "Не проведен"
    }
}

private fun formatMoney(value: Any?): String {
    val number = value.numericOrNull() ?: return formatMetric(value)
    return String.format(Locale.forLanguageTag("ru-RU"), "%,.2f", number)
}

private fun formatPercentValue(value: Any?): String {
    val number = value.numericOrNull() ?: return formatMetric(value)
    val percent = if (abs(number) <= 1.0) number * 100.0 else number
    val formatted = if (percent % 1.0 == 0.0) {
        percent.roundToLong().toString()
    } else {
        String.format(Locale.forLanguageTag("ru-RU"), "%.1f", percent)
    }
    return "$formatted%"
}

private fun formatDateValue(value: Any?): String {
    val text = value?.toString()?.trim().orEmpty()
    if (text.isBlank()) return "—"
    val match = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(text)
    return if (match != null) {
        val (year, month, day) = match.destructured
        "$day.$month.$year"
    } else {
        text
    }
}

private fun Any?.numericOrNull(): Double? {
    return when (this) {
        is Number -> toDouble()
        null, JSONObject.NULL -> null
        else -> toString()
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .toDoubleOrNull()
    }
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

private fun JSONArray.nullableNumbers(): List<Double?> = buildList {
    for (i in 0 until length()) {
        val value = opt(i)
        add(
            when (value) {
                is Number -> value.toDouble()
                null, JSONObject.NULL -> null
                else -> value.toString().replace(",", ".").toDoubleOrNull()
            },
        )
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

private fun String.percentNumberOrNull(): Double? {
    return replace("%", "")
        .replace("\u00A0", "")
        .replace(" ", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull()
}
