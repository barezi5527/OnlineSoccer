package com.onlinesoccer.app.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Kennzeichnet den Client für schreibende Session-Toggles (Teamwechsel).
 *
 * Eigener Qualifier nötig, weil rund 20 Repositories den Standard-Client
 * ohne Qualifier injizieren — ein zweiter `OkHttpClient`-Provider ohne
 * Unterscheidung wäre ein Hilt-Duplicate-Binding.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ToggleClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Standard-Client für alle lesenden Abrufe — **ohne** HTTP-Cache.
     *
     * ⚠️ Es gab hier bis T38 einen 10-MB-Diskcache. Der Server sendet auf **allen**
     * teamunabhängigen URLs (`haupt.php`, `showteam.php?s=N`, `zugabgabe.php`, `ka.php`,
     * auch `changetosecond`) durchgehend `Cache-Control: no-store, no-cache,
     * must-revalidate` + `Pragma: no-cache` — live an allen vier geprüft (T38). Ein
     * OkHttp-Cache kann solche Antworten weder speichern noch ausliefern, der Cache
     * brachte also **keinen** Nutzen, legte aber Sitzungsdaten unverschlüsselt auf
     * Platte und war bei gerade diesen URLs die einzige Stelle, die Team 1 für Team 2
     * ausliefern **könnte**, falls sich die Header je ändern. Jetzt: keine Cache-
     * Konfiguration, damit die Frage nicht erneut aufkommt.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(cookieStore: OsCookieStore): OkHttpClient =
        buildOkHttpClient(cookieStore = cookieStore)

    /**
     * Client für den Teamwechsel-Toggle.
     *
     * Zwei Abweichungen gegenüber dem Standard-Client, beide wegen Regel 5:
     * `retryOnConnectionFailure = false` — OkHttp würde den GET bei einem
     * Verbindungsabbruch ein zweites Mal schicken, das ist ein **zweiter**
     * Toggle, also ein stiller Rückwechsel. Und **kein Cache**: die Antwort
     * von `haupt.php` ist der einzige Beleg für den neuen Teamzustand, sie
     * darf nicht aus einem Cache kommen.
     */
    @Provides
    @Singleton
    @ToggleClient
    fun provideToggleClient(cookieStore: OsCookieStore): OkHttpClient =
        buildOkHttpClient(
            cookieStore = cookieStore,
            retryOnConnectionFailure = false,
        )

    private fun buildOkHttpClient(
        cookieStore: OsCookieStore,
        retryOnConnectionFailure: Boolean = true,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieStore)
            .retryOnConnectionFailure(retryOnConnectionFailure)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", OsApi.USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
}