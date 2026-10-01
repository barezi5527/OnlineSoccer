package com.onlinesoccer.app.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.Cache
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

    @Provides
    @Singleton
    fun provideHttpCacheDir(@ApplicationContext context: Context): File =
        File(context.cacheDir, "http_cache")

    /**
     * Standard-Client für alle lesenden Abrufe (inkl. 10-MB-Diskcache).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(cookieStore: OsCookieStore, cacheDir: File): OkHttpClient =
        buildOkHttpClient(cookieStore = cookieStore, cacheDir = cacheDir)

    /**
     * Client für den Teamwechsel-Toggle.
     *
     * Zwei Abweichungen gegenüber dem Standard-Client, beide wegen Regel 5:
     * `retryOnConnectionFailure = false` — OkHttp würde den GET bei einem
     * Verbindungsabbruch ein zweites Mal schicken, das ist ein **zweiter**
     * Toggle, also ein stiller Rückwechsel. Und **kein Cache**: die Antwort
     * von `haupt.php` ist der einzige Beleg für den neuen Teamzustand, sie
     * darf nicht aus dem Diskcache kommen.
     */
    @Provides
    @Singleton
    @ToggleClient
    fun provideToggleClient(cookieStore: OsCookieStore): OkHttpClient =
        buildOkHttpClient(
            cookieStore = cookieStore,
            cacheDir = null,
            retryOnConnectionFailure = false,
        )

    private fun buildOkHttpClient(
        cookieStore: OsCookieStore,
        cacheDir: File?,
        retryOnConnectionFailure: Boolean = true,
    ): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieStore)
            .retryOnConnectionFailure(retryOnConnectionFailure)
            .apply { cacheDir?.let { cache(Cache(it, 10L * 1024 * 1024)) } }
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