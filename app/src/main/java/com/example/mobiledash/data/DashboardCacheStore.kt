package com.example.mobiledash.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class DashboardCacheStore(context: Context) {
    private val prefs = context.getSharedPreferences("mobile_dash_payload_cache", Context.MODE_PRIVATE)

    suspend fun readDashboard(key: String): DashboardPayload? = withContext(Dispatchers.IO) {
        val raw = prefs.getString(cacheKey(key), null)?.takeIf { it.isNotBlank() } ?: return@withContext null
        runCatching { parseCachedDashboardPayload(JSONObject(raw)) }.getOrNull()
    }

    suspend fun saveDashboard(key: String, payload: DashboardPayload) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(cacheKey(key), payload.toJson().toString())
            .putLong(cacheUpdatedKey(key), System.currentTimeMillis())
            .apply()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun cacheKey(key: String): String = "dashboard:${key.safeKey()}"

    private fun cacheUpdatedKey(key: String): String = "dashboard_updated:${key.safeKey()}"
}

internal fun dashboardCacheKey(
    nickname: String,
    department: String?,
    month: Int,
    year: Int,
    aggregation: String,
    chairmanFor: String?,
): String {
    return listOf(
        nickname,
        department.orEmpty(),
        month.toString(),
        year.toString(),
        aggregation,
        chairmanFor.orEmpty(),
    ).joinToString("|")
}

private fun String.safeKey(): String = URLEncoder.encode(this, "UTF-8")

private fun DashboardPayload.toJson(): JSONObject = JSONObject()
    .put("department", department)
    .put("kpiCount", kpiCount)
    .put("tiles", JSONArray().also { array -> tiles.forEach { array.put(it.toJson()) } })
    .put("charts", JSONArray().also { array -> charts.forEach { array.put(it.toJson()) } })
    .put("tables", JSONArray().also { array -> tables.forEach { array.put(it.toJson()) } })
    .put("rawSummary", rawSummary)

private fun KpiTile.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("badge", badge)
    .put("title", title)
    .put("period", period)
    .put("fact", fact)
    .put("plan", plan)
    .put("expected", expected)
    .put("kpiPercent", kpiPercent)
    .put("units", units)
    .put("rag", rag)
    .put("hasData", hasData)
    .put("goal", goal)
    .put("formula", formula)
    .put("source", source)
    .put("description", description)
    .put("cacheRefreshStatus", cacheRefreshStatus)

private fun ChartBlock.toJson(): JSONObject = JSONObject()
    .put("title", title)
    .put("type", type.name)
    .put("points", JSONArray().also { array -> points.forEach { array.put(it.toJson()) } })
    .put("series", JSONArray().also { array -> series.forEach { array.put(it.toJson()) } })

private fun ChartPoint.toJson(): JSONObject = JSONObject()
    .put("label", label)
    .put("value", value)
    .put("plan", plan)
    .put("color", color)
    .put("percentLabel", percentLabel)
    .put("detailLabel", detailLabel)
    .put("detailValue", detailValue)

private fun ChartSeries.toJson(): JSONObject = JSONObject()
    .put("name", name)
    .put("points", JSONArray().also { array -> points.forEach { array.put(it.toJson()) } })
    .put("color", color)
    .put("valueRole", valueRole)
    .put("dashed", dashed)

private fun TableBlock.toJson(): JSONObject = JSONObject()
    .put("key", key)
    .put("title", title)
    .put("description", description)
    .put("headers", JSONArray().also { array -> headers.forEach { array.put(it) } })
    .put("rows", JSONArray().also { rowsArray ->
        rows.forEach { row ->
            rowsArray.put(JSONArray().also { columns -> row.forEach { columns.put(it) } })
        }
    })
    .put("cacheRefreshStatus", cacheRefreshStatus)

private fun parseCachedDashboardPayload(json: JSONObject): DashboardPayload = DashboardPayload(
    department = json.optString("department"),
    kpiCount = json.optInt("kpiCount"),
    tiles = json.optJSONArray("tiles").orEmptyObjects().map(::parseCachedTile),
    charts = json.optJSONArray("charts").orEmptyObjects().map(::parseCachedChart),
    tables = json.optJSONArray("tables").orEmptyObjects().map(::parseCachedTable),
    rawSummary = json.optString("rawSummary"),
)

private fun parseCachedTile(json: JSONObject): KpiTile = KpiTile(
    id = json.optString("id"),
    badge = json.optString("badge"),
    title = json.optString("title"),
    period = json.optString("period"),
    fact = json.optString("fact"),
    plan = json.optString("plan"),
    expected = json.optString("expected"),
    kpiPercent = json.optString("kpiPercent"),
    units = json.optString("units"),
    rag = json.optString("rag"),
    hasData = json.optBoolean("hasData", true),
    goal = json.optString("goal"),
    formula = json.optString("formula"),
    source = json.optString("source"),
    description = json.optString("description"),
    cacheRefreshStatus = json.optString("cacheRefreshStatus"),
)

private fun parseCachedChart(json: JSONObject): ChartBlock = ChartBlock(
    title = json.optString("title"),
    type = runCatching { ChartType.valueOf(json.optString("type")) }.getOrDefault(ChartType.Line),
    points = json.optJSONArray("points").orEmptyObjects().map(::parseCachedPoint),
    series = json.optJSONArray("series").orEmptyObjects().map(::parseCachedSeries),
)

private fun parseCachedPoint(json: JSONObject): ChartPoint = ChartPoint(
    label = json.optString("label"),
    value = json.optDouble("value", 0.0),
    plan = json.optNullableDouble("plan"),
    color = json.optNullableString("color"),
    percentLabel = json.optString("percentLabel"),
    detailLabel = json.optString("detailLabel"),
    detailValue = json.optString("detailValue"),
)

private fun parseCachedSeries(json: JSONObject): ChartSeries = ChartSeries(
    name = json.optString("name"),
    points = json.optJSONArray("points").orEmptyObjects().map(::parseCachedPoint),
    color = json.optNullableString("color"),
    valueRole = json.optString("valueRole"),
    dashed = json.optBoolean("dashed", false),
)

private fun parseCachedTable(json: JSONObject): TableBlock = TableBlock(
    key = json.optString("key"),
    title = json.optString("title"),
    description = json.optString("description"),
    headers = json.optJSONArray("headers").orEmptyStrings(),
    rows = json.optJSONArray("rows").orEmptyRows(),
    cacheRefreshStatus = json.optString("cacheRefreshStatus"),
)

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
}

private fun JSONObject.optNullableDouble(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key)
}

private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) optJSONObject(i)?.let(::add)
    }
}

private fun JSONArray?.orEmptyStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) add(optString(i))
    }
}

private fun JSONArray?.orEmptyRows(): List<List<String>> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val row = optJSONArray(i) ?: continue
            add(row.orEmptyStrings())
        }
    }
}
