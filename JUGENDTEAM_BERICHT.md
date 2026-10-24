# Jugendteam – Stand & Handoff

Lokale Notiz, **nicht** committen (persönlicher Zwischenstand, kein Release-Inhalt).
Stand: So 04. Okt 2026.

---

## 1. Was umgesetzt ist (Fertig)

Zwei zusammenhängende Aufgaben:

### 1a) ⋮-Menü „ins A-Team berufen" pro Jugendspieler (voriger Auftrag)
- Ursache: `parseJugend` legte das `ziehmich`-Formular in einem **eigenen Abschnitt**
  an. `SeiteScreen` verknüpft Zeile und Aktion aber nur **innerhalb desselben Abschnitts** →
  kein ⋮-Menü erschien, stattdessen unten eine rohe Radioliste.
- Fix: Formular hängt jetzt an jedem Jahrgang mit ziehbaren Spielern;
  eigener Abschnitt entfällt.
- `AktionDialog.kt`: `zeilenBestaetigungstext()` (reine Funktion) liefert
  `„18 Jahre · Geb. 12" wirklich ins A-Team berufen?`; Bestätigungsbutton heißt
  `Ins A-Team berufen` (nur Anzeige, POST-Wert bleibt `button.text`).

### 1b) Unterreiter des Jugendteams (dieser Auftrag)
Die Website `ju.php` hat vier Unterreiter (`?page=1..4`):

| page | Label | Inhalt |
|------|-------|--------|
| 1 | Teamübersicht | bisherige Ansicht (Alter/Geb./Land/U/Skill/Talent/Aufwertung + Aktion) |
| 2 | Spielereinzelwerte | 17 Einzelskills (SCH…EIN) je Spieler als Chips |
| 3 | Opt. Skill | Skillschnitt + Opt-Skill je Position (TOR/ABW/DMI/MIT/OMI/STU) |
| 4 | Optionen | Formulare „Jugendförderung" + „Jugendschranke" |

„Jugendteam" auswählen öffnet weiterhin Teamübersicht (page 1) – unverändert.

**Geänderte/neue Dateien:**

| Datei | Änderung |
|-------|----------|
| `data/model/Bewerbe.kt` | neu `SeitenTab` + Feld `tabs: List<SeitenTab>` in `SeitenAnsicht` |
| `data/repository/TeamRepository.kt` | `vonPfad` trennt Query ab; neu `jugendPage`, `jugendTabs`, `parseJugendSeite` (internal), `parseJugendEinzelwerte`, `parseJugendOptSkill`, `parseJugendOptionen`; `parseJugend` liefert `tabs` |
| `feature/team/SeiteViewModel.kt` | `path` jetzt mutable; neu `wechsleTab(pfad)` |
| `feature/team/SeiteScreen.kt` | `FilterChip`-Reiterzeile, wenn `tabs.size > 1`; `onTabClick` |
| `feature/team/AktionDialog.kt` | Bestätigungstext (siehe 1a) |
| `feature/team/SeiteViewModel.kt` | `TeamGeneration`-Schreibgate in `sendeAktion` (siehe 2c) |
| Test-Resources | neu `ju_einzelwerte.html`, `ju_opt_skill.html`, `ju_optionen.html` |
| `TeamSeitenParseTest.kt` | 5 neue Tests (Einzelwerte/OptSkill/Optionen/Tabs/Query-Strip) |
| `ZeilenAktionenTest.kt` | 3 Tests für `zeilenBestaetigungstext` |

---

## 2. Abgeschlossen

### 2a) Build komplett verifiziert (05.10.2026)
```
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
BUILD SUCCESSFUL – 473 Tests, 0 Fehler (36 Klassen, inkl. AktionSendenTest)
Lint: 0 Errors, 39 Warnings (alle vorbestehend: UseKtx, GradleDependency, …)
app-debug.apk 24,5 MB · app-release.apk 16,0 MB (debug-signiert)
```
Hinweis: `TeamRepository.kt:1460` warnt „Condition is always 'true'“ – **vorbestehend**
(gleiche Stelle vor diesem Auftrag bei Zeile 1253), nicht angefasst.

### 2b) `Analyse_OnlineSoccer.md` nachgezogen (AGENTS.md-Pflicht)
| Abschnitt | Ergänzung |
|-----------|-----------|
| A „Jugend“ | vier Unterreiter mit Spaltenzuordnung, „keine Spielernamen“, Formular-Regel je Jahrgang |
| B Navigationsbaum | `Jugendteam → ju.php (Unterreiter ?page=1..4)` |
| F Aktionen | drei verifizierte Schreibaktionen (`ziehmich`+`ziehen`, `foerderung`, `schranke`) |
| G Klassifikation | Jugendteam **D → B** (nach Login verifiziert); Jugendscouting bleibt **D** |
| Anhang | belegte Fakten zu `ju.php` (Menü, Kopfzeilen, namenlose Submit-Buttons) |

### 2c) `TeamGeneration`-Schreibgate in `SeiteViewModel`

Bestehende Lücke geschlossen, Muster wie `ZugabgabeViewModel`/`TaktikViewModel`:

- Konstruktor: `private val teamGeneration: TeamGeneration` (Hilt liefert den
  `@Singleton`, keine DI-Änderung nötig – `SeiteScreen` holt per `hiltViewModel()`).
- `private var geladeneGeneration = teamGeneration.current()` + `private fun gate(): String?`
- `lade()` setzt `geladeneGeneration = teamGeneration.current()` (deckt auch
  `wechsleTab` ab, das über `lade()` läuft).
- `sendeAktion()` prüft das Gate **vor** dem POST; bei Ablehnung `sendend = false`
  und `aktioFehler = TEAMWECHSEL_VERWORFEN` (erscheint als Fehlerkarte in der Seite).
- ~~`stadionnameSpeichern()` bleibt ohne Gate: es schreibt lokal, schlüsselbar nach
  `teamId`.~~ **Nachtrag 24.10.2026:** Die lokale Stadionnamen-Eingabe wurde aus
  urheberrechtlichen und Wartungsgründen **entfernt** und wird nicht weiterverfolgt.
  Der Spielbericht zeigt wieder ausschließlich den Servernamen.
- Kein neuer Test: das Projekt hat keine Mock-Bibliothek (nur junit4 + org.json),
  `ZugabgabeViewModel`/`TaktikViewModel`/`TransferStatusViewModel` haben aus
  demselben Grund keine ViewModel-Tests. Abgedeckt ist die reine Funktion
  `schreibvorgangErlaubt` durch `TeamGenerationTest`.

---

## 3. Wichtige Hinweise

- **Login-Daten:** wurden für den Live-Abruf verwendet, liegen ausschließlich in
  `/tmp/opencode/os/` (nicht im Repo). Passwort nach dieser Sitzung ändern.
- **Live-Dumps** (zum Nachprüfen der Seitenstruktur): `/tmp/opencode/os/`
  `ju_live.html`, `ju_p2.html`, `ju_p3.html`, `ju_p4.html`, `cookies.txt`.
- Die neuen Fixtures enthalten **keine** personenbezogenen Daten
  (nur Nation/Alter/Skill-Werte; keine Namen, keine PIDs, keine Mail) – geprüft.

---

## 4. Spalten-Zuordnung (falls Website-Layout sich ändert)

- **page 2:** Kopf `[Land(colspan3), U, Alter, SCH…EIN]`; Datenzeile
  `0 Flag, 1 Kürzel, 2 leer, 3 U, 4 Alter, 5.. Skills`.
- **page 3:** Kopf `[Land(colspan2), U, Alter, Skill, TOR, ABW, DMI, MIT, OMI, STU]`;
  Datenzeile `0 Flag, 1 Kürzel, 2 U, 3 Alter, 4 Skill, 5.. Positionen`.
- **page 4:** zwei Formulare; Absende-Buttons ohne `name` → nur Feldwert wird
  übertragen (`foerderung=<betrag>` bzw. `schranke=<wert>`).

---

## 5. Gerätetest (05.10.2026, MI 8 / LineageOS, Android 15)

`app-debug.apk` per `adb install -r` installiert – Signatur passte, **kein Uninstall,
Login/Session erhalten**. Getestet über UI-Dump (`uiautomator`), nicht über Screenshots.

| Prüfung | Ergebnis |
|---------|----------|
| App-Start | kein Crash, Login intakt (SC Viktoria Ulm, 2. Liga B, ZAT 11) |
| Reiterzeile | vier Chips sichtbar; „Optionen" erst nach horizontalem Scroll (LazyRow) |
| Teamübersicht | „Bitte beachten" + Jahrgangsgruppen, pro Zeile ⋮ („Aktionen") |
| Spielereinzelwerte | 17 Chips SCH…EIN, Zeilenüberschrift „18 Jahre · GER" |
| Opt. Skill | Skill + ABW/DMI/MIT/OMI/STU |
| Optionen | „Allgemeine Optionen" (Jugendförderung-Select `500` + „speichern"), „Jugendschranke" mit 3 Radios und 2 Hinweispunkten |
| Zeilen-Dialog | ⋮ → „Ins A-Team berufen" → Dialog: `„18 Jahre · Geb. 6" wirklich ins A-Team berufen?`, Buttons **Abbrechen** / **Ins A-Team berufen** |

**Zwei Befunde, kein Fehler:**
1. Auf „Opt. Skill" fehlt der Chip **TOR** – die Website liefert die Spalte nur für
   die 3 Toren (z. B. UZB 43.78), bei den anderen 24 Zeilen ist sie leer. Spalten-
   zuordnung ist korrekt (TOR = Zellenindex 5).
2. Beim allerersten Öffnen der Jugendteam-Seite zeigte ein UI-Dump die Liste bereits
   weit unten (Titel + Chips fehlten), nach Scroll-to-top war alles korrekt.
   Nicht reproduziert – vermutlich Dump-Timing beim ersten Composing, nicht
   bestätigt. Bei Bedarf gezielt nachprüfen.

**Nicht ausgeführt** (echte Schreibaktionen auf dem Livespiel):
`speichern` der Jugendförderung, „Jugendschranke setzen", „Ins A-Team berufen"
bestätigen.

---

## 5b) Nachweis des Berufungs-Requests ohne Livespiel (05.10.2026)

Ziel: belegen, **was** die App bei „Ins A-Team berufen" wirklich sendet – ohne
Geld auszugeben und ohne den echten Spielerserver anzufassen. Zwei Ebenen:

### 5b-1) Dauerhafter Test in der CI

`app/src/test/java/com/onlinesoccer/app/data/repository/AktionSendenTest.kt`
(4 Tests) bildet die komplette Kette aus **echten** Server-Dumps ab:
`parseJugend` → `zeilenAktionen` (Zuordnung Zeile ↔ Formular) →
`formularFelder` (was der Dialog übergibt) → `fuehreAktionAus`. Der Request geht
dabei an einen **aufzeichnenden OkHttp-Interceptor**, der Methode, URL und den
tatsächlich kodierten Body festhält. Abgedeckt sind:

| Test | Erwarteter Body |
|------|-----------------|
| `berufungSendetZiehmichUndZiehenAnJuPhp` | `ziehmich=<ID>&ziehen=Markierten%20Spieler%20ins%20A-Team%20berufen` an `https://os.ongapo.com/ju.php`, POST |
| `jugendfoerderungSendetNurDenBetrag` | `foerderung=1500` |
| `jugendschrankeSendetNurDenWert` | `schranke=1` |
| `leererWertWirdNichtGesendet` | kein Feldpaar für unberührtes Auswahlfeld |

Dafür wurde `formularFelder` in `AktionDialog.kt` aus dem Composable in eine
`internal` Top-Level-Funktion gezogen (gleiche Logik, jetzt ohne Android testbar).

### 5b-2) Gerätetest gegen lokalen Mock-Server

Temporäre Debug-only Activity (danach vollständig entfernt, siehe 5b-3) lud
`ju.php` von einem lokalen Python-Mock über `adb reverse tcp:8099 tcp:8099`,
baute die Felder genauso wie der Dialog und rief das echte `fuehreAktionAus`.
Vom Mock serverseitig protokolliert:

```
POST /ju.php HTTP/1.1
User-Agent: OnlineSoccerApp/0.1 (Android; okhttp 4.12)
Content-Type: application/x-www-form-urlencoded
Content-Length: 68

ziehmich=246866&ziehen=Markierten%20Spieler%20ins%20A-Team%20berufen
```

Der Anzeigetext „Ins A-Team berufen" im Dialog ist nur die Beschriftung; gesendet
wird der Button-Text der Website. Leerzeichen kodiert OkHttp als `%20`.

### 5b-3) Aufräumen und Grenzen des Nachweises

- Debug-only `NetworkSecurityConfig` (Klartext nur für `127.0.0.1`) und
  `MockAktionActivity` nach dem Test gelöscht, Debug-Manifest via `git checkout`
  zurückgesetzt, `adb reverse` entfernt, Mock-Server beendet. `git status`
  bestätigt: `app/src/debug` unverändert, kein Debug-Gerüst im APK
  (`dumpsys package` findet `MockAktionActivity` nicht mehr).
- Der Mock hat **kein** Cookie erhalten – die Session-Cookies gehen nur an
  `os.ongapo.com`, es sind keine Zugangsdaten auf den lokalen Server geflossen.
- **Nicht bewiesen** ist damit, dass der echte Server den Request annimmt oder
  wie die Seite danach antwortet. Das wäre nur mit einer echten Aktion prüfbar
  und bleibt bewusst offen.

---

## 6. Offen für den nächsten Auftrag

Nichts aus diesem Auftrag. Kandidaten für den nächsten Durchgang:

- `TeamRepository.kt:1460` Compiler-Warnung „Condition is always 'true'“ (alt).
- `juscout.php?g=` Feldernamen des Jugendgebots – erst mit aktivem Angebot klärbar.
- `PLAN_Phase3.md` / `PLAN_UpdateFunktion.md` auf Restpunkte prüfen.
