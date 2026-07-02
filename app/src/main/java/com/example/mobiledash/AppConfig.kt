package com.example.mobiledash

object AppConfig {
    const val API_BASE_URL = "http://193.105.37.142:8000"
    const val API_FALLBACK_BASE_URL = "http://192.168.1.157:8000"
    val API_BASE_URLS = listOf(API_BASE_URL, API_FALLBACK_BASE_URL)
    const val GITHUB_RELEASES_API = "https://api.github.com/repos/Turbulentnost/mobileDash/releases/latest"
    const val AUTH_SCHEME = "Bearer "
}
