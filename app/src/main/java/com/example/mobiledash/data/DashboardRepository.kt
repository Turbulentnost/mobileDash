package com.example.mobiledash.data

class DashboardRepository(
    private val api: DashboardApi,
    private val sessionStore: SessionStore,
) {
    suspend fun restoreSession(): LoginSession? = sessionStore.readSession()

    suspend fun login(nickname: String, password: String, rememberSession: Boolean): ApiResult<LoginSession> {
        return when (val result = api.login(nickname, password)) {
            is ApiResult.Success -> {
                if (rememberSession) {
                    sessionStore.saveSession(result.value)
                } else {
                    sessionStore.clear()
                }
                result
            }
            is ApiResult.Failure -> result
        }
    }

    suspend fun logout() {
        sessionStore.clear()
    }

    suspend fun fetchLoginCandidates(): ApiResult<List<LoginCandidate>> = api.fetchLoginCandidates()

    suspend fun fetchDepartments(): ApiResult<List<String>> = api.fetchDepartments()

    suspend fun submitRegistration(nickname: String, password: String, department: String): ApiResult<Unit> {
        return api.submitRegistration(nickname, password, department)
    }

    suspend fun submitPasswordReset(nickname: String, password: String): ApiResult<Unit> {
        return api.submitPasswordReset(nickname, password)
    }

    suspend fun fetchDashboard(
        session: LoginSession,
        department: String?,
        month: Int,
        year: Int,
        aggregation: String,
        chairmanFor: String? = null,
    ): ApiResult<DashboardPayload> {
        val result = api.fetchDashboard(session.token, department, month, year, aggregation, chairmanFor)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }

    suspend fun fetchImmediateSubordinates(session: LoginSession, department: String?): ApiResult<List<HierarchyNode>> {
        val result = api.fetchImmediateSubordinates(session.token, department)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }

    suspend fun fetchChairmanCatalog(session: LoginSession): ApiResult<List<ChairmanCatalogItem>> {
        val result = api.fetchChairmanCatalog(session.token)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }

    suspend fun fetchAccessRequests(session: LoginSession): ApiResult<List<AccessRequestItem>> {
        val result = api.fetchAccessRequests(session.token)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }

    suspend fun approveAccessRequest(session: LoginSession, id: Int): ApiResult<Unit> {
        val result = api.approveAccessRequest(session.token, id)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }

    suspend fun rejectAccessRequest(session: LoginSession, id: Int, comment: String): ApiResult<Unit> {
        val result = api.rejectAccessRequest(session.token, id, comment)
        if (result is ApiResult.Failure && result.unauthorized) sessionStore.clear()
        return result
    }
}
