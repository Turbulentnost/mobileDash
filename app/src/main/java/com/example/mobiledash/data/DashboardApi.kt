package com.example.mobiledash.data

import com.example.mobiledash.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

class DashboardApi(
    private val baseUrl: String = AppConfig.API_BASE_URL,
) {
    suspend fun login(nickname: String, password: String): ApiResult<LoginSession> {
        val body = JSONObject()
            .put("nickname", nickname)
            .put("password", password)
        return request("/api/user/login/", method = "POST", body = body).mapJson { json ->
            LoginSession(
                token = json.stringOrEmpty("token"),
                user = parseUser(json.getJSONObject("user")),
            )
        }
    }

    suspend fun fetchLoginCandidates(): ApiResult<List<LoginCandidate>> {
        return request("/api/kpi/users/").mapJson { json ->
            json.optJSONArray("users").orEmptyObjects().map {
                LoginCandidate(
                    nickname = it.stringOrEmpty("nickname"),
                    department = it.stringOrEmpty("department"),
                )
            }.filter { it.nickname.isNotBlank() }
        }
    }

    suspend fun fetchDepartments(): ApiResult<List<String>> {
        return request("/api/user/departments/").mapJson { json ->
            json.optJSONArray("departments").orEmptyStrings()
        }
    }

    suspend fun submitRegistration(nickname: String, password: String, department: String): ApiResult<Unit> {
        val body = JSONObject()
            .put("nickname", nickname)
            .put("password", password)
            .put("department", department)
        return request("/api/user/access-requests/register/", method = "POST", body = body).mapJson { Unit }
    }

    suspend fun submitPasswordReset(nickname: String, password: String): ApiResult<Unit> {
        val body = JSONObject()
            .put("nickname", nickname)
            .put("password", password)
        return request("/api/user/access-requests/password-reset/", method = "POST", body = body).mapJson { Unit }
    }

    suspend fun fetchDashboard(
        token: String,
        department: String?,
        month: Int,
        year: Int,
        aggregation: String,
        chairmanFor: String? = null,
    ): ApiResult<DashboardPayload> {
        val params = mutableMapOf(
            "month" to month.toString(),
            "year" to year.toString(),
        )
        if (!department.isNullOrBlank()) params["department"] = department
        if (!chairmanFor.isNullOrBlank()) params["for"] = chairmanFor
        if (aggregation != "current") params["aggregation"] = aggregation
        return request("/api/kpi/", token = token, query = params).mapJson(::parseDashboardPayload)
    }

    suspend fun fetchImmediateSubordinates(token: String, department: String?): ApiResult<List<HierarchyNode>> {
        val params = mutableMapOf<String, String>()
        if (!department.isNullOrBlank()) params["department"] = department
        return request("/api/kpi/immediate-subordinates/", token = token, query = params).mapJson { json ->
            val children = json.optJSONArray("immediate_children") ?: json.optJSONArray("children") ?: JSONArray()
            children.toMixedList().mapNotNull { item ->
                when (item) {
                    is JSONObject -> HierarchyNode(
                        department = item.stringOrEmpty("department", "name", "title"),
                        count = item.optInt("count", 0),
                    )
                    is String -> HierarchyNode(item)
                    else -> null
                }
            }.filter { it.department.isNotBlank() }
        }
    }

    suspend fun fetchChairmanCatalog(token: String): ApiResult<List<ChairmanCatalogItem>> {
        return request("/api/kpi/chairman/for-catalog/", token = token).mapJson { json ->
            val array = json.optJSONArray("items")
                ?: json.optJSONArray("catalog")
                ?: json.optJSONArray("data")
                ?: JSONArray()
            array.objects().mapIndexed { index, item ->
                ChairmanCatalogItem(
                    id = item.stringOrEmpty("id", "for", "key").ifBlank { index.toString() },
                    title = item.stringOrEmpty("title", "name", "label").ifBlank { "Раздел ${index + 1}" },
                    subtitle = item.stringOrEmpty("department", "description", "subtitle"),
                )
            }
        }
    }

    suspend fun fetchAccessRequests(token: String): ApiResult<List<AccessRequestItem>> {
        return request("/api/user/access-requests/", token = token, query = mapOf("status" to "pending")).mapJson { json ->
            val array = json.optJSONArray("requests") ?: JSONArray()
            array.objects().mapNotNull {
                val id = it.optInt("id", -1)
                if (id < 0) null else AccessRequestItem(
                    id = id,
                    typeLabel = it.stringOrEmpty("request_type_label", "request_type"),
                    statusLabel = it.stringOrEmpty("status_label", "status"),
                    nickname = it.stringOrEmpty("nickname"),
                    department = it.stringOrEmpty("department"),
                    createdAt = it.stringOrEmpty("created_at"),
                    comment = it.stringOrEmpty("comment"),
                )
            }
        }
    }

    suspend fun approveAccessRequest(token: String, id: Int): ApiResult<Unit> {
        return request("/api/user/access-requests/$id/approve/", method = "POST", token = token, body = JSONObject()).mapJson { Unit }
    }

    suspend fun rejectAccessRequest(token: String, id: Int, comment: String): ApiResult<Unit> {
        return request(
            "/api/user/access-requests/$id/reject/",
            method = "POST",
            token = token,
            body = JSONObject().put("comment", comment),
        ).mapJson { Unit }
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        token: String? = null,
        query: Map<String, String> = emptyMap(),
        body: JSONObject? = null,
    ): ApiResult<JSONObject> = withContext(Dispatchers.IO) {
        val url = URL(buildUrl(path, query))
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", AppConfig.AUTH_SCHEME + token)
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            }.orEmpty()
            val json = if (text.isBlank()) JSONObject() else JSONObject(text)
            if (status in 200..299) {
                ApiResult.Success(json)
            } else {
                ApiResult.Failure(
                    message = json.stringOrEmpty("error", "message").ifBlank { "Ошибка запроса ($status)" },
                    unauthorized = status == 401,
                )
            }
        } catch (error: Exception) {
            ApiResult.Failure(error.message ?: "Ошибка сети")
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val normalizedBase = baseUrl.trimEnd('/')
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        if (query.isEmpty()) return normalizedBase + normalizedPath
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        return "$normalizedBase$normalizedPath?$queryString"
    }
}

private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

private fun JSONArray?.orEmptyObjects(): List<JSONObject> = this?.objects() ?: emptyList()

private fun JSONArray?.orEmptyStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val value = opt(i)?.toString()?.trim().orEmpty()
            if (value.isNotBlank()) add(value)
        }
    }
}

private fun JSONArray.toMixedList(): List<Any> = buildList {
    for (i in 0 until length()) opt(i)?.let(::add)
}

private inline fun <T> ApiResult<JSONObject>.mapJson(block: (JSONObject) -> T): ApiResult<T> = when (this) {
    is ApiResult.Success -> try {
        ApiResult.Success(block(value))
    } catch (error: Exception) {
        ApiResult.Failure(error.message ?: "Ошибка разбора ответа")
    }
    is ApiResult.Failure -> this
}
