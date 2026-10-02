# Maßnahmeplan Teamwechsel (1|2-Button im Header)

**Feature:** Toggle-Button `1` / `2` in der `TopAppBar` neben dem Darkmode-Button.
`1` = Hauptteam, `2` = Zweitteam. Button wird **nur** angezeigt, wenn der angemeldete
Account ein Zweitteam besitzt.

**Arbeitsweise:** Jeder Schritt dauert **max. 30 Minuten** und wird einzeln abgearbeitet.
Nach jedem Schritt: Haken setzen, `./gradlew test` laufen lassen, ggf. committen.
Kein Schritt wird übersprungen — wenn ein Schritt zu groß wird, teile ihn auf (siehe „Aufteilung" unten).

**Aktueller Stand:** ☐ offen / ☑ erledigt — wird nach jedem Schritt aktualisiert.

---

## Verifizierte Ausgangslage (nicht nochmal prüfen)

Live mit dem Account geprüft, das gilt als gesichert:

| Sachverhalt | Wert |
|---|---|
| Endpunkt | `GET /haupt.php?changetosecond=true` — reiner `isset()`-Toggle |
| Gegenstück | **gibt es nicht** (kein `false`, keine `teamId`, kein Zielparameter) |
| `os`-Cookie | byte-identisch vor/nach Wechsel → Team lebt in der PHP-Session |
| `changetosecond` auf `haupt.php` | genau **1×**, Text `Zu NK Kamen Sesvete wechseln` |
| Eigene Wappen-Img auf `haupt.php` | **2× nur das eigene** (links + rechts), Gegnerwappen kommt **nicht** vor |
| Andere Team-ID auf `haupt.php` | **0 Treffer** |
| Andere Team-ID auf `showteam.php?s=0` | **vorhanden**: `<a href="st.php?c=1216" onClick="teaminfo(1216)">Mein Zweitteam</a>` |
| Eigene Team-ID auf `showteam.php?s=0` | `<a href="javascript:tabellenplatz(3449)">Tabellenplätze</a>` |
| Cache-Header | `no-store, no-cache, must-revalidate` auf allen Teamseiten |
| Frischer Login / stiller Re-Login via `lc` | Server setzt **immer auf Hauptteam** zurück — aber **nur**, wenn wirklich neu angemeldet wird. Greift das Cookie noch, wird **gar nicht** angemeldet und der Server bleibt auf Team 2 (in T26a live bestätigt). |
| Teams dieses Accounts | `3449` SC Viktoria Ulm (Hauptteam), `1216` NK Kamen Sesvete (Zweitteam, `.gif`-Wappen) |

### Bereits angelegte Test-Fixtures (durch Agent erstellt, kein Aufwand)

`app/src/test/resources/dumps/`:
- `haupt_team1_aktiv.html` — Team 1 aktiv, Link „Zu NK Kamen Sesvete wechseln"
- `haupt_team2_aktiv.html` — Team 2 aktiv, Link „Zu SC Viktoria Ulm wechseln"
- `haupt_team1_zurueck.html` — nach Rückwechsel, Team 1 aktiv
- `haupt_ohne_zweitteam.html` — aus `haupt_team1_aktiv.html` erzeugt, Anker entfernt → simuliert Account **ohne** Zweitteam

---

## Feste Regeln für den ganzen Plan

1. **KEIN persistierter Team-State.** Der aktive Index wird **immer** aus dem Server gelesen
   (`DashboardRepository.fetchDashboard().teamId`). Grund: die PHP-Session überlebt einen
   Kaltstart (gültiges Cookie ⇒ kein Neu-Login), der Server steht dann weiter auf Team 2 —
   und bei abgelaufener Session setzt ein Neu-Login auf das Hauptteam zurück. Beide Fälle
   ändern den Serverzustand, ein gespeicherter Index wäre also in einem der beiden Fälle
   falsch.
2. **KEIN `TeamScope`-Refactor.** `teamId == null` (→ `showteam.php?s=N` ohne `c`) bleibt unverändert.
   Der Session-Toggle wirkt automatisch auf alle Requests.
3. **KEIN automatischer zweiter Toggle.** Genau ein Schreibvorgang pro Nutzergeste.
4. **Erkennung nur über `a[href*="changetosecond"]`.** Linktext ist reine Anzeige.
   Grund: das bestehende Fixture `DashboardRepositoryParseTest.kt:47` hat einen **leeren Anchor**
   (`<a href="?changetosecond=true"></a>`) — ein textbasierter Parser würde daran sterben.
5. **`retryOnConnectionFailure(false)`** für Toggle + Verifikation, sonst schickt OkHttp den
   GET bei Verbindungsabbruch ein zweites Mal → stiller Rück-Toggle.
6. **Single-Flight-Guard statt `Mutex`.** Ein `Mutex` serialisiert, dedupliziert aber nicht:
   Doppelklick → zweiter Request wartet und toggelt **zurück**.
7. **Wechselsperre nach bestätigtem Wechsel (T38a).** 15 s kein weiterer Toggle. Grund: der
   Endpunkt ist ein blinder Toggle **ohne Ziel**, jeder Request schaltet um — und weil T26 nach
   dem Wechsel den Backstack leert, bestätigt sich der Tipp nicht *am selben Ort*. Sperre
   ausschließlich nach `Erfolgreich`, **nicht** nach `Unveraendert`/`Fehler`: sonst würde sie
   genau den Retry blockieren, den ein gescheiterter Aufruf braucht.

---

## Legende Aufwand

`S` = ~15 min · `M` = ~30 min · `L` = aufteilen (siehe Hinweis)

---

## Block 0 — Datenmodell & Parser (kein Netz, kein UI)

> Grundlage für alles Weitere. Erst dieser Block, dann der Rest.

- [x] **T01 — Test-Fixtures anlegen** · `S`
  Live-Dumps aus `/tmp/opencode/` nach `app/src/test/resources/dumps/` kopieren und eine
  Variante ohne Anker erzeugen. **Erledigt** — Dateien liegen vor, siehe „Ausgangslage".

- [x] **T02 — `TeamwechselInfo` anlegen** · `S`
  Neue Datei `data/model/Teamwechsel.kt`:
  ```kotlin
  data class TeamwechselInfo(
      val wechselMoeglich: Boolean,
      val zweitTeamName: String? = null,   // aus "Zu X wechseln", nur Anzeige
  )
  ```
  `DashboardData` (data/model/DashboardData.kt) um ein Feld erweitern:
  `val teamwechsel: TeamwechselInfo? = null` — **Default `null`**, damit alle bestehenden
  Konstruktor-Aufrufe und Tests gültig bleiben.
  **Erledigt** — `data/model/Teamwechsel.kt` neu, Feld in `DashboardData` hinten angehängt.
  ⚠️ Präzisierung für T04: `parse()` befüllt das Feld **immer** (auch ohne Zweitteam mit
  `wechselMoeglich = false`) — der Default `null` gilt nur für Aufrufer, die es nicht setzen.
  Grund: `teamwechsel?.wechselMoeglich == false` wäre bei `null` nicht erfüllbar.

- [x] **T03 — `DashboardRepository.parse()` erweitern** · `M`
  In `DashboardRepository.parse()` (aktuell Zeile 77–196) ergänzen:
  - `doc.selectFirst("a[href*=\"changetosecond\"]")` → Anwesenheit = `wechselMoeglich`
  - Zweitteam-Name per `Regex("""^\s*Zu\s+(.+?)\s+wechseln\s*$""")` aus `it.text()`
    (mit `&nbsp;`-Normalization). **Nur Anzeige, nie Erkennung.**
  - Nichts am bestehenden `teamId`-/`teamName`-Parsing ändern — die
    Willkommenszelle ist die Identitätsquelle (siehe T09).
  ⚠️ Fallstrick: `doc.text()` nicht `ownText()` verwenden (Entities wie `&uuml;`).
  **Erledigt** — Erkennung über `a[href*="changetosecond"]` (dokumentweit, laut Ausgangslage
  genau 1 Treffer), Name über `WECHSELTEXT` mit vorheriger `&nbsp;`-/Whitespace-Normalisierung.
  Bestehendes `teamId`-/`teamName`-/`liga`-/`kontostand`-Parsing unverändert — gegen alle drei
  `haupt_*.html`-Fixtures geprüft, Team 2 liefert Kontostand 7.242.629 € wie im Plan vermerkt.
  (Achtung T04: das `<br />` **innerhalb** des Ankers verschiebt die `liga`-Zeile nicht — der
  Absatz „2. Liga …" bleibt Segment 1.)

- [x] **T04 — Parser-Tests schreiben** · `M`
  `app/src/test/java/.../DashboardRepositoryParseTest.kt` erweitern:
  - `teamwechselTeam1Aktiv` → Fixture `haupt_team1_aktiv.html`: `wechselMoeglich == true`,
    `zweitTeamName == "NK Kamen Sesvete"`
  - `teamwechselTeam2Aktiv` → `haupt_team2_aktiv.html`: `zweitTeamName == "SC Viktoria Ulm"`
  - `teamwechselFehltOhneZweitteam` → `haupt_ohne_zweitteam.html`: `wechselMoeglich == false`
  - `teamwechselLeererAnchorErkannt` → inline HTML mit `<a href="?changetosecond=true"></a>`
    → `wechselMoeglich == true` (Regression gegen Regel 4)
  - Bestehende Tests müssen grün bleiben (Beweis: keine Regression).
  **Erledigt** — 4 Tests in `DashboardRepositoryParseTest.kt` ergänzt (Fixture-Helper `dump()`
  nach Muster der anderen Parse-Tests). Zusätzlich je Team-ID + Teamname mitgeprüft, weil das
  `<br />` **im** Anker die `liga`-Zeile verschieben könnte. Suite: 383 Tests, 0 Fehler.

- [x] **T05 — Block 0 grün** · `S`
  `./gradlew :app:testDebugUnitTest` → grün. **Commit erfolgt (e89391d).**

---

## Block 1 — Netzwerkschicht für den Toggle

- [x] **T06 — `OsApi.TEAMWECHSEL` ergänzen** · `S`
  **Erledigt** — `OsApi.TEAMWECHSEL` direkt nach `INDEX`.
  In `core/network/OsApi.kt` (aktuell Zeile 18 nach `MAIN`):
  ```kotlin
  /** Reiner Session-Toggle: jeder Aufruf schaltet um. Kein Ziel, kein 'false'. */
  const val TEAMWECHSEL = "$BASE_URL/haupt.php?changetosecond=true"
  ```

- [x] **T07 — Toggle-Client ohne Retry** · `M`
  **Erledigt** — mit einer Abweichung, die der Plan nicht berücksichtigt hatte:
  ⚠️ Ein zweiter `@Provides fun …: OkHttpClient` **ohne** Qualifier wäre ein
  Hilt-Duplicate-Binding, weil ~20 Repositories den Standard-Client unqualifiziert
  injizieren (verifiziert per grep). Deshalb eigener Qualifier `@ToggleClient`
  (in NetworkModule.kt). Der gemeinsame Aufbau steckt jetzt in `buildOkHttpClient(...)`
  mit `cacheDir: File?` = `null` ⇒ **kein Cache** auf dem Toggle-Client (wie gefordert).
  `:app:assembleDebug` grün ⇒ Hilt-Graph validiert.
  In `core/network/NetworkModule.kt` einen **zweiten** Provider ergänzen (Regel 5):
  ```kotlin
  @Provides @Singleton
  fun provideToggleClient(cookieStore: OsCookieStore): OkHttpClient =
      provideOkHttpClient(cookieStore, ...)   // Cache weglassen
          .newBuilder()
          .retryOnConnectionFailure(false)
          .build()
  ```
  Einfacher: `provideOkHttpClient(...)` um einen Parameter `retry: Boolean = true` erweitern.
  **Kein `Cache`** auf diesem Client.

- [x] **T08 — `TeamwechselRepository` anlegen** · `M`
  **Erledigt** — injiziert den `@ToggleClient`. `teamwechselDurchfuehren(): Boolean`
  (true = Server hat geantwortet, false = Fehler/Abbruch/laufend), Single-Flight über
  `AtomicBoolean.compareAndSet(false, true)` im `try`/`finally`, **kein** `Mutex`.
  Antwort wird über `holeToggleHtml(): String?` durchgereicht (T09) statt selbst geparst;
  `SessionGuard.isPersonalView` fängt die abgelaufene Session ab, damit die Login-Ansicht
  nicht als „Wechsel ok" durchgeht. `:app:assembleDebug` grün.
  ⚠️ Für T09 wichtig: die Begrüßungszelle selbst enthält **kein** Wappen-Img — die beiden
  `images/wappen/…`-Treffer liegen links und rechts daneben (beidefixtures geprüft:
  `00003449.png` bzw. `00001216.gif`).
  `data/repository/TeamwechselRepository.kt`, `@Singleton`, injiziert den Toggle-Client.
  ```kotlin
  suspend fun teamwechselDurchfuehren(): Boolean  // true = Server antwortete, false = Fehler
  ```
  Ablauf: **Single-Flight-Guard** (`if (laeuft) return false`), dann GET `TEAMWECHSEL`.
  Kein `Mutex`. Kein Retry. Kein automatischer zweiter Versuch.
  Der zurückgegebene Byte-Stream wird an T09 übergeben, nicht selbst geparst.

- [x] **T09 — Verifikation: Identität am Willkommens-Zelltext** · `M`
  **Erledigt** — `internal fun aktivesTeamAusHtml(html): Pair<Long?, String?>` in
  `TeamwechselRepository` (liegt dort, damit `TeamwechselRepository` die einzige Stelle bleibt,
  die den Toggle kennt; die Auswertungs-Tests in T11 nutzen sie mit).
  ⚠️ **Plan-Korrektur, live verifiziert:** die Begrüßungszelle enthält **kein** Wappen-Img,
  die Plan-Angabe „erstes Wappen innerhalb dieser Zelle" hätte immer `null` geliefert. Korrekt
  ist die **umgebende Tabellenzeile** (`buero.closest("tr")`) — dort stehen links/rechts die
  beiden eigenen Wappen. Gegen alle 3 Fixtures geprüft:
  `haupt_team1_aktiv → (3449, SC Viktoria Ulm)`, `haupt_team2_aktiv → (1216, NK Kamen Sesvete)`,
  `haupt_team1_zurueck → (3449, SC Viktoria Ulm)`.
  Neue interne Funktion in `TeamwechselRepository` (oder in `DashboardRepository`, damit die
  bestehenden Parse-Tests greifen):
  ```kotlin
  internal fun aktivesTeamAusHtml(html: String): Pair<Long?, String?>
  ```
  Quelle: `td:contains(Willkommen im Managerbüro)` → Text nach `von `, Team-ID aus
  `Regex("""(\d+)\.""")` auf dem **ersten** `img[src*=images/wappen]` **innerhalb dieser Zelle /
  der Header-Tabelle** (live verifiziert: auf `haupt.php` gibt es nur das eigene Wappen).
  ⚠️ Nicht das erste Wappen-Img im **ganzen** Dokument nehmen — die Bestandslogik in
  `DashboardRepository.kt:101` ist korrekt, aber die Verifikation darf nicht breiter werden
  als der geprüfte Bereich.
  **Warum Zelltext statt `DashboardData.teamId`:** der 30-s-Cache macht den Vorzustand sonst
  systematisch falsch, und der Zelltext ist die Quelle, die auch bei leerem Wappen trägt.

- [x] **T10 — `TeamwechselErgebnis` als Sealed Interface** · `S`
  **Erledigt** — `data/model/TeamwechselErgebnis.kt`. Ergänzt um `internal fun werteAus(
  vorher, nachher)` in `TeamwechselRepository`: rein, ohne Netz, in T11 testbar.
  Fallunterscheidung: ID-Vergleich, wenn auf beiden Seiten eine ID vorliegt;
  sonst Namensvergleich; `Unveraendert` bei gleicher ID **oder** gleichem Namen
  (kein blindes „Erfolg" — Plan-Kriterium aus T11); `Fehler` bei fehlendem
  Vor-/Nachzustand.
  ```kotlin
  sealed interface TeamwechselErgebnis {
      data class Erfolgreich(val teamId: Long?, val teamName: String?) : TeamwechselErgebnis
      data object Unveraendert : TeamwechselErgebnis   // Server blieb gleich
      data class Fehler(val text: String) : TeamwechselErgebnis
  }
  ```
  Auswertungslogik rein und `internal`, damit sie ohne Netz testbar ist (T11).

- [x] **T11 — Auswertungs-Tests** · `M`
  **Erledigt** — `TeamwechselAuswertungTest.kt`, 8 Tests, ohne Netz. Deckt die 4 Plan-Fälle ab
  plus 4 Ergänzungen: Name-unterschied ohne IDs ⇒ Erfolg, ID ohne Namen ⇒ Erfolg,
  fehlender Vor-/Nachzustand ⇒ `Fehler`, und die Verifikation gegen alle drei `haupt_*.html`.
  `OsApi.TEAMWECHSEL == "https://os.ongapo.com/haupt.php?changetosecond=true"` ist mit
  drin. Suite: 391 Tests, 0 Fehler.
  `app/src/test/java/.../TeamwechselAuswertungTest.kt` — **ohne Netz**, gegen handgebaute Werte:
  - Team-IDs `3449 → 1216` ⇒ `Erfolgreich`
  - gleiche ID vorher/nachher ⇒ `Unveraendert`
  - `teamId == null` + gleicher Name ⇒ `Unveraendert` (nicht blind Erfolg melden)
  - `OsApi.TEAMWECHSEL == "https://os.ongapo.com/haupt.php?changetosecond=true"`

- [x] **T12 — Block 1 grün** · `S`
  `./gradlew :app:testDebugUnitTest` → grün (391/0). `:app:assembleDebug` grün. Commit erfolgt.

---

## Block 2 — Team-IDs ermitteln (Discovery, einmalig)

> Ohne diese IDs kann der Button den aktiven Index (1 oder 2) nicht anzeigen.

- [x] **T13 — Discovery-Parser schreiben** · `M`
  **Erledigt** — `internal fun parseTeamIds(html): TeamIds?` in `TeamRepository`
  (neben `ladeTeaminformationenMenu`), `TeamIds` in `data/model/TeamDetails.kt`.
  ⚠️ **Live-Fund, im Plan nicht berücksichtigt:** der `st.php?c=`-Link ist in einen
  `<a href="javascript:writePM(1398)">Bahri Er</a>`-Anker **verschachtelt**. Mit `text()`
  zöge der Fremdtext „Bahri Er" mit und der Vergleich auf „Mein Zweitteam" scheiterte →
  `zweitTeamId` immer `null`. Korrekt ist `ownText()` **am `st.php?c=`-Anker selbst**.
  Gegen 3 Fixtures geprüft: `showteam_mit_zweitteam → (3449, 1216)`,
  `showteam_mit_hauptteam_link → (1216, 3449)`, `showteam_s0 → (3449, null)`.
  Fixtures aus `/tmp/opencode/` übernommen.
  In `TeamRepository` (dort liegt schon `ladeTeaminformationenMenu()`, Zeile ~1303, das dieselbe
  Seite lädt):
  ```kotlin
  internal fun parseTeamIds(html: String): TeamIds?
  data class TeamIds(val hauptTeamId: Long?, val zweitTeamId: Long?)
  ```
  Quellen (beide live verifiziert, **sprachneutral**):
  - Hauptteam: `a[href*="tabellenplatz("]` → `Regex("""tabellenplatz\((\d+)\)""")`
  - Zweitteam: `a[href*="st.php?c="]` **mit** Ankertext `Mein Zweitteam` / `Mein Hauptteam`
    → `Regex("""st\.php\?c=(\d+)""")`
  ⚠️ `st.php?c=`-Links gibt es auf `showteam.php?s=0` **auch für Fremdvereine** in Tabellen.
  Nur der Link **direkt neben** `Mein Zweitteam` zählt → über den Ankertext filtern und
  den Container (`closest("td")`) prüfen.

- [x] **T14 — Discovery-Tests** · `S`
  **Erledigt** — 4 Tests in `TeamRepositoryParseTest.kt` (Suite dort jetzt 23 Tests).
  Zwei Fixtures neu: `showteam_mit_zweitteam.html` (Plan-Pfad) und zusätzlich
  `showteam_mit_hauptteam_link.html` (Aufnahme mit aktivem Team 2) — ohne den zweiten
  Fall wäre die Rollenumkehr ungetestet. Negativtest als Plan gefordert: `showteam_s0`
  (kein „Mein Zweitteam") ⇒ `zweitTeamId == null`. Zusätzlich ein Fremdverein-Test, der den
  `st.php?c=1216`-Link durch `st.php?c=1382` „SV Beispiel" ersetzt — beweist, dass die
  Filterung über den Ankertext wirklich am Text hängt (Plan-Warnung vor Fremdvereins-Links).
  Suite: 395 Tests, 0 Fehler.
  Gegen `/tmp/opencode/page_showteam.php.html` als neues Fixture `showteam_mit_zweitteam.html`
  ⇒ `hauptTeamId == 3449`, `zweitTeamId == 1216`.
  Zusätzlich Negativtest: Fixture **ohne** `Mein Zweitteam` ⇒ `zweitTeamId == null`.

- [x] **T15 — Discovery im AppViewModel verdrahten** · `M`
  **Erledigt** — `MutableStateFlow<Long?>` + `StateFlow<Long?> zweitTeamId` in `AppViewModel`,
  dazu `TeamRepository.ladeTeamIds()` (holt `showteam.php?s=0`).
  Einmalig beim Übergang in `SignedIn` über `beobachteAnmeldung()` (collect auf
  `sessionManager.state`), **nicht** bei jedem Screenwechsel. `SignedInDemo` ⇒ bleibt `null`
  (Gast hat kein Zweitteam, Button bleibt unsichtbar), `logout()` setzt zurück.
  **Nicht persistiert** (Regel 1) — kein Schreibzugriff auf `TokenStorage`.
  `ui/AppViewModel.kt`: `MutableStateFlow<Long?>` für `zweitTeamId`.
  Einmalig nach `AuthUiState.SignedIn` laden (nicht bei jedem Screenwechsel).
  Bei `logout()`/`login()` zurücksetzen — **nicht persistieren** (Regel 1).

- [x] **T16 — Block 2 grün** · `S`
  `./gradlew :app:testDebugUnitTest` → grün (395/0), `:app:assembleDebug` grün. Commit erfolgt.

---

## Block 3 — ViewModel-Logik

- [x] **T17 — `teamwechselState` im AppViewModel** · `M`
  **Erledigt** — `TeamwechselUiState` (top-level in `AppViewModel.kt`, neben der bestehenden
  `vertraegeKurzVorAuslauf`-Testlogik) + `StateFlow` im ViewModel.
  ⚠️ Plan-Korrektur: `internal val teamwechsel` **muss** `internal` sein — `AppViewModel` ist
  `public`, eine `public`-Property mit `internal`-Typ-Argument kompiliert nicht (Kotlin-Fehler
  „exposes its internal type argument"). Die UI liest den State ohnehin aus demselben Modul.
  `ladeTeamIds()` spiegelt die Zweitteam-ID jetzt in den Button-Zustand, sonst bliebe
  [aktiverIndex] dauerhaft auf 1.
  `ui/AppViewModel.kt` ergänzen:
  ```kotlin
  data class TeamwechselUiState(
      val wechselMoeglich: Boolean = false,
      val zweitTeamName: String? = null,
      val teamId: Long? = null,
      val zweitTeamId: Long? = null,
      val laeuft: Boolean = false,
      val meldung: String? = null,
  ) {
      /** 1 = Hauptteam, 2 = Zweitteam. Default 1: Login setzt immer auf Hauptteam. */
      val aktiverIndex: Int get() = if (zweitTeamId != null && teamId == zweitTeamId) 2 else 1
  }
  ```
  `aktiverIndex` wird **nie persistiert** — immer aus dem Server-Befund.

- [x] **T18 — `wechsleTeam()` implementieren** · `M`
  **Erledigt** — `ui/AppViewModel.kt`: `wechsleTeam()` + `fuehreWechselAus()`, Reihenfolge
  exakt wie unten (ein Toggle, `invalidate()`, **ein** Refetch). Acht Abweichungen bzw.
  Zusätze, alle aus dem Code heraus begründet:
  1. ⚠️ **`teamwechselDurchfuehren()` liefert jetzt `String?` statt `Boolean`** (die HTML).
     Nötig, damit der ViewModel über `aktivesTeamAusHtml` + `werteAus` verifizieren kann
     (T09/T10/T11 wären sonst toter Code). Der `AtomicBoolean`-Guard ist genau der Grund,
     warum der ViewModel **diese** Methode aufruft und nicht `holeToggleHtml()` direkt —
     Letzteres würde an der Single-Flight-Sperre vorbeigehen.
  2. ⚠️ **`laeuft` wird vor dem `launch` gesetzt**, nicht darin. Sonst passieren zwei Klicks
     in derselben Frame beide die Prüfung aus Schritt 1 — das Loch steckt in der
     Planformulierung, nicht in der Absicht.
  3. **Vorzustand (Schritt 2) wird beim Anmelden geseedet**, sonst endet der *erste* Klick
     immer in `Fehler`: `teamId` ist vor dem ersten Toggle unbekannt. Quelle ist
     `TeamIds.teamId` aus `showteam.php?s=0` — das ist der 1:1-Befund für „gerade aktiv",
     unabhängig von der Rolle. Kein zusätzlicher Request, kein Cache-Problem.
     ⚠️ Die frühere Begründung hier („beim Anmelden steht der Server garantiert auf dem
     Hauptteam") war **falsch** — sie hat den in T26a gefundenen Bug erzeugt. `teamId` ist
     keine Rolle, sondern der Serverbefund.
  4. **Nachzustand (Schritt 6) kommt aus dem Refetch**, die Toggle-Antwort ist die
     unabhängige zweite Quelle und greift, wenn der Refetch ausfällt. Damit ist der
     `Fehler`-Zweig aus `werteAus` praktisch nur noch bei echtem Totalausfall erreichbar:
     eine verlorene Toggle-Antwort wird nicht als „nicht bestätigt" gemeldet, wenn der
     Refetch den Wechsel zeigt.
  5. **`TeamwechselUiState.teamName` ergänzt** (T17 hatte es nicht) — der Namensvergleich
     in `werteAus` braucht den Vor-Namen, und die Erfolgsmeldung soll den neuen Teamnamen
     nennen können.
  6. **`teamwechselMeldung()`** als top-level `internal fun` (Muster `vertraegeKurzVorAuslauf`),
     ohne Netz in T19 testbar, Eingang für die Snackbar in T22. ⚠️ Erfolg nennt
     `Erfolgreich.teamName` (aus dem Refetch) — **nicht** `zweitTeamName`: der Linktext
     „Zu X wechseln" nennt nach dem Wechsel das eben **verlassene** Team.
  7. **`ladeTeamIds()` nur noch in `beobachteAnmeldung()`.** Der Aufruf im `init` war
     überflüssig: `StateFlow.collect` liefert den aktuellen Zustand sofort mit, d. h. jeder
     Kaltstart holte `showteam.php?s=0` **zweimal**.
  8. **Reset beim Ab-/Anmelden** über `teamwechselZuruecksetzen()` (bisher nur `_zweitTeamId`,
     wodurch ein zweiter Account die `teamId` des ersten geerbt hätte und der Vergleich aus
     Schritt 2 den Erfolg falsch bewertet hätte). Der abschließende Schreibvorgang nutzt
     deshalb den **aktuellen** State, nicht die Vorzustands-Kopie — zwischen Toggle und
     Refetch suspendiert der Pfad, da kann ein Logout dazwischenliegen.
  Ablauf **exakt in dieser Reihenfolge**:
  1. `if (laeuft) return` — Single-Flight (Regel 6)
  2. Vorzustand aus dem **aktuellen** Serverstand sichern
  3. `TeamwechselRepository.teamwechselDurchfuehren()`
  4. `DashboardRepository.invalidate()`
  5. `DashboardRepository.fetchDashboard(forceRefresh = true)` — genau **ein** Refetch
  6. Aus dem Ergebnis `teamId`/`teamName` setzen
  7. `bewerbeRepository` / ViewModel-Caches neu binden → siehe T22
  8. `meldung` setzen (Erfolg nennt den **neuen** Teamnamen aus dem Refetch)
  **Kein Optimismus:** der Button zeigt erst nach Schritt 6 den neuen Wert.
  **Kein `CancellationException` als Fehler** behandeln (App-Kill mitten im Toggle ist harmlos,
  der nächste Start liest den Server).
  ⚠️ Für T21/T22: `TeamwechselUiState.zweitTeamId` darf **nach** einem Wechsel nicht neu
  ermittelt werden — `parseTeamIds` ist rollenbezogen und liefert dann das Hauptteam als
  „Zweitteam", womit `aktiverIndex` auf 1 zurückspringt, obwohl Team 2 aktiv ist.

- [x] **T19 — ViewModel-Tests** · `M`
  **Erledigt** — `AppViewModelTeamwechselTest.kt`, 11 Tests, ohne Netz und ohne Android.
  Die 4 Plan-Fälle für `aktiverIndex` sind drin (`3449/1216 ⇒ 1`, `1216/1216 ⇒ 2`,
  `1216/null ⇒ 1`, `null/null ⇒ 1`), dazu 7, die die in T18 eingebauten Fallen festschreiben:
  * `wechselHinterlaesstZweitTeamIdUndIndex` — spielt genau den Schreibvorgang aus
    `fuehreWechselAus()` nach (nur `teamId`/`teamName` wandern) und beweist, dass der Index
    danach auf 2 steht.
  * `neuErmittelteTeamIdsWuerdenDenIndexVerfaelschen` — der Gegenbeweis zum Rollen-Irrtum
    aus `parseTeamIds`: mit neu gelesenen IDs (3449 als „Zweitteam") fiele der Index auf 1.
  * `ohneZweitteamBleibtDerButtonAus` — `wechselMoeglich == false` ⇒ T21 rendert nichts.
  * 4 Tests für `teamwechselMeldung()`: Erfolg nennt den neuen Namen, ohne Namen eine
    neutrale Meldung statt „nichts", `Unveraendert` behauptet **keinen** Erfolg
    (`assertFalse(text.contains("aktiv"))`), `Fehler` wird wörtlich durchgereicht.
  ⚠️ Bewusst keine Tests für `wechsleTeam()` selbst: `AppViewModel` hängt an sechs
  konkreten Klassen (keine Interfaces) und die App hat **kein** `coroutines-test`, kein
  MockK, kein Robolectric (`build.gradle.kts` hat nur JUnit4 + org.json). Die Ablauf-Logik
  ist deshalb so geschnitten, dass ihre Bausteine einzeln testbar sind: `werteAus`
  (T11), `aktivesTeamAusHtml` (T09), `TeamwechselUiState`/`teamwechselMeldung` (hier).
  Suite: 406 Tests, 0 Fehler.

- [x] **T20 — Block 3 grün** · `S`
  `./gradlew :app:testDebugUnitTest --rerun-tasks` → grün (406/0, vollständiger Lauf ohne
  UP-TO-DATE-Rest), `:app:assembleDebug` grün (Hilt-Graph validiert). Commit erfolgt.

---

## Block 4 — Der Button in der TopAppBar

- [x] **T21 — Button in `AppRoot.kt`** · `M`
  **Erledigt** — `IconToggleButton` in `TopAppBar.actions`, **vor** dem Theme-`IconButton`
  (jetzt Zeile 334–347), Zustand via `appViewModel.teamwechsel.collectAsStateWithLifecycle()`
  in `MainScaffold`. Code wie im Plan, plus zwei Ergänzungen:
  * `enabled = !teamwechsel.laeuft` deaktiviert den Button **während** des laufenden
    Wechsels — der Wert wechselt ja erst danach (kein Optimismus), ein sichtbar aktives
    Toggle ohne Reaktion wäre sonst ein Bug-Signal.
  * `Modifier.semantics { contentDescription = … }`: ein nacktes `Text("1")`/`Text("2")`
    ist für TalkBack eine Zahl ohne Bedeutung. Neu sind dafür die Imports
    `IconToggleButton`, `semantics`, `contentDescription`.
  Der `!demo`-Zweig ist redundant zum ViewModel-Reset (Gast ⇒ `wechselMoeglich == false`),
  bleibt aber als Absicherung — die `TopAppBar` ist der einzige Ort mit Button-Zustand.
  ⚠️ Der Backstack wird hier **noch nicht** zurückgesetzt (T26), und die Schreibpfade sind
  noch nicht gesperrt (T33/T35) — der Button ist damit zwischen T21 und T26 nicht abnahmefähig.
  `:app:assembleDebug` grün, Suite 406/0.

- [x] **T22 — Snackbar / Meldung** · `S`
  **Erledigt** — `snackbarHost = { SnackbarHost(snackbarHostState) }` am `Scaffold` in
  `MainScaffold`, getriggert von `LaunchedEffect(teamwechsel.meldung)` +
  `SnackbarDuration.Short`. Kein Dialog.
  ⚠️ Warum Snackbar im Scaffold und nicht die bestehende seitenlokale Meldung
  (`TaktikScreen.kt:162`, `ZugabgabeScreen.kt:291` als Inline-`Text`): ab T26 springt die
  Ansicht nach dem Wechsel auf das Dashboard zurück, eine seitenlokale Meldung wäre dann
  weg. Der `SnackbarHostState` hängt am Scaffold, **über** dem `NavHost`, und überlebt die
  Navigation.
  Dazu `AppViewModel.teamwechselMeldungQuittiert()`: die Meldung wird nach dem Anzeigen
  geleert, sonst würde sie bei jeder Rekombination und nach einer Rotation erneut
  erscheinen. Das Leeren löst zugleich das Problem doppelter Schlüssel: zwei gleichlautende
  Fehlermeldungen hintereinander erscheinen beide, weil der Wert zwischendurch auf `null`
  läuft.
  ⚠️ Keine Erfolg/Fehler-Einfärbung: dafür bräuchte es ein Schweregrad-Feld im State (T17
  hat es nicht), und die Texte unterscheiden sich eindeutig. Bewusst weggelassen.
  Rotation mitten in der Snackbar: der Effekt wird abgebrochen, `showSnackbar` kehrt nie
  zurück, die Meldung bleibt stehen und erscheint nach der Rotation erneut — gewollt.
  `:app:assembleDebug` grün (ohne Warnungen), Suite 406/0.

- [x] **T23 — Sichtprüfung am Gerät** · `M`
  **Erledigt (3 von 4 Punkten bestätigt)** — MI 8, LineageOS, Android 15,
  `app-debug.apk` per `adb install -r` (Debug-Signatur identisch, App-Daten blieben
  erhalten). **Ein Login war nicht nötig**: `SessionManager.restore()` hat die Session über `lc`
  still wiederhergestellt und ist auf dem **Hauptteam** gelandet — weil der Server zu dem Zeitpunkt
  noch auf Team 1 stand, **nicht** weil ein Re-Login zurückgesetzt hätte. Diese Unterscheidung
  ist in T26a erst aufgefallen (bei gültigem Cookie wird nicht zurückgesetzt) und hat dort die
  falsche Prämisse von T31 korrigiert.
  Geprüft über `uiautomator dump` (Compose-Semantik: `text`/`content-desc`):
  - [x] Button erscheint **links** vom Darkmode-Button (`content-desc`-Knoten `[552,121][662,231]`,
        in der Reihenfolge zwischen Titel und „Theme wechseln")
  - [x] zeigt `1` (`Team 1 aktiv – zwischen Haupt- und Zweitteam wechseln`)
  - [x] Klick ⇒ `2` + Snackbar „Jetzt aktiv: NK Kamen Sesvete"; Dashboard danach
        **NK Kamen Sesvete / 2. Liga A Kroatien**, Gegner HFK Cakovec und N.K. Istra Rovinj,
        Zugababe „✗ ungültig" — Zweitteam-Daten, nicht nur eine andere Überschrift
  - [ ] **offen:** ~~Kontoauszug/Kontostand 7.242.629 €~~ **erledigt in T29.** Dort steht jetzt,
        dass es sich um den **Zweitteam**-Kontostand handelt (auf Team 1: 8.626.294 €).
  - [x] Zurück auf `1` ⇒ Snackbar „Jetzt aktiv: SC Viktoria Ulm", Dashboard wieder
        SC Viktoria Ulm / 2. Liga B Deutschland, Gegner Aachener BSC und Rheinisch Köln
  ⚠️ **Beobachtung für T26 (wichtig):** direkt nach dem Klick zeigte das sichtbare Dashboard
  noch ~2 s lang Team 1, erst der Rückweg auf den Tab zeigte Team 2. `DashboardViewModel` liest
  beim Tabwechsel neu, aber **nicht** live — die sichtbare Ansicht aktualisiert sich nicht von
  selbst. Genau das behebt T26 (Navigate + `popUpTo`).
⚠️ **Beobachtung, unabhängig von dieser Änderung:** die untere Leiste (`ResponsiveNavigationBar`)
   blieb bei den ersten programmatischen Wischgesten stehen — „Transfers" nur halb sichtbar,
   „Statistiken" und „Verein" unerreichbar.
   **In T29 aufgelöst (und meine Zwischenkorrektur aus T27 war selbst falsch):** Die Leiste hat
   8 Tabs, zeigt im Hochformat ein **Fenster von 5** und scrollt zum gewählten Tab — man sieht
   also je nach Route einen anderen Ausschnitt, nicht „zu wenig". Sie ist aber sehr wohl
   wischbar: `adb shell input swipe 900 2150 100 2150 400` (y ≈ 2150, Dauer ≈ 400 ms) schiebt
   sie zuverlässig und macht „Verein" sichtbar. Die frühere Vermutung einer **Klemme** in
   `AppRoot.kt:911-940` ist damit **weder bestätigt noch widerlegt** — sie war nie die Ursache;
   kein eigener Bug nachzutragen. Querformat wäre die Alternative gewesen, lässt sich auf
   diesem Gerät aber nicht erzwingen (`user_rotation` wird ignoriert).
   ⚠️ Die visuelle Hervorhebung des aktiven Werts (T21 „aktiver Wert hervorgehoben") ließ sich
   nicht automatisiert prüfen: Compose füllt im A11y-Baum `checked`/`clickable` nicht
   (der Knoten erscheint als `android.widget.CheckBox` mit `checked=false`, obwohl der Klick
   wirkt). Bitte einmal mit dem Auge prüfen.

- [x] **T24 — Block 4 grün** · `S`
  `./gradlew :app:assembleDebug` grün, Suite 406/0, Gerätetest T23 durch (ein Punkt dort
  offen, von T29 abgedeckt). Commit erfolgt.

---

## Block 5 — Zustands-Reset (der wichtigste Block)

> Ohne diesen Block zeigt die App nach dem Wechsel **Daten des falschen Teams**.
> `popUpTo` allein reicht nicht: `Routes.TEAM` ist eine Root-Destination ohne `saveState`-Trennung,
> und `hiltViewModel()` lebt pro `NavBackStackEntry` — die VMs überleben einen Tabwechsel.

- [x] **T25 — `DashboardRepository.invalidate()` im Wechselpfad** · `S`
  **Erledigt — Prüfaufgabe, kein Code geändert.** Ergebnis: **kein** Pfad kann den Cache nach
  dem `invalidate()` mit Team-1-Daten befüllen. Beweis in zwei Punkten:
  1. Der **einzige** Schreibvorgang auf `cacheDatensatz` liegt in `fetchDashboard`
     (`DashboardRepository.kt:46-48`), unter dem Mutex, und der Wert stammt **immer** aus
     `ladeVonServer()` — er wird nie aus dem alten Cache kopiert. Eine Neubefüllung kann also
     nur Daten enthalten, die der Server **in diesem Moment** geliefert hat.
  2. Der Server ist bereits in Schritt 3 umgeschaltet, `invalidate()` läuft in Schritt 4.
     Jeder spätere Load liefert folglich Team 2.
  Ein bereits laufender Fetch kann nicht dazwischenfunken: `invalidate()` und `fetchDashboard`
  teilen sich denselben Mutex (`:24`), der laufende Fetch gibt ihn also **vor** dem
  Invalidate frei. Damit ist bestätigt, was der Plan bereits vermutet — **kein** Lost-Update.
  Weitere In-Memory-Caches: keine. `grep` über `data/repository/` findet nur
  `DashboardRepository.cacheDatensatz`; `FreundschaftRepository.endpoint` ist eine einmalig
  ermittelte URL, keine Teamdaten.
  ⚠️ **Fund bei den persistenten Stores** (alle vier `getSharedPreferences` geprüft):
  - `ThemeState` — teamunabhängig, unkritisch.
  - `StadionnameStore` — strikt je Team (`v1_name_<teamId>`, `lesen(0)` liefert `null`).
    Die Vermutung aus T28 ist damit **belegt**.
  - `ElfDesSpieltagsRepository` — **echter, wenn auch geringer Befund**, siehe unten.
  ⚠️ **`ElfCache.schluessel()` ist bei `teamId == null` nicht team-skopiert** (`ElfCache.kt:177`
  schreibt `"-"`). Die `teamId` stammt aus `ladeEigeneTeamId()` =
  `dashboardRepository.fetchDashboard().teamId` und ist `null`, wenn das Dashboard nicht
  ladbar ist. Für *diesen* Account ist eine Kollision unmöglich, weil die Schlüssel zusätzlich
  `landId`/`ligaId` enthalten und die Teams in verschiedenen Ligen spielen (2. Liga B
  Deutschland vs. 2. Liga A Kroatien). Für ein Konto, dessen beide Teams in **derselben**
  Liga/Saison spielen, könnte bei ausgefallenem Dashboard eine Team-1-Aufstellung als Team-2
  geliefert werden. **Bewusst nicht in T25 behoben** (Bestandsproblem, außerhalb des Plans):
  sauber wäre „bei `teamId == null` gar nicht cachen" oder `leereCache()` im Wechselpfad.
  → Bei T29 prüfen, ob der Bereich „Elf des Spieltags" nach dem Wechsel überhaupt erreicht wird.

- [x] **T26 — Backstack-Reset auf Start-Destination** · `M`
  **Erledigt** — die Navigation steht wörtlich wie im Plan in `AppRoot.kt`
  (`MainScaffold`), getriggert von einem neuen Einmal-Signal
  `AppViewModel.teamwechselAusgefuehrt`:
  ```kotlin
  navController.navigate(Routes.DASHBOARD) {
      popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
      launchSingleTop = true
  }
  ```
  `inclusive = true` + Start-Destination selbst entfernen und neu aufsetzen ⇒ **alle**
  `NavBackStackEntry`s werden zerstört, damit alle `NavBackStackEntry`-ViewModels sterben.
  Betrifft automatisch: `TeamViewModel` (Lazy-Guards Zeilen 114,129,144,159,188,203,218,233),
  `ZatViewModel:36`, `ZuzuViewModel:39`, `BewerbeViewModel:39` (`eigeneTeamId`),
  `SpieleViewModel`, `SeiteViewModel`, `SpielberichtViewModel`.
  **Kein** `saveState`/`restoreState` über den Wechsel hinweg.
  Warum ein Einmal-Signal und kein State-Feld: `teamId` ändert sich auch beim Anmelden —
  ein beobachteter State hätte dort eine unnötige Navigation ausgelöst. `MutableSharedFlow`
  mit `replay = 0` und bewusst **nicht** als Kanal: wer nicht zuhört, soll das Signal nicht
  später bekommen, sonst würde es nach einer Rotation noch einmal navigieren. Gesendet wird
  nur bei `TeamwechselErgebnis.Erfolgreich`, und **erst nach** dem Zustandsschreiben — so
  liest der neue `DashboardViewModel` schon den aktualisierten Cache. Bei einem Fehler
  bleibt der Nutzer, wo er ist. Nebenbei `dashboardRefreshTrigger = 0` zurückgesetzt, damit
  der neue `DashboardViewModel` nicht sofort einen überflüssigen Refetch anstößt.
  ⚠️ **Gerätetest bestätigt die T23-Beobachtung als behoben:** Wechsel aus dem Team-Tab
  heraus ⇒ landet direkt auf dem Dashboard mit Team 2, **keine** 2 s mit Team 1 mehr. Die
  Snackbar überlebt die Navigation (Scaffold über dem `NavHost`). **Backstack-Beweis:** ein
  anschließendes BACK verlässt die App — es gibt also nur noch das Dashboard im Stack.
  ⚠️ **Dabei ein echter Bug gefunden und in T26a behoben** (siehe dort).
  Suite 409/0, `:app:assembleDebug` grün.

- [x] **T26a — Fund aus dem T26-Gerätetest: `zweitTeamId` rollenkorrekt ableiten** · `M`
  **Erledigt** — im T26-Test fiel auf, dass nach **Back + Neustart** (gültiges Cookie ⇒ kein
  Neu-Login ⇒ Server bleibt auf Team 2) das Dashboard korrekt „NK Kamen Sesvete" zeigte, der
  Button aber „Team 1 aktiv". Ursache: `showteam.php?s=0` ist **rollenrelativ** — die Seite zeigt
  das gerade aktive Team, und der Partner-Anker heißt dann „Mein **Haupt**team" (→ 3449). T13
  hat diese Rolleninformation weggeworfen, `zweitTeamId` war damit faktisch „die andere ID"
  statt „die ID des Zweitteams". Die T18-Notiz („nicht nach einem Wechsel neu lesen") griff zu
  kurz: `ladeTeamIds()` läuft auch beim Kaltstart, und genau dort ist das Problem aufgetreten.
  Der Ankertext ist die **einzige** Rolleninformation der ganzen Seite — die Fix ist deshalb
  zwangsläufig, eine Ausweichroute gibt es nicht (`haupt.php` nennt nur „Zu X wechseln", ohne
  Rolle; `showteam.php?s=1` ist nicht eindeutig „das Zweitteam").
  Umsetzung ohne Persistenz und ohne Verstoß gegen Regel 1 (der aktive *Index* kommt weiter
  allein aus dem Serverbefund):
  - `TeamIds`: `hauptTeamId`/`zweitTeamId` → **`teamId`/`partnerTeamId`** (rollenfrei benannt) +
    neues Flag **`partnerIstHauptteam`**. Der Umbenennung kommt mehr als Kosmetik zu: die alten
    Namen haben die falsche Annahme bereits kodiert.
  - `parseTeamIds`: merkt sich, welcher der beiden Ankertexte gegriffen hat.
  - `zweitTeamIdAus(ids)` (neu, `internal` und top-level wie `teamwechselMeldung`, damit ohne
    Netz testbar): festes Zweitteam = Partner, **außer** der Server nennt ihn „Mein Hauptteam",
    dann ist das eigene Team das Zweitteam. `null` bei fehlendem/gleichem Paar ⇒ kein Button.
  - `ladeTeamIds()` ruft nur noch diese Funktion; `teamId` ist der 1:1-Befund des **aktiven**
    Teams und damit weiter der Vorzustand für den Vergleich in `wechsleTeam()`.
  Tests: 2 Parser-Tests auf das neue Flag umgestellt, `teamIds_festesZweitteamIstInBeiden
  RollenlagenGleich` (beide Fixtures ⇒ 1216) neu; im ViewModel-Test den obsoleten Test
  `neuErmittelteTeamIdsWuerdenDenIndexVerfaelschen` (fror den alten Fehler als Erwartung ein)
  durch drei Tests ersetzt, darunter die Kaltstart-Regression. Suite 406 → **409/0**.
  **Gerätetest:** Kaltstart mit aktivem Team 2 ⇒ Button „Team 2 aktiv", nach Klick zurück auf
  Team 1 ⇒ „Team 1 aktiv". Vor dem Fix stand hier nach demselben Ablauf „Team 1 aktiv" bei
  Team 2 im Dashboard. Damit ist zugleich klar, dass **T31s Erwartung** („Button steht nach
  App-Start wieder auf 1") **nur bei einem echten Neu-Login** gilt: bei gültigem Cookie
  startet die App auf Team 2 — das ist der Serverzustand, kein App-Bug.

- [x] **T27 — `BewerbeViewModel.eigeneTeamId` nicht cachen** · `S`
  **Erledigt — kein Code geändert, nichts zu tun.** `eigeneTeamId` (`:39`, `:72-74`),
  `tabelleFilter`, `spieltagLiga/Land/Saison/Zat`, `pokalLand/Saison/Runde` und die Flags
  `pokalJob`/`pokalVersucht` sind **ausnahmslos** `private var`-Felder desselben
  `BewerbeViewModel`. Dieses wird per `hiltViewModel()` **innerhalb** von
  `composable(Routes.BEWERBE)` (`AppRoot.kt:737`) bezogen, ist also an genau diesen
  `NavBackStackEntry` gebunden und stirbt mit T26s `inclusive`-Reset samt allen Feldern. Ein
  explizites Nullen wäre doppelt und nur Kosmetik.
  ⚠️ **Die Vorbedingung aus der Planzeile gilt erfüllt, aber nicht wegen der Route, sondern
  wegen der `saveState`-Frage:** `Routes.BEWERBE` **ist** ein `BottomTab` (`AppRoot.kt:891`).
  Die App benutzt aber **nirgends** `saveState`/`restoreState` (0 Treffer im ganzen
  `main/`-Quellbaum), der Tabwechsel ist `popUpTo(start) { }` + `launchSingleTop` — es gibt
  also keinen Zustand, der den ViewModel über einen Reset hinweg retten könnte. Die
  `rememberSaveable`-Werte in `BewerbeScreen` (`bereich`, `spieltageUnteransicht`,
  `ergebnisseSichtbar`, `pokalErgebnisseSichtbar`) liegen in der `SavedStateRegistry`
  desselben Eintrags und fallen damit ebenfalls weg — sie sind ohnehin reine UI-Schalter
  ohne Teambezug.
  **Gerätetest (der eigentliche Nachweis, end-to-end über T26):** Nationale Bewerbe auf
  Team 1 ⇒ **2. Liga B / Deutschland / Saison 24**. Wechsel direkt aus diesem Tab heraus ⇒
  T26 landet auf dem Dashboard, erneuter Besuch ⇒ **2. Liga A / Kroatien / Croatia
  Visnjan**. Also: die Kontext-Vorbelegung folgt dem neuen Team. Gegenprobe am Pokal-Zweig
  (`pokalLand`/`pokalSaison`, von T27 ausdrücklich genannt): Team 2 ⇒ „Keine Pokalrunden
  gefunden.", Team 1 ⇒ „Saison 24 · Runde 1 / Deutschland / 1. Runde / Eintracht Wuppertal
  vs. Augsburger SSV". Auch dieser Zweig wird neu abgeleitet statt mitgeschleppt.
  Damit sind alle vier im ViewModel gecachten Zustände empirisch widerlegt.
  ⚠️ **Nebenbefund, wichtig für T29/T30 — hier war ich noch auf dem falschen Gleis:** die
  untere Leiste ist **routenabhängig** (Dashboard: `Dashboard | Team | ZAT | Nationale Bewerbe
  | Intern. Bewerbe`; anderswo: `ZAT | Nationale Bewerbe | Intern. Bewerbe | Transfers |
  Statistiken`), und ich folgerte daraus „die Tabs waren immer da, es waren nur andere — die
  T23-Notiz sei eine Fehldiagnose". **Das war falsch**, T29 hat es richtiggestellt: es ist ein
  **Fenster von 5 Tabs bei 8 insgesamt**, das zusätzlich gewischt werden muss
  (`input swipe 900 2150 100 2150 400`). Die hereingeschriebenen Folgerungen für die
  Geräteschritte bleiben aber gültig: Tab-Positionen **immer** aus einem frischen
  `uiautomator dump` lesen, nie annehmen — mein Blindkoordinaten-Tipp landete dadurch auf
  „Transfers" statt „Nationale Bewerbe". Vollständige Auflösung in T29.
  ⚠️ Und: beim App-Start blendet der Dialog „Einer deiner Jugendspieler …" die komplette
  Ansicht (nur 3 Texte im Dump, keine Tabs). Vor jedem Dump wegsortieren, sonst wertet man
  den nächsten Schritt auf einem leeren Bild aus.

- [x] **T28 — `StadionnameStore` prüfen** · `S`
  **Erledigt — kein Code geändert, unkritisch wie im Plan vermutet.** Belege:
  - **Store:** `nameKey = "v1_name_$teamId"`, `aktivKey = "v1_aktiv_$teamId"`, und **beide**
    `lesen` (`:19`) und `speichern` (`:33`) lehnen `teamId <= 0` ab. Es gibt also keine
    teamneutrale Sammelkey, in den Team 1 etwas für Team 2 ablegen könnte. Weil Name **und**
    Aktivitätszeitstempel pro Team gequotet sind und `lesen` einen Zeitstempel `<= 0`
    zurückweist (`:23`), kann auch ein halb geschriebener Eintrag nicht zurückgelesen werden.
  - **Herkunft der `teamId` (die eigentliche Frage des Schritts):** beide Aufrufstellen
    beziehen sie aus `dashboardRepository.fetchDashboard(forceRefresh = true).teamId` — also
    aus einem **erzwungenen** Refetch, nie aus einem Cache-Feld:
    `SeiteViewModel.kt:100-102` (Lesen + Schreiben) und `SpielberichtViewModel.kt:50-52`
    (nur Lesen). Zusätzlich `?.takeIf { it > 0L }`, und ohne `teamId` wird gar nicht gelesen
    bzw. `ladeFehler` gesetzt statt ein falscher Key zu beschreiben.
  - **Lesen und Schreiben benutzen denselben Key:** `stadionnameSpeichern()` schreibt unter
    `stadionname.teamId`, also unter der beim Laden ermittelten ID.
  - **Zusätzliche Absicherung in `StadionnameLogik.anzeigename()` (`Stadionname.kt:43-44`),
    doppelt und unabhängig von der Key-Quotierung:** `gespeichert?.teamId != teamId` ⇒ null
    (fremder gespeicherter Name wird nie angewandt) und `bericht.heimId != teamId` ⇒ null
    (der Override erscheint nur im Spielbericht des **eigenen** Teams als Heimmannschaft —
    nie im Bericht eines Fremdvereins). Das ist genau die Eigenschaft, die T30 braucht.
  ⚠️ **Die eine Stelle, an der ein stehengebliebener Teambezug denkbar wäre**, ist
  `stadionnameSpeichern()` (`:141`, `teamId` aus dem Ladezustand statt frisch geholt). Sie ist
  trotzdem sicher, aber **nur dank T26**: die TopAppBar liegt über allen Routen, ein Wechsel
  zerstört `SeiteViewModel` samt Zustand, und ohne Teamwechsel kann sich der Serverteam unter
  einer offenen Seite nicht ändern. Das ist eine Abhängigkeit, die im Plan nirgends notiert
  war — hiermit ist sie festgehalten; fiele T26 später weg, wäre `stadionnameSpeichern()` die
  erste Stelle, die nachziehen müsste.
  Für **T29/T30** ist der Stadionname ein sichtbares Feature (eigener Anzeigename im
  Spielbericht und auf der Vereinsseite) und gehört mit auf die Prüfliste.

- [x] **T29 — Gerätetest: alle Bereiche** · `M`
  **Erledigt — 22 von 23 Bereichen geprüft, kein einziger zeigte Team-1-Werte auf Team 2.**
  Der eine offene Punkt ist unten benannt und an T33 abgegeben. **Kein Code geändert.**
  ⚠️ **Methode (aus den Fehlalmen dieses Schritts gelernt, bitte für T30/T31 übernehmen):**
  1. Nach **jedem** `adb input tap` neu dumppen, auch nach `KEYCODE_BACK` — sonst arbeitet man
     auf einem zwei Schritte alten Dump und „nicht gefunden" ist eine Falschmeldung.
  2. Tab-Positionen **niemals annehmen**, sondern aus dem Dump lesen (`>2000` = untere Leiste).
  3. Nicht auf Labels tippen, sondern auf den **klickbaren Vorfahren** — Compose-Labels sitzen
     mehrere Pixel neben ihrem Tippbereich, Labels ohne klickbaren Vorfahren (z. B. „ZAT
     wählen …") gar nicht.
  4. Vor jedem Dump den Dialog „Einer deiner Jugendspieler …" wegsortieren.
  5. **Ein Tipp ist gelegentlich ein Fehlgriff** (ohne Fehlermeldung, Dump zeigt denselben
     Screen) — bei „Tipp wirkte nicht" einmal wiederholen, bevor man einen Befund meldet.
  6. ⚠️ **Ein Tipp direkt nach `uiautomator dump` wird verschluckt** (T38a, 3 von 4 Tips
     auf Anhieb stumm). Deshalb **~1 s zwischen Dump und `input tap`** warten — nicht dumppen
     und sofort tippen. Sonst hält man einen Bedienfehler für einen App-Fehler; genau so sah
     es sich zunächst aus („Tipp ohne jede Wirkung").
  ⚠️ **Die untere Leiste: T23s Beobachtung war richtig, meine T27-Korrektur war falsch.**
  `ResponsiveNavigationBar` (`AppRoot.kt:899-960`) hat **8** Tabs, zeigt im Hochformat ein
  Fenster von 5 (`screenWidthDp 393 / 72 = 5`) und **scrollt zum gewählten Tab**. Der
  Bildschirm zeigt also je nach Route einen **anderen Ausschnitt** — das erklärt T23s
  „Transfers halb sichtbar, Statistiken/Verein unerreichbar". Zusätzlich ist die Leiste per
  Wischgeste scrollbar; die entscheidende Rüste sind **y ≈ 2150 und Dauer ≈ 400 ms**
  (`adb shell input swipe 900 2150 100 2150 400`), damit wird „Verein" sichtbar. Querformat
  hilft **nicht**: `settings put system user_rotation` wird auf diesem Gerät ignoriert
  (bleibt ROTATION_0), und `wm set-user-rotation` existiert nicht.
  **Geprüft auf Team 2** (T2-Marker: „Kamen", „2. Liga A", „Kroatien", „Cakovec", „Istra";
  T1-Marker: „Viktoria", „2. Liga B", „Aachener", „Köln"):
  - **Dashboard** NK Kamen Sesvete / 2. Liga A Kroatien, Gegner HFK Cakovec, „✗ Zugababe
    ungültig" ✓
  - **Mannschaft** 31 Spieler, kroatische Namen (Pat Merson, Panu Rinne, Matija Brozovic,
    Metodi Pentchev…) ✓
  - **Training** (Trainer-Betreuung, 5/4 Spieler), **Trainer** (Stab, 60.000 Euro Monatslöhne),
    **Taktik-Editor** (0 Positionen belegt, keine gespeicherten Taktiken) ✓
  - **Verträge/Stärken/Statistiken** am Spielerkarten-Beispiel *Pat Merson* (Skill 45.2, Vertrag
    78.888, Laufzeit 24 ZAT, Marktwert 8.981.270) ✓
  - **Vertragsdaten** des Vereins: TOR Matija Brozovic (Gehalt 3.500, Laufzeit 20, Wert
    921.415) und Panu Rinne (35.357) ✓
  - **ZAT/Zugababe** ZAT 10, „✗ Ungültig" ✓ — und der **ZAT-Report ZAT 9** als Beleg:
    „2. Trainingserfolge: TOR Matija Brozovic / Panu Rinne" = genau die Team-2-Torhüter ✓
  - **Spiele/Matchcenter** „HFK Cakovec 0:0 NK Kamen Sesvete", ZAT 10 ✓
  - **Nationale Bewerbe** Ligatabelle 2. Liga A / Kroatien / Croatia Visnjan ✓,
    **Landespokal** „Keine Pokalrunden gefunden." (Gegenprobe Team 1 siehe unten) ✓
  - **Internationale Bewerbe** (OS-/Club-Ranking) ohne Teambezug, kein Fremd-Datenleck ✓
  - **Statistiken** Topscorer in **2. Liga A Kroatien** (Croatia Visnjan, Lokomotive Zadar …) ✓,
    Top-Teams global (GRE/BIH/SVN), kein Team-1-Anteil ✓
  - **Transfers** Hub, **Transferliste** (globaler Spielermarkt, 1324 Spieler — konstruktions-
    bedingt nicht teamgebunden), **Leihspieler Übersicht** („Keine verliehenen oder geliehenen
    Spieler") ✓
  - **Verein** komplett: **Kontoauszug** Kontostand **7.242.629** (siehe unten!), **Steuer-
    übersicht** Saisonstart 5.665.656 + Zwischensumme 1.576.973 = 7.242.629 (rechnet sich),
    **Jugendteam** Jahrgang 19, 18 Jahre, **CRO**-Flaggen, **Jugendscouting** (Angebote),
    **Stadionausbau** 44.000 Plätze „Kompakt-Stadion · Kasten", **Teamübersicht (Website)**
    Kader Matija Brozovic / Panu Rinne ✓ — die 44.000 Plätze passen zu den Stadionkosten
    −176.000 im ZAT-Report, Querprobe stimmt.
  - **Teaminformationen** komplett: **Saisonplan** (NK Varos Varazdin auswärts, SK Croatia
    Vrbovec, Dalmacija Sibenik), **Saisonhistorie** („Liga 2. Liga A, 10. Platz (13 Punkte)"),
    **Vereinshistorie** (291 Einträge, Saison 24, 6 ZAT, **∑Spieler 31**, ØSkill 36.63),
    **Transferhistorie** („**NK Kamen Sesvete** → Kap Hoorn / Kresimir Budisa, Bargeld
    210.021"), **Leihhistorie** („Rasa Šležas · Von: ASS Genua · **Zu: NK Kamen Sesvete**"),
    **Statistik Saison/Gesamt** („31 Spieler · 24 Statistikwerte"), **Einzelwerte**
    („31 Spieler · 17 Einzelwerte", Matija Brozovic, Panu Rinne, Pat Merson, Ludek Baba),
    **Teaminfo** (44.000 Plätze) ✓
  **Gegenprobe auf Team 1** — sie macht die Aussage erst belastbar, denn „keine Team-1-Werte"
  wäre sonst vacuous. Vier Seiten liefern **nachweislich verschiedene** Werte, und die App
  zeigte jeweils die passende Menge:
  | Seite | Team 1 | Team 2 |
  |---|---|---|
  | Kader (Einzelwerte/Vereinshistorie) | **35** Spieler (Lars Vincez, Heiko Kehrer, Kuldar Mitt …) | **31** Spieler |
  | Saisonhistorie | 2. Liga **B**, 4. Platz (61 P.) | 2. Liga **A**, 10. Platz (13 P.) |
  | Kontoauszug Kontostand | **8.626.294** (ZAT-9-Eingang 238.500) | **7.242.629** (Eingang 2.242.629) |
  | Transferhistorie | **SC Viktoria Ulm** → Dschibuti, 1.214.361 | **NK Kamen Sesvete** → Kap Hoorn, 210.021 |
  ⚠️ **Der offene Punkt aus T23 ist damit erledigt — und die Zahl im Plan war falsch
  zugeordnet:** 7.242.629 € ist der **Zweitteam**-Kontostand. Auf Team 1 sind es **8.626.294 €**,
  es sind also zwei **verschiedene** Konten. Die Prüfung „Kontostand 7.242.629" galt
  unbemerkt für Team 2 und wäre auf Team 1 nie erfüllbar gewesen. Wer den Wert später als
  Sollgröße nutzt, muss das Team dazuschreiben.
  ⚠️ **Offen bleibt genau ein Bereich: die Aufstellung im ZAT-Screen** (`zugabe.php`). Der
  Bildschirm selbst ist teamkorrekt (ZAT 10, „✗ Ungültig" wie im Dashboard, Zugababe gültig/
  ungültig konsistent), aber das **Aufstellungs-Menu „ZAT wählen …" lässt sich per adb nicht
  öffnen**: der Compose-`ExposedDropdownMenu` liegt in einem Popup-Fenster, das im
  `uiautomator`-Dump **nicht** auftaucht (der Dump zeigt dann exakt 1 Text), und Blindtöpfe
  landen auf dem Scrim und schließen es wieder. Der Bereich ist damit **nicht** visuell
  geprüft. Er wird an **T33** abgegeben: dort wird der ZAT-Screen ohnehin samt Aufstellung
  untersucht, und dann mit echtem Finger statt mit adb.

- [x] **T30 — Gerätetest: Fremdverein** · `S`
  Auf Team 2 (NK Kamen Sesvete, 2. Liga A Kroatien), Ligatabelle → Klick auf **Lokomotive
  Zagreb** ⇒ `Routes.VEREIN` zeigt **den Fremdverein**: Kopf „Lokomotive Zagreb / 2. Liga A",
  „Kader · 32 Spieler", Spieler mit Position und Alter (Gareth Harlan, Torwart · 23 Jahre).
  **Kein einziges Vorkommen von „Sesvete" auf dem Bildschirm** — der eigene Verein
  leckt nicht in die Fremdverein-Ansicht. Der `1|2`-Button bleibt sichtbar und zeigt
  **„Team 2 aktiv"**: er zeigt den *eigenen* Serverstand, nicht den Fremdverein.
  **Zweiter Teil der Zusage — „der Button betrifft die Fremdverein-Ansicht nicht":** den
  Schalter **auf der Fremdverein-Ansicht selbst** gedrückt ⇒ Wechsel auf Team 1 lief
  durch, und wegen des Backstack-Resets (T26) landete die App auf der Start-Destination:
  Dashboard mit SC Viktoria Ulm, 2. Liga B, „✓ Zugababe gültig", **keine Reste des
  Fremdvereins** (weder Zagreb noch Cakovec im Dump). Genau das ist der gefährliche
  Fall: ohne Reset stünde eine Fremdverein-Ansicht mit Team-1-Daten im Backstack.

- [x] **T31 — Gerätetest: App-Neustart (Fall 1 am Gerät geprüft)** · `S`
  Auf Team 2, App schließen, öffnen ⇒ **der Serverzustand bestimmt den Button**, es gibt zwei
  Fälle, und die Prämisse dieses Schritts war nur für einen davon richtig:
  1. **Cookie noch gültig** (`verifySession()` erfolgreich, kein Neu-Login) ⇒ der Server bleibt
     auf Team 2, die App startet auf Team 2, der Button steht korrekt auf `2`. Live bestätigt
     in T26/T26a. Kein Fehler — der Server hat nie zurückgesetzt.
  2. **Session abgelaufen** ⇒ es wird neu angemeldet, und **das** setzt serverseitig auf das
     Hauptteam zurück ⇒ Button `1`, Dashboard Team 1. Genau das ist „gewollt".
  Zu prüfen ist also nicht „steht er wieder auf 1", sondern **stimmt der Button mit dem Team
  überein, das das Dashboard zur selben Zeit anzeigt** — in beiden Fällen `1|2` == Dashboard.
  (Vor T26a war dieser Schritt nicht durchführbar: der Button zeigte nach Fall 1 „1" bei
  Team 2 im Dashboard.)
  **Fall 1 am Gerät, live:** Server stand auf Team 2 (Button „Team 2 aktiv"), dann
  `am force-stop` + Kaltstart ⇒ **kein** Re-Login, Dashboard **NK Kamen Sesvete**,
  Button **„Team 2 aktiv"**. `1|2` == Dashboard, wie gefordert.
  **Fall 2 nicht prüfbar** — er verlangt eine abgelaufene Session, also das Löschen der
  App-Daten bzw. eine Anmeldung mit Zugangsdaten, die hier nicht vorliegen. Bewusst **nicht**
  simuliert: `pm clear` hätte den einzigen Login des Geräts zerstört. Fall 2 ist nicht
  nur Behauptung, sondern die dokumentierte Folge eines serverseitigen Verhaltens
  (stiller Re-Login setzt auf das Hauptteam zurück) — er wird in **T41** mitgeprüft,
  sobald ohnehin eine Anmeldung durchgeführt wird.
  ⚠️ **Methodenbefund für alle weiteren Gerätetests:** auf dem Team-Screen sitzt der
  `1|2`-Schalter **oben in der Topbar** (`y≈176`), die Tabs darunter (`y≈342`). Ein
  Blindtipp auf den Team-Screen kann damit den **Schalter** treffen statt eines Tabs —
  das ist in diesem Gerätetest passiert und hat den Server unbemerkt auf Team 2
  geschaltet. Vor T30 war der Server deshalb unerwartet auf Team 2. Es gibt keinen
  zweiten Schreibpfad: `teamwechselDurchfuehren()` wird projektweit **ausschließlich** aus
  dem einen `onCheckedChange` in `AppRoot.kt:379` aufgerufen.

- [x] **T32 — Block 5 grün** · `S`
  Gerätetest komplett grün (Dashboard, Tabs T29, Fremdverein T30, Neustart Fall 1 T31, keine Team-1-Daten nach Wechsel sichtbar). Commit erfolgt.

---

## Block 6 — Schreibpfade schützen (Datenverlust-Risiko)

> ⚠️ **Höchstes Risiko im ganzen Plan.** Wenn `speichern()` mit PIDs aus Team 1 läuft,
> schreibt die App **Team-1-Inhalt in Team 2**. Das ist Datenbeschädigung auf einem
> fremden Spielerserver, kein Kosmetikfehler.

- [x] **T33 — Toggle bei offener Zugababe sperren** · `M`
  **Der Zustand war da, die Wirkung fehlte.** `ZugabgabeViewModel` meldet über
  `OffeneAenderung.oeffnen(AenderungBereich.ZUGABABE)` bereits, dass eine Änderung offen
  ist, und `bestaetigungstext()` erzeugt sogar den passenden Satz — **nur hatte diese
  Funktion keinen einzigen Aufrufer**, und in `AppRoot` gab es keinen Dialog. Effekt: der
  1|2-Button hat ungespeicherte Änderungen stillschweigend verworfen, genau der
  Datenverlust, den dieser Block verhindern soll. (`dirty` wird in der UI bis heute nicht
  angezeigt — sieht man am Gerät nichts; nur der Dialog sagt Bescheid.)
  **Umsetzung (dieser Schritt):** `TeamwechselUiState.offeneBereiche` (Spiegel von
  `OffeneAenderung.offen`, im `AppViewModel` per `collect` nachgeführt) und
  `TeamwechselUiState.bestaetigung` (nur durch einen Tipp gesetzt — sonst erschiene der
  Dialog schon beim Öffnen eines Screens). Neuer Ausgang `WechselAktion.Bestaetigen(text)`,
  `wechsleTeam(bestaetigt: Boolean = false)`: der erste Tipp stellt nur den Dialog,
  `onWechseln` startet mit `bestaetigt = true`. Dialog in
  `ui/TeamwechselBestaetigungDialog.kt`, eingehängt in `MainScaffold` **außerhalb** des
  `Scaffold` — er muss den Backstack-Reset (T26) überleben.
  ⚠️ **Reihenfolge der Prüfung:** `laeuft` → `gesperrt` → `offeneBereiche`. Ein Dialog
  während der Wechselsperre oder eines laufenden Wechsels führte ins Leere.
  **Gerätetest:** eine Rasterzelle im Taktik-Editor umgeschaltet (= dirty), dann 1|2
  gedrückt ⇒ **kein** Toggle, Dialog „Ungespeicherte Änderungen / Taktik offen — wirklich
  wechseln? Nicht gespeicherte Änderungen gehen verloren." mit „Abbrechen" und „Trotzdem
  wechseln". „Abbrechen" ⇒ Dialog zu, **weiter Team 1**, Bildschirm unverändert.
  „Trotzdem wechseln" ⇒ Wechsel auf Team 2, Backstack-Reset aufs Dashboard, Sperre
  („Teamwechsel in 2 s möglich") — danach zurück auf Team 1. **Erledigt** (Dialog + State + Tests).

- [x] **T34 — Toggle bei offener Taktik sperren** · `M`
  Gleiche Behandlung, anderer Bereich: `TaktikViewModel` registriert über
  `AenderungBereich.TAKTIK`, derselbe Dialog deckt beide ab (`"Zugababe und Taktik
  offen — …"`, falls beide offen sind; die Bereichsnamen kommen sortiert aus
  `bestaetigungstext`). **Gerätetest:** genau dieser Fall oben — der Taktik-Editor war der
  Bereich, an dem dirty ausgelöst wurde.

- [x] **T35 — Generation-Gate für alle Writer** · `M`
  ⚠️ **Der Plan war hier falsch und ist es noch:** die Zeilennummern und zwei der
  genannten Stellen existieren nicht mehr. `ZatEditorViewModel.loescheMarkerte` gibt es
  nicht (der ZAT-Editor delegiert an `ZugabgabeViewModel`), `TaktikViewModel` liegt in
  `feature/taktik/`, nicht in `feature/team/`. Wer die Liste abhakt, hält sich für fertig,
  obwohl drei Writer ungeschützt sind.
  **Der Kern war bereits gebaut** (`core/state/TeamGeneration.kt`, `schreibvorgangErlaubt`,
  `increment()` in `AppViewModel`), aber nur an **zwei** von vier ViewModels verdrahtet.
  **Lücken, die dieser Schritt geschlossen hat** (alle drei sind Schreibvorgänge mit
  **eigenem** Team-Content):
  - `ZugabgabeViewModel.loescheKader()` → `loescheKaderAufstellung()`: löscht die Aufstellung
    des **aktiven** Teams. Nach einem Wechsel hätte es die Aufstellung des *neuen* Teams
    geleert.
  - `TaktikViewModel.loescheGewaehlte()` → `loescheTaktik(id)`: `gewaehlteEigeneId` stammt
    aus der Taktikliste des alten Teams — gelöscht hätte es die fremde Taktik.
  - `TransferStatusViewModel.bestaetigen()` → `transferStatusSpeichern(zeilen)`: der POST
    trägt die **ganze Statusliste** der eigenen Spieler (`tstatus.php`). Das war der
    gravierendste Fall: kein Gate, `TeamGeneration` gar nicht injiziert.
  **Bewusst *nicht* gegatet:** Gebote/Aktionen auf Markt- oder fremde Spieler
  (`gebot.php`, `vmgebot.php`, `vmsetzen.php`), Freundschaftsspiel-Aktionen und
  PM-Versand. Diese Requests tragen keine **eigenen** Spieler-IDs, also kann dort kein
  Team-1-Inhalt im Team-2-Request landen; sie hätten nur Nutzer-Aktionen blockiert, die
  nach einem bewussten Wechsel völlig legitim sind. Diese Trennung ist eine
  **Bewertung**, keine Messung — sie steht hier, damit sie jemand prüfen kann.
  **Warum zusätzlich zur UI-Sperre:** die UI-Sperre greift nicht   für Hintergrund-Coroutines. **Erledigt** (Zugabgabe, Taktik, TransferStatus gegatet).

- [x] **T36 — Gate-Tests** · `M`
  `core/state/TeamGenerationTest.kt` (war vorhanden): gleiche Generation erlaubt,
  verschiedene verweigert — **auch in die Gegenrichtung** (5 → 0), weil die Generation
  über einen Kaltstart hinweg nicht-monoton ist und ein `>=`-Vergleich hier falsch wäre.
  Dazu `bestaetigungstext`: leer ⇒ `null`, zwei Bereiche ⇒ beide genannt.
  **Ergänzt für T33/T34 (6 Tests in `AppViewModelTeamwechselTest`):** offene Änderung
  liefert `Bestaetigen` mit Bereichs- und Folgenennung; zwei offene Bereiche nennen beide;
  leerer Satz ⇒ `Starten`; zweimalige Prüfung liefert **denselben** Dialog (kein
  Flackern, kein Durchlassen); `laeuft` und `gesperrt` schlagen beide den Dialog
  (Reihenfolge der Prüfung). Ein erster Entwurf behauptete, ein offener Dialog müsse den
  Start blockieren — der Test fiel zu Recht um: bei leerem `offeneBereiche` darf er es
  nicht, sonst wäre die App durch einen unerklärten Zustand dauerhaft gesperrt. **Erledigt** (6 Tests ergänzt).

- [x] **T37 — Block 6 grün** · `S`
  431 Tests / 0 Fehler, `:app:assembleDebug` grün, Gerätetest siehe T33/T34. **Erledigt**.

---

## Block 7 — Härtung & Abschluss

- [x] **T38b — Fehlermeldung „Toggle kam nicht an" + Logzeile** · `S`
  **Anlass:** Nutzerbefund („Server meldet weiterhin dasselbe Team, Wechsel ging erst beim
  zweiten Mal") — im Gerätetest von T38a **live reproduziert**: der Wechsel auf Team 1
  schlug mit dieser Meldung fehl, und der Server stand per Kaltstart geprüft **weiterhin auf
  Team 2**. Die Meldung war also *sachlich richtig und inhaltlich falsch erklärt*.
  **Ursache (durch Logzeile auf dem Gerät belegt):**
  `java.io.IOException: unexpected end of stream on https://os.ongapo.com/haupt.php?changetosecond=true`
  — der Server bricht die Verbindung ab, bevor eine Antwort kommt. Häufigkeit im Gerätetest:
  **4 von 8** bzw. **5 von 10** Wechseln, jeweils **ohne Serverwirkung** (Schalterstand
  unbeeinflusst, am Ende per Kaltstart gegengeprüft: Team 2 wie angezeigt).
  **Zwei Befunde, die die naheliegenden Erklärungen ausschließen:**
  - **Kein OkHttp-Diskcache-Fehler:** der Toggle-Client hat gar keinen Cache, und der
    Refetch läuft mit `forceRefresh` gegen `no-store` (live geprüft).
  - **Kein HTTP/2-Artefakt:** der Toggle-Client auf `HTTP_1_1` festgenagelt ⇒ gleiche
    Fehlerrate, identische Meldung. **Kein Retrying** (`retryOnConnectionFailure=false`,
    Regel 5) ist die *richtige* Entscheidung, erzeugt aber genau diesen Fall: eine
    vor dem Schreiben abgerissene (kalte, ungenutzte) Verbindung wird nicht neu versucht.
  **Änderungen:** `werteAus(vorher, nachher, antwort)` — `antwort == null` bei *gleichem*
  Team ⇒ `Fehler(TOGGLE_NICHT_ERREICHT)` mit **„Wechsel nicht übernommen – bitte erneut
  tippen."** — bewusst **ohne** die Angabe einer nötigen Anzahl Tipps („zwei Tipps"): die
  ~50 %-Fehlerrate ist ein Defekt, keine Bedienregel, und eine Zahl wäre eine Zusage, die
  bei jeder Änderung falsch wird (dieselbe Argumentation wie bei „Erfolg" ohne Beleg);
  ein **belegter** Wechsel bleibt `Erfolgreich`, auch wenn nur die Antwort fehlt (der Refetch
  ist die Wahrheit, nicht der HTTP-Status). Dazu die **erste und einzige Logzeile der App**
  (`Log.w("Teamwechsel", …)`) in `holeToggleHtml()`, die IOException, leere Antwort und
  „keine persönliche Ansicht" unterscheidet — ohne sie war der Fall am Gerät nicht
  aufklärbar. Suite 424/0, `assembleDebug` grün.
  **Gerätetest:** 10 Wechsel, 5 Fehlschläge ⇒ **jeder** mit Snackbar „Wechsel nicht
  übernommen – bitte erneut tippen." und passender Logzeile; kein Fehlschlag mehr mit der
  irreführenden „weiterhin dasselbe Team"-Meldung.
  ⚠️ **Noch offen (bewusst nicht entschieden):** die ~50 %-Fehlerrate selbst ist damit nur
  *erklärt*, nicht *behoben*. Nutzertext ist entschieden („Wechsel nicht übernommen – bitte
  erneut tippen."), die *technische* Gegenmaßnahme nicht:
  - **(a) nichts** — die Meldung ist ehrlich, der Nutzer tippt erneut. Empfehlung, weil jeder
    Retry die zentrale Zusicherung des Plans („genau ein Schreibvorgang pro Geste") aufweicht.
  - **(b) ein Retry, aber nur nach frischem Refetch-Beleg**, dass das Team unverändert ist.
    Sicherheitsargument: PHP serialisiert die Session ⇒ ein Refetch sieht die Toggle-Wirkung,
    sobald der Server sie schreibt; bricht die Verbindung **vor** dem Skript ab, ist der Retry
    folgenlos. **Widerspricht Regel 3** ⇒ braucht Amendment + eigenen Gerätetest.
  - **(c) `retryOnConnectionFailure=true`** — einzeilig, aber OkHttp entscheidet selbst, wann es
    erneut sendet; einer dieser Fälle ist der gefährliche (Antwort abgeschnitten **nach** der
    Serveraktion ⇒ stiller Rückwechsel).

- [x] **T38a — Wechselsperre (Cooldown) nach bestätigtem Wechsel** · `M`
  **Anlass (Gerätebefund des Nutzers):** zu schnelles Wechseln ⇒ Meldung
  „Der Server meldet weiterhin dasselbe Team – es wurde nicht gewechselt", und der Wechsel
  „ging" erst beim zweiten Mal. Ein zu schneller zweiter Tipp ist beim blinden Toggle
  folgenlos *und* nicht widerrufbar.
  **Entscheidung:** 15 s Sperre nach `TeamwechselErgebnis.Erfolgreich`, Meldung **mit echter
  Restzeit** („Teamwechsel in 24 s möglich.").
  ⚠️ **`enabled` bleibt im Sperrzustand `true`** — bei `enabled = false` feuert Compose kein
  `onClick`, die Restzeit-Meldung käme nie an. Gesperrt wird im ViewModel, der Button ist
  optisch nur gedimmt (`alpha`) und nennt die Restzeit in der `contentDescription`.
  ⚠️ **Der Countdown darf nicht durch `meldung` laufen:** `LaunchedEffect(teamwechsel.meldung)`
  (`AppRoot.kt:279`) ist an den *Text* gebunden — ein sekündlich wechselnder Text ließe die
  Snackbar im Sekundentakt neu auslösen. Deshalb eigenes State-Feld `sperrRestSekunden`
  (1-s-Ticker im `viewModelScope`) für die Anzeige, `meldung` nur **einmalig beim Tipp**.
  Umsetzung: `TeamwechselUiState.sperrRestSekunden` + `gesperrt`, `wechselAktion(stand)`
  (reine Funktion ⇒ ohne Netz testbar), `teamwechselSperrMeldung(rest)`, `restzeitNachTakt(rest)`,
  `starteWechselsperre()` nur im `Erfolgreich`-Zweig, Abbruch in `teamwechselZuruecksetzen()`.
  Tests: gesperrt/nicht gesperrt, `Lauft` schlägt `Gesperrt`, Meldung nennt die Restzeit
  (auch bei 1 s), Countdown endet bei 0. Gerätetest: Wechsel ⇒ Button getippt ⇒ Snackbar
  „Teamwechsel in … s möglich.", Dashboard bleibt Team 2, nach 15 s wieder wechselbar.
  **Stand: Code + Unit-Tests fertig (Suite 420/0, `assembleDebug` grün), Gerätetest steht
  noch aus** — ohne Gerät kein Häkchen, der Snackbar-Pfad (Tipp → `meldung` →
  `LaunchedEffect`) ist bisher nur über die reine Funktion abgesichert.
  **Gerätetest (MI 8, Debug-Build) — bestanden:**
  1. Team 1 ⇒ Tipp ⇒ Dashboard `NK Kamen Sesvete` / 2. Liga A Kroatien / `✗ Zugababe
     ungültig`, Snackbar „Jetzt aktiv: NK Kamen Sesvete", Button
     „Team 2 aktiv – **Teamwechsel in 15 s möglich**".
  2. Tipp **im** Sperrfenster (nach 3,2 s): **kein** Wechsel — Dashboard bleibt Team 2,
     Button bleibt „Team 2 aktiv – Teamwechsel in 10 s möglich", Snackbar
     **„Teamwechsel in 13 s möglich."** mit der echten Restzeit. Genau der verlangte Fall.
  3. Nach 13 s: Sperre vorbei, Description wieder normal, Rückschalter auf Team 1 prinzipiell
     frei. ⚠️ Der Rückschalter lieferte in diesem Lauf **„Der Server meldet weiterhin dasselbe
     Team – es wurde nicht gewechselt"** — dazu unten der Nebenbefund.
  **Nebenbefund, wichtig für T38/T41:** Der Server stand nach diesem Fehlschlag **weiterhin auf
  Team 2** — per `am force-stop` + Neustart geprüft (Kaltstart liest `showteam.php` +
  Dashboard neu: Button „Team 2 aktiv", `NK Kamen Sesvete`). Der Toggle-Request ist also
  **nicht** angekommen bzw. folgenlos geblieben; die Meldung stimmte, erklärte aber nichts.
  Genau der in T38a nicht behobene Fall aus der Nutzerbeobachtung.

- [x] **T38 — OkHttp-Diskcache bewusst entscheiden: entfernt** · `S`
  **Entscheidung: Cache raus** (nicht „Interceptor für team-scoped Pfade") — ein
  team-scoped Interceptor wäre die aufwendigere Variante für einen Nutzen, den es nicht
  gibt, und ließe die Grundannahme („der Cache ist harmlos, nur team-scoped
  problematisch") stehen, obwohl er **gar nicht** existiert.
  **Beleg, live an allen vier teamunabhängigen URLs geprüft** (`haupt.php`,
  `showteam.php?s=N`, `zugabgabe.php`, `ka.php`) — und ausdrücklich **auch** am
  Toggle-Endpoint selbst:
  ```
  Cache-Control: no-store, no-cache, must-revalidate
  Pragma: no-cache
  Expires: Thu, 19 Nov 1981 08:52:00 GMT
  ```
  OkHttp kann solche Antworten weder speichern noch ausliefern ⇒ der 10-MB-Diskcache
  brachte **keinen** Nutzen, genau wie es schon im Plan-Erstbefund stand — dort stand
  allerdings noch „nur team-scoped", was sich als **falsche Abschwächung** erwiesen hat:
  die Header gelten **global**, nicht pro Pfad. Er lag also vollständig umsonst
  Sitzungsdaten unverschlüsselt auf Platte — und war bei gerade diesen URLs die
  **einzige** Stelle, die Team 1 für Team 2 ausliefern **konnte**, falls sich die Header
  je ändern.
  **Umsetzung:** `provideHttpCacheDir` und der `cacheDir`-Parameter komplett entfernt
  (inkl. `Cache`-/`File`-/`Context`-Imports), beide Clients über
  `buildOkHttpClient(...)` ohne Cache-Konfiguration. Kein No-Store-Interceptor nötig:
  der Server erledigt das selbst, und `no-store` in OkHttp zu erzwingen hieße, die
  Header zu duplizieren, die der Server bereits korrekt sendet.
  **Gerätetest:** `http_cache` aus `cache/` gelöscht, App neu gestartet und benutzt
  (Dashboard + zwei Wechsel) — Verzeichnis wird **nicht** neu angelegt
  (`ls -a cache` ⇒ nur `code_cache`, `files`, `shared_prefs`). Beide Toggles
  funktionieren (T1→T2 und T2→T1), alle Repositories laufen auf dem cachefreien
  Standard-Client. Suite 424/0, `assembleDebug` grün.

- [x] **T39 — `DashboardData.teamId` auf `Long?` umgestellt** · `M`
  **Ausgangsbefund:** `DashboardData.teamId: Int?`, während **alle** übrigen Team-IDs im
  Projekt `Long?` sind (Navigation `NavType.LongType`, `StadionnameStore`,
  `TeamwechselInfo`, Ligatabelle). Die Folge waren 5 Casts `.toLong()` an
  Konsumenten (`ElfDesSpieltagsRepository.kt:150`, `SpielberichtViewModel.kt:50`,
  `SeiteViewModel.kt:102`, `AppViewModel.kt:389`, `BewerbeRepository.kt:120`) und ein
  Cast **im** Konsumenten (`BewerbeViewModel.kt:219/306`).
  **Der Plan-Befund war unvollständig** — beim Umbau zeigte sich, dass `Int` nicht an
  `teamId` hing, sondern sich von dort in **Nachbarmodelle** ausgebreitet hatte. Wer nur
  die 5 Casts entfernt, bekommt Compile-Fehler:
  - `DashboardData.MatchInfo.gegnerId: Int?` — **muss** mitwandern, weil
    `SpieleViewModel.kt:57` beide IDs in einem Elvis koppelt
    (`if (next.heim) dashboard.teamId else next.gegnerId`); unterschiedliche Typen ⇒ der
    Elvis typisiert zu `Comparable<*>`, und `ladeSpiel(teamId: Int)` passt nicht mehr.
  - `LivegameRepository.ladeSpiel(teamId: Int)` ⇒ `Long`.
  - `BewerbeViewModel.eigeneTeamId: Int?` ⇒ `Long?`, sonst bleibt die halbe Kette `Int`
    und der Cast `own.toLong()` in `BewerbeRepository.parseLigatabelle` (Zeile 120) nur
    kosmetisch entfernt. Das ist die Stelle, die den **eigenen Verein in der
    Ligatabelle** markiert — sie darf nicht stillschweigend ihre Bedeutung verlieren.
  **Umsetzung:** `toIntOrNull()` ⇒ `toLongOrNull()` in `DashboardRepository` (beide
  Parse-Pfade: Wappen-URL und `teaminfo(n)`-Link), `teamId`/`gegnerId`/`eigeneTeamId`/
  `ladeSpiel`/`ladeLigatabelle`/`parseLigatabelle` auf `Long?`, alle 7 Casts entfernt,
  Parse-Tests auf `L`-Literale. Die Integer-Literale in den Ligatabelle-Tests
  (`parseLigatabelle(dump, 3449)`) typisieren dabei **automatisch** zu `Long` — kein
  Testumbau nötig, das ist der eigentliche Wert der Typkorrektur.
  Suite 424/0 grün, `assembleDebug` grün.
  **Gerätetest:** Dashboard, Team, Taktik-Editor, ZAT/Aufstellung und
  Nationale Bewerbe/Ligatabelle laden nach dem Umbau fehlerfrei. Die eigene
  Vereinszeile in der Ligatabelle ist **nicht** per Screenshot belegt — sie wird im
  Rendering offenbar nicht über eine abweichende Hintergrundfarbe markiert (Pixelstich-
  probe: eigene Zeile und Nachbarzeilen identisch `rgb(248,250,248)`). Der Nachweis ist
  stattdessen der Unit-Test `BewerbeRepositoryParseTest.tabelle_mitEigenerZeile`, der
  genau die geänderte Stelle abdeckt (`parseLigatabelle(dump, 3449)` ⇒ `eigenZeile` zeigt
  auf die Zeile mit „SC Viktoria Ulm"). Ein Screenshot als Beleg wäre hier irreführend
  gewesen.
  **Kosmetsch, kein Fehler** — wie im Plan vermerkt; der Wert liegt darin, dass
  `teamId` jetzt denselben Typ hat wie jede andere Team-ID im Projekt und der nächste
  Entwickler an dieser Stelle nicht mehr über `Int`/`Long` nachdenken muss.

- [ ] **T40 — Doku aktualisieren** · `M`
  `Analyse_Teamwechsel_Website.md` ist **überholt** (behauptet „Teamwechsel existiert nicht").
  Aktualisieren auf den Live-Befund; Verweise aus diesem Plan ergänzen.

- [ ] **T41 — Abnahme am Gerät** · `M`
  Kompletter Durchlauf: Login ⇒ Button `1` ⇒ Wechsel auf `2` ⇒ alle Tabs (T29) ⇒ Fremdverein
  (T30) ⇒ Zugababe-sperre (T33) ⇒ App-Neustart (T31) ⇒ zurück auf `1`.

- [ ] **T42 — Abschluss-Commit** · `S`
  Alle Änderungen committen, Häkchen in diesem Dokument gesetzt lassen.

---

## Aufwandssumme

| Block | Schritte | Summe |
|---|---|---|
| 0 — Datenmodell & Parser | T01–T05 | ~1 h 45 min |
| 1 — Netzwerk | T06–T12 | ~2 h 30 min |
| 2 — Discovery | T13–T16 | ~1 h 30 min |
| 3 — ViewModel | T17–T20 | ~2 h |
| 4 — Button | T21–T24 | ~1 h 30 min |
| 5 — Zustands-Reset | T25–T32 | ~2 h 45 min |
| 6 — Schreibschutz | T33–T37 | ~2 h 45 min |
| 7 — Härtung | T38a, T38b, T38–T42 | ~3 h |
| **Gesamt** | **45 Schritte** (T01–T42 + T26a, T38a, T38b) | **~17 h 45 min** |

Alle Schritte sind einzeln **≤ 30 min**.

---

## Abnahmekriterien (Feature gilt als fertig, wenn)

1. [ ] Der Button `1|2` erscheint nur bei Konten mit Zweitteam, links neben dem Darkmode-Button.
2. [ ] Ein Klick wechselt das Team **serverseitig verifiziert** — kein Optimismus.
3. [ ] Nach dem Wechsel zeigt **jeder** Bereich Team-2-Daten (T29).
4. [ ] Fremdverein-Ansichten bleiben unverändert (T30).
5. [ ] Kein Schreibvorgang kann Team-1-Inhalt in Team 2 schreiben (T35) und ein offener
   Bearbeitungsstand geht beim Wechsel nicht still verloren (T33/T34).
6. [ ] App-Neustart auf Team 2 zeigt korrekt `1` (Server-Wahrheit, T31).
7. [ ] Ein Doppelklick erzeugt **genau einen** Toggle (Single-Flight).
8. [ ] Ein Verbindungsabbruch beim Toggle erzeugt **keinen** Rück-Toggle (`retryOnConnectionFailure=false`).
9. [ ] Ein Wechselversuch während der 15-s-Sperre löst **keinen** Request aus, sondern die
   Restzeit-Meldung (T38a).
10. [ ] Ein gescheiterter Toggle-Request meldet „Wechsel nicht übernommen – bitte erneut
   tippen" statt „weiterhin dasselbe Team" (T38b).
11. [ ] Es existiert **kein** OkHttp-Diskcache mehr: `cache/http_cache` wird nicht
   angelegt (T38).
12. [ ] `./gradlew :app:testDebugUnitTest` und `:app:assembleDebug` sind grün.

---

## Verworfene Alternativen (nicht wieder aufgreifen)

| Alternative | Warum verworfen |
|---|---|
| Persistierter `ActiveTeamStore` mit `teams[]`, `generation`, `verifiziert` | Stiller Re-Login via `lc` = neue PHP-Session ⇒ Server setzt auf Hauptteam zurück ⇒ persistierter `aktivId` ist nach jedem Kaltstart systematisch falsch. Ein gespeicherter Wert kann dem Server nur **widersprechen**, nie recht haben. |
| `TeamScope` als Ersatz für `teamId == null` | 11 Repo-Methoden + 10 ViewModel-Wrapper umstellen für **null** Zusatzinformation — der Session-Toggle wirkt bereits automatisch auf alle `showteam.php`-Aufrufe. Gefährdet Fremdverein-Logik. |
| „Wenn `aktivId != ziel`, genau einmal nachzutogglen" | Schreibvorg aufgrund einer **veralteten Annahme**. Zusammen mit OkHttps Retry realistisch drei Toggles ⇒ Team springt zurück, App meldet Erfolg. |
| Globaler `no-store`-Interceptor für team-scoped URLs | Der Server sendet die Header bereits — und zwar **global**, nicht team-scoped (T38, live an vier URLs geprüft). Ein Interceptor würde sie nur duplizieren. Stattdessen: Cache komplett entfernt. |
| Ziellisten-Dropdown „Team 1 / Team 2" | Die Website hat nur einen Toggle. Ein Dropdown wäre eine Lüge über den Endpunkt. Statischer 1|2-Button ist die ehrliche Abbildung. |