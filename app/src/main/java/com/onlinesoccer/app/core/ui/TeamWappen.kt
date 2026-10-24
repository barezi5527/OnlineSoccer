package com.onlinesoccer.app.core.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.onlinesoccer.app.core.network.OsApi
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Zentraler Lader für Vereinswappen.
 *
 * Der Cache ist nach Team-ID geschlüsselt. Dadurch bleiben Haupt- und
 * Zweitteam getrennt, und ein Teamwechsel invalidiert keine Einträge.
 */
object TeamWappenLoader {
    private val cache = ConcurrentHashMap<Long, ImageBitmap>()
    private val sperren = ConcurrentHashMap<Long, Mutex>()

    /** Bereits geladenes Wappen ohne Netzzugriff, z. B. als Startwert für Compose. */
    fun cached(teamId: Long): ImageBitmap? = cache[teamId]

    /**
     * Lädt das Wappen einer Team-ID. Die Endungen png, gif und jpg werden
     * nacheinander probiert, weil der Server sie je Verein unterschiedlich ablegt.
     * Parallele Aufrufe für dieselbe ID teilen sich einen Request.
     */
    suspend fun load(teamId: Long): ImageBitmap? {
        cache[teamId]?.let { return it }
        val sperre = sperren.getOrPut(teamId) { Mutex() }
        return sperre.withLock {
            cache[teamId] ?: lade(teamId)?.also { cache[teamId] = it }
        }
    }

    private suspend fun lade(teamId: Long): ImageBitmap? = withContext(Dispatchers.IO) {
        OsApi.wappenUrls(teamId).firstNotNullOfOrNull { url ->
            runCatching {
                val verbindung = URL(url).openConnection() as HttpURLConnection
                try {
                    verbindung.connectTimeout = 8_000
                    verbindung.readTimeout = 8_000
                    verbindung.setRequestProperty("User-Agent", OsApi.USER_AGENT)
                    verbindung.inputStream.use { strom ->
                        BitmapFactory.decodeStream(strom)?.asImageBitmap()
                    }
                } finally {
                    verbindung.disconnect()
                }
            }.getOrNull()
        }
    }
}

/**
 * Zeigt das Wappen eines Vereins. Ohne Team-ID oder bei fehlendem Bild
 * erscheint ein dezenter Kreis mit der Initiale des Vereinsnamens.
 */
@Composable
fun TeamWappen(
    teamId: Long?,
    verein: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val vorhanden = teamId?.let(TeamWappenLoader::cached)
    val bild by produceState<ImageBitmap?>(initialValue = vorhanden, teamId) {
        value = if (teamId == null) null else TeamWappenLoader.load(teamId)
    }
    val geladen = bild
    if (geladen != null) {
        Image(
            bitmap = geladen,
            contentDescription = "Vereinswappen $verein",
            modifier = modifier.size(size),
            contentScale = ContentScale.Fit,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initiale(verein),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

private fun initiale(verein: String): String =
    verein.trim().firstOrNull { it.isLetter() }?.uppercaseChar()?.toString() ?: "–"
