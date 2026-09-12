package com.onlinesoccer.app.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpCacheDir(@ApplicationContext context: Context): File =
        File(context.cacheDir, "http_cache")

    @Provides
    @Singleton
    fun provideOkHttpClient(cookieStore: OsCookieStore, cacheDir: File): OkHttpClient =
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(cookieStore)
            .cache(Cache(cacheDir, 10L * 1024 * 1024))
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