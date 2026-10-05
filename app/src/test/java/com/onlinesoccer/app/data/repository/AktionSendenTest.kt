package com.onlinesoccer.app.data.repository

import com.onlinesoccer.app.data.model.AktionFeld
import com.onlinesoccer.app.data.model.AktionFeldTyp
import com.onlinesoccer.app.data.model.AktionForm
import com.onlinesoccer.app.data.model.AktionsButton
import com.onlinesoccer.app.data.model.AktionsOption
import com.onlinesoccer.app.data.model.SeitenAnsicht
import com.onlinesoccer.app.data.model.UebersichtZeile
import com.onlinesoccer.app.feature.team.ZeilenAktion
import com.onlinesoccer.app.feature.team.formularFelder
import com.onlinesoccer.app.feature.team.zeilenAktionen
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Beweist, **was** die App bei einer Schreibaktion tatsächlich an den Server
 * sendet – ohne den echten Spielerserver zu erreichen.
 *
 * Die Kette wird Ende zu Ende aus dem echten Server-Dump aufgebaut:
 * `ju.html` → `parseJugend` → `zeilenAktionen` (Zuordnung Zeile ↔ Formular) →
 * `formularFelder` (was der Dialog übergibt) → `fuehreAktionAus` (der POST).
 * Statt ins Netz geht der Request an einen **aufzeichnenden Interceptor**, der
 * Methode, URL und Body festhält und eine Dummy-Antwort liefert.
 *
 * Damit ist der Nachweis unabhängig vom Livespiel und läuft in der CI mit.
 * Er belegt **nicht**, dass der echte Server den Request annimmt – das wäre
 * nur mit einer echten Aktion prüfbar und ist bewusst nicht Teil des Tests.
 */
class AktionSendenTest {

    /** Ein aufgezeichneter Request: Methode, URL und die tatsächlich kodierte Formular-Body. */
    private data class Aufzeichnung(val methode: String, val url: String, val body: String) {
        override fun toString() = "$methode $url  |  $body"
    }

    private val aufzeichnungen = mutableListOf<Aufzeichnung>()

    private fun client(antwortHtml: String = "<html><body>ok</body></html>"): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request()
            // `FormBody` ist `Iterable<Pair<Name, Wert>>`; die Iteration liefert exakt
            // die Reihenfolge und die Kodierung, die auch auf die Leitung gehen.
            val body = (request.body as? FormBody)?.let { form ->
                (0 until form.size).joinToString("&") { i ->
                    "${form.encodedName(i)}=${form.encodedValue(i)}"
                }
            }.orEmpty()
            aufzeichnungen += Aufzeichnung(request.method, request.url.toString(), body)
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(antwortHtml.toResponseBody("text/html".toMediaType()))
                .build()
        }
        .build()

    private fun repo(antwortHtml: String = "<html><body>ok</body></html>"): TeamRepository =
        TeamRepository(client(antwortHtml), ZugabgabeRepository(client()))

    private fun dump(name: String): String =
        File("src/test/resources/dumps/$name.html").readText()

    /**
     * Holt Zeile und Formular einer Seite so, wie es die UI tut: erster Abschnitt
     * mit Zeilen, erste Zeile mit Aktionsschlüssel, zugehörige Aktion dazu.
     */
    private fun ersteZeilenAktion(
        ansicht: SeitenAnsicht,
    ): Triple<UebersichtZeile, ZeilenAktion, AktionForm> {
        val abschnitt = ansicht.abschnitte.first { it.zeilen.any { z -> z.aktionSchluessel != null } }
        val zeile = abschnitt.zeilen.first { it.aktionSchluessel != null }
        val aktionen = zeilenAktionen(zeile, abschnitt.aktionen)
        assertTrue("Zeile hat eine Aktion", aktionen.isNotEmpty())
        return Triple(zeile, aktionen.first(), aktionen.first().form)
    }

    @Test
    fun berufungSendetZiehmichUndZiehenAnJuPhp() = runBlocking {
        val repository = repo()
        val ansicht = repository.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")
        val (zeile, aktion, form) = ersteZeilenAktion(ansicht)

        // So setzt der Dialog das Radio: auf den Schlüssel der angetippten Zeile.
        val radio = form.felder.single { it.name == "ziehmich" }
        val werte = mapOf(radio.name to zeile.aktionSchluessel.orEmpty())
        val felder = formularFelder(form.felder, werte, form.buttons.single())

        val ergebnis = repository.fuehreAktionAus(form.ziel, felder)

        assertTrue("Aktion wird als erfolgreich gemeldet: ${ergebnis.meldung}", ergebnis.erfolg)
        val gesendet = aufzeichnungen.single()
        assertEquals("POST", gesendet.methode)
        assertEquals("https://os.ongapo.com/ju.php", gesendet.url)
        // Genau die zwei Felder der Website: die Spieler-ID und der Submit-Name.
        // OkHttp kodiert Leerzeichen als %20 (nicht als +) – beides ist für den
        // Formular-POST gültig, deshalb wird hier die tatsächliche Leitung encodiert.
        assertEquals(
            "ziehmich=${zeile.aktionSchluessel}" +
                "&ziehen=%20%20%20%20Markierten%20Spieler%20ins%20A-Team%20berufen%20%20%20%20",
            gesendet.body,
        )
        // Die ID muss die des angeklickten Spielers sein, nicht irgendeine.
        assertTrue(
            "Übertragene ID stammt aus dem Dump",
            dump("ju").contains("""name="ziehmich" value="${zeile.aktionSchluessel}""""),
        )
        assertEquals("Ins A-Team berufen", aktion.titel)
    }

    /**
     * `ju.php?page=4`: beide Absende-Buttons haben **keinen** `name`. Die App darf
     * deshalb keinen leeren Parameter mitschicken – sonst wären es zwei Requests.
     */
    @Test
    fun jugendfoerderungSendetNurDenBetrag() = runBlocking {
        val repository = repo()
        val ziel = "https://os.ongapo.com/ju.php?page=4"
        val ansicht = repository.parseJugendOptionen(dump("ju_optionen"), ziel)
        val form = ansicht.abschnitte.first { it.titel == "Allgemeine Optionen" }.aktionen.single()
        val feld = form.felder.single { it.name == "foerderung" }

        val felder = formularFelder(form.felder, mapOf(feld.name to "1500"), form.buttons.single())
        repository.fuehreAktionAus(form.ziel, felder)

        assertEquals("foerderung=1500", aufzeichnungen.single().body)
    }

    /** Dasselbe für die Jugendschranke – der Wert ist eine Schranken-Stufe, kein Betrag. */
    @Test
    fun jugendschrankeSendetNurDenWert() = runBlocking {
        val repository = repo()
        val ziel = "https://os.ongapo.com/ju.php?page=4"
        val ansicht = repository.parseJugendOptionen(dump("ju_optionen"), ziel)
        val form = ansicht.abschnitte.first { it.titel == "Jugendschranke" }.aktionen.single()

        val felder = formularFelder(form.felder, mapOf("schranke" to "1"), form.buttons.single())
        repository.fuehreAktionAus(form.ziel, felder)

        assertEquals("schranke=1", aufzeichnungen.single().body)
    }

    /**
     * Regression vom 05.10.2026: `parseJugend` verdrahtete den Submit-Button fest
     * mit getrimmtem Wert. Die Website liefert ` value=" Markierten Spieler ins
     * A-Team berufen "`. Der Request muss den Wert der Seite verwenden, sonst
     * erkennt der Server die Aktion nicht und die Berufung bleibt ohne Wirkung.
     */
    @Test
    fun berufungSendetDenUnveraendertenButtonwertDerWebsite() {
        val repository = repo()
        val ansicht = repository.parseJugend(dump("ju"), "https://os.ongapo.com/ju.php")
        val (_, _, form) = ersteZeilenAktion(ansicht)

        assertEquals("ziehen", form.buttons.single().name)
        assertEquals(
            "Der Wert muss exakt dem value-Attribut der Seite entsprechen (mit Randleerzeichen)",
            "    Markierten Spieler ins A-Team berufen    ",
            form.buttons.single().text,
        )
    }

    /** Die Positions-, Namens- und Vertragsoptionen kommen vollständig aus der Serverantwort. */
    @Test
    fun berufungsFormularLiestDynamischePositionenNamenVertraegeUndHinweise() {
        val repository = repo()
        val formular = repository.parseJugendBerufungsFormular(
            dump("ju_berufung_formular"),
            "https://os.ongapo.com/ju.php",
        )

        assertTrue("Serverformular erkannt", formular != null)
        formular ?: return
        assertEquals("Jugendspieler ins A-Team berufen", formular.titel)
        assertTrue(formular.hinweise.any { "Skillschnitt: 23.24" in it })
        assertEquals("123456", formular.felder.single { it.name == "ziehmich" }.standard)

        val position = formular.felder.single { it.name == "pos" }
        assertEquals(AktionFeldTyp.RADIO, position.typ)
        assertEquals("Position", position.label)
        assertEquals(listOf("2", "3", "4", "5", "6"), position.optionen.map { it.wert })
        assertEquals("ABW · Opt. 24.11 · Marktwert 311.377", position.optionen.first().label)

        val spielername = formular.felder.single { it.name == "name" }
        assertEquals("Spielername", spielername.label)
        assertEquals(listOf("200001", "200002"), spielername.optionen.map { it.wert })
        assertEquals("Spielername Eins", spielername.optionen.first().label)

        val vertrag = formular.felder.single { it.name == "vertrag" }
        assertEquals("Vertragslaufzeit", vertrag.label)
        assertEquals(listOf("24", "36", "48", "60", "72"), vertrag.optionen.map { it.wert })
        assertEquals("24 Monate Laufzeit: 3.500 Euro", vertrag.optionen.first().label)

        assertEquals("transfer", formular.buttons.single().name)
        assertEquals("   Spieler ins A-Team befördern   ", formular.buttons.single().text)
        assertEquals("https://os.ongapo.com/ju.php", formular.ziel)
    }

    /** Erster Schritt: POST mit Jugend-ID/Button lädt das dynamische Folgeformular, befördert noch nicht. */
    @Test
    fun ersterBerufungsschrittLaedtFolgeformularOhneFinalenSubmit() = runBlocking {
        val html = dump("ju_berufung_formular")
        val repository = repo(html)
        val ziel = "https://os.ongapo.com/ju.php"
        val startfelder = listOf(
            "ziehmich" to "123456",
            "ziehen" to " Markierten Spieler ins A-Team berufen ",
        )

        val formular = repository.ladeJugendBerufungsFormular(ziel, startfelder)

        assertEquals("dynamisches Folgeformular erkannt", "transfer", formular?.buttons?.single()?.name)
        assertEquals("POST", aufzeichnungen.single().methode)
        assertEquals(ziel, aufzeichnungen.single().url)
        assertEquals(
            "ziehmich=123456&ziehen=%20Markierten%20Spieler%20ins%20A-Team%20berufen%20",
            aufzeichnungen.single().body,
        )
        assertTrue("Positionen noch nicht ausgewählt", formular?.felder?.single { it.name == "pos" }?.standard.isNullOrEmpty())
        assertTrue("Vertrag noch nicht ausgewählt", formular?.felder?.single { it.name == "vertrag" }?.standard.isNullOrEmpty())
    }

    /** Der finale Request verwendet die dynamisch geparsten Werte und den finalen Submit-Button. */
    @Test
    fun finaleBerufungSendetPositionNameVertragUndTransferButton() = runBlocking {
        val repository = repo()
        val formular = repository.parseJugendBerufungsFormular(
            dump("ju_berufung_formular"),
            "https://os.ongapo.com/ju.php",
        ) ?: error("Berufungsformular fehlt")
        val werte = mapOf(
            "pos" to formular.felder.single { it.name == "pos" }.optionen[1].wert,
            "name" to formular.felder.single { it.name == "name" }.optionen[1].wert,
            "vertrag" to formular.felder.single { it.name == "vertrag" }.optionen[1].wert,
        )
        val felder = formularFelder(formular.felder, werte, formular.buttons.single())

        val ergebnis = repository.fuehreAktionAus(formular.ziel, felder)

        assertTrue("Mock bestätigt den finalen POST", ergebnis.erfolg)
        val gesendet = aufzeichnungen.single()
        assertEquals("POST", gesendet.methode)
        assertEquals(formular.ziel, gesendet.url)
        val dekodiert = gesendet.body.split('&').map { feld ->
            val teile = feld.split('=', limit = 2)
            URLDecoder.decode(teile[0], StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(teile.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
        }
        assertEquals(
            listOf(
                "ziehmich" to "123456",
                "pos" to "3",
                "name" to "200002",
                "vertrag" to "36",
                "transfer" to "   Spieler ins A-Team befördern   ",
            ),
            dekodiert,
        )
    }

    /** Die Wirkungskontrolle erkennt eine wirkungslose Aktion anhand der Antwort. */
    @Test
    fun wirkungskontrolleMeldetNochWaehlbareOption() {
        val repository = repo()
        val html = dump("ju")
        val pid = repository.parseJugend(html, "https://os.ongapo.com/ju.php")
            .abschnitte.first { it.zeilen.any { z -> z.aktionSchluessel != null } }
            .zeilen.first { it.aktionSchluessel != null }
            .aktionSchluessel.orEmpty()

        val meldung = repository.unveraenderteWirkung(html, listOf("ziehmich" to pid))

        assertTrue("Meldung nennt den Grund: $meldung", meldung != null && "ziehmich=$pid" in meldung)
    }

    /** Nach einer gelungenen Berufung ist der Spieler nicht mehr auswählbar. */
    @Test
    fun wirkungskontrolleSchweigtWennOptionVerschwundenIst() {
        val repository = repo()
        val html = dump("ju").replace("""name="ziehmich" value="246866"""", """name="ziehmich" value="999999"""")

        assertEquals(null, repository.unveraenderteWirkung(html, listOf("ziehmich" to "246866")))
    }

    /**
     * Eine `<select>`-Liste führt ihre Optionen dauerhaft (Trainer, Heizung).
     * Dort darf die Wirkungskontrolle nicht anschlagen, sonst würde eine
     * erfolgreiche Trainer-Einstellung als Scheitern gemeldet.
     */
    @Test
    fun wirkungskontrolleIgnoriertSelectListen() {
        val repository = repo()
        val html = """<form><select name="trainer"><option value="3" selected>Trainer 3</option></select></form>"""

        assertEquals(null, repository.unveraenderteWirkung(html, listOf("trainer" to "3")))
    }

    /** Unberührtes Auswahlfeld darf keinen leeren Parameter senden. */
    @Test
    fun leererWertWirdNichtGesendet() {
        val form = AktionForm(
            ziel = "https://os.ongapo.com/ju.php?page=4",
            felder = listOf(
                AktionFeld(
                    name = "foerderung",
                    typ = AktionFeldTyp.AUSWAHL,
                    optionen = listOf(AktionsOption("500", "500")),
                ),
            ),
            buttons = listOf(
                AktionsButton(name = null, text = "speichern"),
            ),
        )

        val felder = formularFelder(form.felder, emptyMap(), form.buttons.single())

        assertTrue("Leerer Wert erzeugt kein Feldpaar", felder.isEmpty())
    }
}
