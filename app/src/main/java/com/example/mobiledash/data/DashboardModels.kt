package com.example.mobiledash.data

data class DashboardUser(
    val id: Int? = null,
    val nickname: String,
    val role: String = "",
    val department: String = "",
) {
    val isAdmin: Boolean
        get() = nickname.equals("User1", ignoreCase = true) ||
            role.contains("admin", ignoreCase = true)
}

data class LoginSession(
    val token: String,
    val user: DashboardUser,
)

data class LoginCandidate(
    val nickname: String,
    val department: String,
)

data class KpiTile(
    val id: String,
    val badge: String,
    val title: String,
    val period: String,
    val fact: String,
    val plan: String,
    val kpiPercent: String,
    val units: String,
    val rag: String,
    val hasData: Boolean,
)

data class ChartPoint(
    val label: String,
    val value: Double,
    val plan: Double? = null,
    val color: String? = null,
    val percentLabel: String = "",
)

data class ChartSeries(
    val name: String,
    val points: List<ChartPoint>,
    val color: String? = null,
    val valueRole: String = "",
    val dashed: Boolean = false,
)

data class ChartBlock(
    val title: String,
    val type: ChartType,
    val points: List<ChartPoint>,
    val series: List<ChartSeries> = emptyList(),
)

enum class ChartType {
    Line,
    Bar,
    Donut,
}

data class TableBlock(
    val key: String,
    val title: String,
    val description: String = "",
    val headers: List<String>,
    val rows: List<List<String>>,
)

data class HierarchyNode(
    val department: String,
    val count: Int = 0,
)

data class ChairmanCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
)

data class AccessRequestItem(
    val id: Int,
    val typeLabel: String,
    val statusLabel: String,
    val nickname: String,
    val department: String,
    val createdAt: String,
    val comment: String = "",
)

data class DashboardPayload(
    val department: String,
    val kpiCount: Int,
    val tiles: List<KpiTile>,
    val charts: List<ChartBlock>,
    val tables: List<TableBlock>,
    val rawSummary: String,
)

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String, val unauthorized: Boolean = false) : ApiResult<Nothing>
}
