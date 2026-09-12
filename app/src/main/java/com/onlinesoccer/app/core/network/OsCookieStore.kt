package com.onlinesoccer.app.core.network

import com.onlinesoccer.app.core.storage.TokenStorage
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cookie-Haushalt für die Server-Session.
 *
 * Hält die Cookies `os` (flüchtig) und `lc` (persistent) spalten-übergreifend fest
 * und persistiert beide verschlüsselt über [TokenStorage], damit ein App-Neustart
 * weiterhin angemeldet ist (stiller Relogin über `lc`).
 */
@Singleton
class OsCookieStore @Inject constructor(
    private val storage: TokenStorage,
) : CookieJar {

    private val cookies = CopyOnWriteArrayList<Cookie>()

    init {
        restorePersisted()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return cookies.filter { cookie ->
            cookie.matches(url) && cookie.expiresAt > now
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookiesFromServer: List<Cookie>) {
        cookiesFromServer.forEach { incoming ->
            cookies.removeAll { it.name == incoming.name && it.matches(url) }
            cookies.add(incoming)
        }
        persist()
    }

    fun hasSession(): Boolean = !storage.lc.isNullOrEmpty()

    fun clear() {
        cookies.clear()
        storage.clearSession()
    }

    private fun persist() {
        val os = cookies.firstOrNull { it.name == COOKIE_OS }
        val lc = cookies.firstOrNull { it.name == COOKIE_LC }
        storage.os = os?.value
        storage.lc = lc?.value
    }

    private fun restorePersisted() {
        storage.os?.let { cookies.add(buildCookie(COOKIE_OS, it)) }
        storage.lc?.let { cookies.add(buildCookie(COOKIE_LC, it)) }
    }

    private fun buildCookie(name: String, value: String): Cookie = Cookie.Builder()
        .domain(OsApi.HOST)
        .path("/")
        .name(name)
        .value(value)
        .expiresAt(System.currentTimeMillis() + MAX_AGE_MILLIS)
        .build()

    private companion object {
        const val COOKIE_OS = "os"
        const val COOKIE_LC = "lc"
        const val MAX_AGE_MILLIS = 2L * 365L * 24L * 60L * 60L * 1000L
    }
}