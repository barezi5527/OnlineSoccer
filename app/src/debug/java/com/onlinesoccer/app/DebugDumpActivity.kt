package com.onlinesoccer.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.onlinesoccer.app.core.network.OsApi
import com.onlinesoccer.app.data.repository.BewerbeRepository
import com.onlinesoccer.app.data.repository.TeamRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

@AndroidEntryPoint
class DebugDumpActivity : ComponentActivity() {

    @Inject lateinit var teamRepository: TeamRepository
    @Inject lateinit var bewerbeRepository: BewerbeRepository
    @Inject lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thread {
            runBlocking {
                // st.php (öffentliches Teamprofil) für Kader-Fremdteam dumpen
                listOf("st_477" to "${OsApi.BASE_URL}/st.php?c=477").forEach { (name, url) ->
                    runCatching {
                        val body = client.newCall(Request.Builder().url(url).build()).execute()
                            .use { it.body?.string().orEmpty() }
                        File(cacheDir, "dump_$name.html").writeText(body)
                        Log.d("OS_OSC", "Dumped $name -> ${body.length} chars")
                    }.onFailure { Log.e("OS_OSC", "dump $name failed", it) }
                }
                // Bewerbe-Hub: echte Struktur von ls/lt/lp dumpen (Parser-Verifikation)
                listOf(
                    "ls" to "${OsApi.BASE_URL}/ls.php",
                    "ls2" to "${OsApi.BASE_URL}/ls.php?ligaauswahl=3&landauswahl=6&stauswahl=2&saauswahl=24&erganzeigen=1&stataktion=Statistik+ausgeben",
                    "lt" to "${OsApi.BASE_URL}/lt.php",
                    "lp" to "${OsApi.BASE_URL}/lp.php",
                    "lp2" to "${OsApi.BASE_URL}/lp.php?landauswahl=6&stauswahl=1&saauswahl=24&erganzeigen=1&stataktion=Statistik+ausgeben",
                    "zer" to "${OsApi.BASE_URL}/zer.php?erganzeigen=1",
                ).forEach { (name, url) ->
                    runCatching {
                        val body = client.newCall(Request.Builder().url(url).build()).execute()
                            .use { it.body?.string().orEmpty() }
                        File(cacheDir, "dump_$name.html").writeText(body)
                        Log.d("OS_OSC", "DUMP_$name LEN=${body.length}")
                        body.chunked(3500).forEachIndexed { i, chunk ->
                            Log.d("OS_OSC", "DUMP_$name[$i]:\n$chunk")
                        }
                    }.onFailure { Log.e("OS_OSC", "dump $name failed", it) }
                }
                // Zugabgabe: echte Struktur von Aufstellung/Aktionen/Einstellungen dumpen
                listOf(
                    "za0" to "${OsApi.BASE_URL}/zugabgabe.php",
                    "za1" to "${OsApi.BASE_URL}/zugabgabe.php?p=1",
                    "za2" to "${OsApi.BASE_URL}/zugabgabe.php?p=2",
                    "za_beta" to "${OsApi.BASE_URL}/zugabgabe_beta.php",
                    "zuzu" to "${OsApi.BASE_URL}/zuzu.php",
                ).forEach { (name, url) ->
                    runCatching {
                        val body = client.newCall(Request.Builder().url(url).build()).execute()
                            .use { it.body?.string().orEmpty() }
                        File(cacheDir, "dump_$name.html").writeText(body)
                        Log.d("OS_OSC", "DUMP_$name LEN=${body.length}")
                        body.chunked(3500).forEachIndexed { i, chunk ->
                            Log.d("OS_OSC", "DUMP_$name[$i]:\n$chunk")
                        }
                    }.onFailure { Log.e("OS_OSC", "dump $name failed", it) }
                }
                listOf(
                    "za1_i1" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=1",
                    "za1_i2" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=2",
                    "za1_i3" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=3",
                    "za1_i4" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=4",
                    "za1_i5" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=5",
                    "za1_i6" to "${OsApi.BASE_URL}/zugabgabe.php?p=1&item=6",
                    "za2_i8" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=8",
                    "za2_i9" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=9",
                    "za2_i10" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=10",
                    "za2_i11" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=11",
                    "za2_i12" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=12",
                    "za2_i13" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=13",
                    "za2_i14" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=14",
                    "za2_i16" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=16",
                    "za2_i17" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=17",
                    "za2_i18" to "${OsApi.BASE_URL}/zugabgabe.php?p=2&item=18",
                    "za_checkza" to "${OsApi.BASE_URL}/checkza.php",
                ).forEach { (name, url) ->
                    runCatching {
                        val body = client.newCall(Request.Builder().url(url).build()).execute()
                            .use { it.body?.string().orEmpty() }
                        File(cacheDir, "dump_$name.html").writeText(body)
                        Log.d("OS_OSC", "DUMP_$name LEN=${body.length}")
                        body.chunked(3500).forEachIndexed { i, chunk ->
                            Log.d("OS_OSC", "DUMP_$name[$i]:\n$chunk")
                        }
                    }.onFailure { Log.e("OS_OSC", "dump $name failed", it) }
                }
                runCatching {
                    val kader = teamRepository.ladeKader()
                    Log.d("OS_OSC", "KADER_COUNT=${kader.size}")
                    kader.forEach { k ->
                        Log.d("OS_OSC", "KADER pid=${k.pid} nr=${k.nummer} ${k.name} pos=${k.position} alt=${k.alter} skill=${k.skill} opti=${k.opti} fit=${k.fit} mor=${k.mor}")
                    }
                }.onFailure { Log.e("OS_OSC", "kader parse failed", it) }

                runCatching {
                    val lt = bewerbeRepository.ladeLigatabelle()
                    Log.d("OS_OSC", "LIGA header=${lt?.header} saison=${lt?.saison} filter=${lt?.filter}")
                    lt?.zeilen?.forEach { z -> Log.d("OS_OSC", "LIGA_ROW $z") }
                }.onFailure { Log.e("OS_OSC", "liga parse failed", it) }

                runCatching {
                    val v = teamRepository.ladeVertraege()
                    Log.d("OS_OSC", "VERTRAEGE_COUNT=${v.size}")
                    v.take(4).forEach { Log.d("OS_OSC", "VERTRAG ${it.name} gehalt=${it.gehalt} lauf=${it.laufzeit} mw=${it.marktwert} geb=${it.geburtstag}") }
                }.onFailure { Log.e("OS_OSC", "vertraege parse failed", it) }

                runCatching {
                    val s = teamRepository.ladeStaerken()
                    Log.d("OS_OSC", "STAERKEN_COUNT=${s.size}")
                    s.take(2).forEach { Log.d("OS_OSC", "STAERKEN ${it.name} keys=${it.werte.keys}") }
                }.onFailure { Log.e("OS_OSC", "staerken parse failed", it) }
            }
            Log.d("OS_OSC", "DEBUG_DUMP_DONE")
        }
    }
}
