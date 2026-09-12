# Phase 3 – Architektur- und Umsetzungsplan (Online Soccer App)

Stand: Phase-2-Analyse abgeschlossen. Quelle der Wahrheit bleibt die OS-Website.
Die App bildet vorhandene Funktionen ab und optimiert Bedienung/Navigation/Darstellung für Smartphones.

> **Status (letzte Aktualisierung):** M1–M7, M9, M10 umgesetzt, `assembleDebug` grün, App installiert. Bewerbe-Hub repariert und **auf dem Gerät verifiziert**. **Aufstellung: Taktik-/Formationsauswahl (M4) nativ umgesetzt und live verifiziert** – inkl. echte Schreibaktion `checkza.php` („Zugabgabe speichern“) und `raster1=…&raster=Laden` (Taktik anwenden). **4-Punkte-Nutzerauftrag (Kader-Zuordnung, „Laden aus ZAT“, erweiterte Spielfeld-Matrix, reduzierte Liga-Tabelle) umgesetzt und Ende-zu-Ende auf dem Gerät verifiziert** (inkl. Schreibaktionen ZAT laden & Kader speichern), s. unten.
>
> **Aufstellungsfeld-Marker wie auf der Webseite (Buchstaben statt Nummern, korrekte Zellen):** Marker zeigen jetzt die `ra`-Kürzel (A–L Feld, T Torwart, U–Z Ersatzbank) statt Trikotnummern. **Ursache „bestätigte Zellen“ war ein Off-by-one:** Die Beta-Seiten-Slots (`moveFieldClone(pid, zeile, spalte)`) sind **0-basiert** (0..14 / 0..10), die klassische Matrix ist 1-basiert → Marker saßen eine Zelle zu weit oben/links. **Lösung:** Positionen werden nun aus der **klassischen Formationstabelle** (`parseRasterLetters`, erste Wahrheit der Website) übernommen – jede feld-Zelle enthält den `ra`-Buchstaben, daraus zeile/spalte (0-basiert). `FeldView` rendert Marker bei `(zeile+0.5)/15` / `(spalte+0.5)/11` (= Zellzentrum), `Trikot`-Parameter `nummer`→`label`. Neon-Demo-Screenshot bewies: **alle 10 Feldmarker + T + U–Z in exakt den Webseiten-Zellen** (A(3,4), B(3,8), C(6,3), D(6,9), E(8,6), F(10,2), G(10,10), H(12,6), K(13,3), L(13,9), T unterm Feld). Offene Positionen-Frage: E/H zeigen z. Zt. (8,6)/(12,6) (aktiver Live-Stand der klassischen Seite u. Beta), älterer Zwischenstand hatte (8,5)/(12,5) – seit längerer Zeit kein Digital auf der Webseite. To-dos: temporäre Debug-Logs entfernt, finaler optischer Abgleich App ↔ Web-Auslade im Browser steht noch aus.

---

## 1. Ziele & Prinzipien

1. **Technisch korrekt** – Server-Datenmodelle exakt einhalten (Phase 2 verbindlich).
2. **Smartphone-optimiert** – Daumenbedienung, große Touchflächen, keine Desktop-Tabellen, keine Winzig-Links.
3. **Nativ** – Kein WebView als Hauptarchitektur. WebView nur für Forum (optional) und statische Berichte.
4. **Führung statt Freiheit bei riskanten Aktionen** – Bei Finanzaktionen/Zugabgabe: Aktion → Zusammenfassung → Bestätigung → Serveraktion → Serverantwort → Ergebnis.
5. **Schrittweise** – Prototyp zuerst (Aufstellung = Kernfunktion), nach jedem Modul kompilieren & testen.

---

## 2. Umgebung / Technologie-Stack (verifiziert)

| Ebene | Wahl | Version | Hinweis |
|---|---|---|---|
| JVM | OpenJDK | 25 | installiert (/usr/bin/java) |
| Build | Gradle (Wrapper) | 9.5.0 | Dist im Cache vorhanden, läuft auf JDK 25 |
| AGP | com.android.application | 9.1.0 | im Cache |
| Sprache | Kotlin | 2.2.10 | + kotlin.plugin.compose, kotlin.plugin.serialization |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 | material3 1.3.1, compose ui 1.7.6 |
| Navigation | androidx.navigation:navigation-compose | 2.8.5 | |
| STATE | viewmodel-compose / lifecycle | 2.8.7 | |
| Network | OkHttp | 4.12.0 | eigener CookieStore für os/lc |
| Serialisierung | kotlinx-serialization-json | 1.8.0 | |
| DI | Hilt | 2.28.3 | + hilt-navigation-compose, KSP |
| Secure Storage | androidx.security:security-crypto | (1.1.0-alpha*) | lc-Token + session encrypted |
| HTML-Fallback | org.jsoup | (nur wo nötig) | z. B. Berichte, seltene POST-Formulare |
| UI-Cache (später) | Room | 2.6.1 | nur sinnvoller lokaler Cache |

compileSdk/targetSdk 35, minSdk 26 (Android 8.0).

---

## 3. Projektstruktur (Monomodul zu Beginn, nach Domänen gepackt)

Ein `app`-Modul; Package-Aufbau nach Domänen, damit eine spätere Multi-Modul-Aufteilung
(`core:network`, `core:data`, `feature:aufstellung`, …) verlustfrei möglich ist.

```
app/src/main/java/com/onlinesoccer/app/
  MainActivity.kt
  OnlineSoccerApp.kt                (Application, Hilt-Konfiguration)
  core/
    network/        OkHttp, CookieStore, SessionGuard, Endpoints
    auth/           AuthState-Machine, LoginRepository
    storage/        SecurePrefs (os/lc), CookieStore-Persistenz
    ui/             Theme, gemeinsame Compose-Komponenten
  data/
    model/          Domainmodelle (Aufstellung, Taktik, Team, Spieler, …)
    repository/     je Domäne ein Repository (AufstellungRepo, TaktikRepo, …)
    html/           Jsoup-Helfer (nur wo nötig)
  feature/
    dashboard/
    mannschaft/
    zugabgabe/      (aufstellung + taktik + aktionen + speichern)
    spiele/         (livegame, freundschaft, fss, emtipp)
    transfers/      (vt, tm, vm, gebote, leih)
    nachrichten/    (pm, benachrichtigungen)
    mehr/           (einstellungen, verein, stadion, forum-webview)
```

---

## 4. Architektur (Schichten)

```
Compose-UI (feature/*)
   │  StateFlow / UiState
ViewModel (feature/…/*ViewModel)
   │
Repository (data/repository)   ← Source of Truth: SERVER, kein lokaler State
   │
Network/API (core/network)     ← Endpoints, Parser (JSON + HTML)
   │
OkHttp + CookieStore (core/network)  ← Session-Pflege
   │
SecureStorage (core/storage)   ← lc-Token, Sessionnebenwerte (verschlüsselt)
```

Regeln:
- UI liest nur UiState (StateFlow); UI-Aktionen gehen über ViewModel → Repository → Netzwerk.
- Keine harcodierten IDs; jede ID/Person kommt vom Server.
- Lokaler Cache (Room/Datastore) nur für nicht-kritische UI-Daten (z. B. letzte Formation als Entwurf), nie als Wahrheit.
- Keine Passwörter/Cookies in Logs; OkHttp-Logging auf Header-losem Level oder deaktiviert.

---

## 5. Auth-/Session-Schicht

### Login (aus Phase 2, verbindlich)
- `POST https://os.ongapo.com/validate.php`
- Body: `action=os_login&loginemail=<mail>&passwort=<pw>&imageField.x=10&imageField.y=10`
- Erfolg = HTTP 302 → `os_menu_haupt.html`, Server setzt `os` (Session) + `lc` (persistent, 2 Jahre).
- Fehler = HTTP 200, Text „Der Username oder das Passwort ist falsch.“

### Silent Relogin
- `lc` allein stellt beim Server eine neue `os`-SID aus (per Hauptseiten-Aufruf). `lc` daher wie ein Passwort behandeln: verschlüsselt speichern (Android Keystore / security-crypto).

### Session-Zustände (AuthState-Machine)
- `SIGNED_OUT` → Login-Screen
- `SIGNING_IN` → Ladeanzeige (LDAP: nur UI)
- `SIGNED_IN` → Haupt-Navigation
- `EXPIRED` → stiller Relogin mit `lc`; gelingt → `SIGNED_IN`, sonst `SIGNED_OUT`

### Session-Guard (Inhalt statt Statuscode, Phase 2)
- Server liefert bei abgelaufener Session HTTP 200 mit Demo-/Login-Ansicht.
- Guard prüft Inhaltsmarker (z. B. „Kontostand“ persönlich vs. „DemoTeam“/Login-Text).
- `haupt.php` nach jedem App-Start einmalig abrufen → persönliche Daten + Session-Gültigkeit klären.

---

## 6. Datenmodelle (Kern)

Aus Phase-2-Mapping; die App übernimmt exakt die Serverfelder.

- `Team`: id, name, tag, liga, platz, punkte, kontostand, stadium, …
- `Player`: id (nur Server!), name, position (Tor/Abwehr/…), stärke, alter, marktwert, kondition, verzichtbar, …
- `LineupSlot`: slot (A–H/K/L/T–Z bzw. Beta-Gitter), playerId (0 = leer)
  - Klassisch: `ra[<pid>] = Platzcode` (SELECT „“ = unbesetzt)
  - Beta: `aufstellung` = JSON `[[Tor,0,0],[10 Feldspieler: links 1..11, oben 1..15],[6 Ersatz: -i,-1]]`
- `Tactic`: `taktik[]` = positionscodes (Checkboxen von HTML-Raster; 11 Spieler + Label)
- `ZatInfo`: zat, gegner (heim/gast), status, torerwartung, …
- `Match` / `Livegame`: gamedata-JSON (actions, squad, tactics, statistics, reporturl)
- `Transfer/Offer`: spieler, angebot, ablauf, status
- `Pm`: id, betreff, von, datum, gelesen, text

WICHTIG: Feldnamen/Platzcodes werden beim Prototyp-Einbau direkt aus den abgerufenen
Original-Seiten verifiziert (kein Raten).

---

## 7. Navigationskonzept

Bottom-Navigation mit 5 festen Tabs (aus Phase-2-Nutzungshäufigkeit):

```
[Dashboard] [Mannschaft] [ZUGABGABE] [Spiele] [Transfers]
```

- **Zugabgabe als eigener Tab** (jedermann schnell erreichbar), in Dashboard zusätzlich ein prominent Button „Zug abgeben“ (mit Badge „offen“).
- **Nachrichten & Mehr** als Icons in der TopBar (Nachrichten-Badge) bzw. über Dashboard erreichbar (vermeidet Tab-Überladung).
- Weitere Seiten: Navigation per Start-Destination im jeweiligen Tab (kein tiefer Tab-Verschachtelungswald).

Erste Navigation (Phase 3-Prototyp):
- Start: Login → Dashboard.
- Tab-Maske oben; innerhalb je ein eigenen NavController-Level.

---

## 8. Serverkommunikation – Speicherweg Entscheidung (Zugabgabe)

Entscheidung: **Beta-Aufstellung primär**, Klassisch als Fallback.

Begründung:
- Beta liefert/akzeptiert strukturiertes JSON (`aufstellung`) → perfekt für natives Editieren, kein HTML-SELECT-Parsing.
- Klassische `ra[<pid>]`-Slots bleibt als Fallback (nur falls Beta-Save ein Problem macht).
- Taktik unverändert `taktik[]` + `speichername` (eigenes Formular), Training `tr1/tr2<pid>`.

Workflow (geführter Prozess, §6 der Freigabe):
1. Spiel auswählen → 2. Aufstellung → 3. Taktik → 4. Aktionen → 5. Einstellungen →
6. Zusammenfassung → 7. Speichern → 8. Serverantwort prüfen (Redirect/Fehlerte XT) → 9. Erfolg anzeigen.

Status sichtbar: Zug gültig/ungültig, ZAT, Gegner, aktuelle Aufstellung, Taktik, fehlende Einstellungen.

---

## 9. Aufstellungs-UI (Prototyp-Kern)

- **Kein** HTML-Tabellen-UI. Moderner Fußballplatz (Compose Canvas / gezeichnete Platzgrafik).
- 11 Spieler auf dem Feld positioniert gemäß Platzcode (links 1..11, oben 1..15) bzw. Slot.
- Bedienung:
  - **Tippen** (Hauptweg): Spieler antippen → Aktionen („ersetzen“, „verschieben“, „raus“, „tauschen“) → Auswahl aus Ersatzbank/Formation. Drag&Drop optional zusätzlich, NIE einziger Weg.
  - Ersatzbank als horizontale Chip-Leiste unten.
  - Formation-Wahl (vordefinierte Muster: 4-4-2, 4-3-3, …) → Belegung vorbelegen, manuell nachjustierbar.
  - Rotation/Rollen (Spieler + Einsatzwunsch, Batteriewert) aus Serverfeldern übernehmen.
- Prototyp-Kriterien (§14 der Freigabe): Spieler auswählen/verschieben/tauschen, Ersatzbank, Formation, Rotation, Scroll, kleine Displays, Hochformat, Speichern, erneutes Laden, Serverantwort.

---

## 10. Taktik-UI

- Exaktes 11×15-Modell der Seite: die Checkbox-Positionen (A–P × 1..11) unverändert als Datenwerte, aber als visuelles Feld dargestellt.
- keine neuen Spielregeln; nur bessere Darstellung/Bedienung (Tap statt Checkbox-Raster).

---

## 11. Sicherheitsregeln (App-seitig)

- Keine automatische Ausführung riskanter Aktionen. Immer: Aktion → Zusammenfassung → Bestätigung → Server → Antwort → Ergebnis.
  (Server hat keine JS-"confirm()"-Absicherung → die App übernimmt diese Pflicht.)
- `os`/`lc` verschlüsselt (Keystore); niemals loggen.
- OkHttp-Logging minimal (kein Header/Body, oder nur in Debug ohne Secrets).
- Keine URLs/IDs hardcoden außer den öffentlichen Endpoint-Pfaden.
- buildTypes: debug mit deaktiviertem StrictMode-Logging, release ohne Debug-Endpoint.

---

## 12. Meilensteine (Umsetzungsreihenfolge)

| # | Modul | DoD |
|---|---|---|
| M1 | Grundgerüst | Gradle/Wrapper/Theme/MainActivity baut, App startet, leere Basis-Navigation |
| M2 | Auth/Session | Login ok, Silent-Relogin ok, Session-Guard erkannt, Secrets encryptiert |
| M3 | Dashboard | persönliche Daten (Kontostand, Team, nächste Spiele, ZAT-Status) geladen & angezeigt |
| M4 | Aufstellung-Prototyp | Platz-UI, Tippen-Interaktion, Ersatzbank, Formation, Speichern, Reload, Serverantwort |
| M5 | Taktik | 11x15 visuell, Speichern + Reload |
| M6 | Zugabgabe E2E | kompletter geführter Workflow (1–9) durchgängig, Fehlertexte erkannt |
| M7 | Spiele/Live | Livegame-JSON → Matchcenter, Berichte, Freundschaft/FSS/EM |
| M8 | Transfers | Transferliste, Gebote, VM/TM/Leihe (mit Bestätigungsdialog) |
| M9 | Nachrichten | PM-Liste/-Lesen/-Schreiben natives UI |
| M10 | Design-Polish | Dark/Light, Animationen, Touch-Feedback, Finale Navigation |

Nach jedem M: `./gradlew assembleDebug` + manueller/ADB-Test; Bugs sofort beheben.

---

## 13. Risiken & offene Punkte

- AGP 9.1 nutzt integriertes Kotlin; äußeres KGP 2.2.10 ist konfiguriert – beim ersten Build verifizieren (Fallback: KGP-Settings anpassen).
- Beta-`aufstellung`-POST exakt bei M4 verifizieren (Formularfelder re-read vom Server, nicht nur Analyse-Doku).
- Kodierung: Seiten aktuell in ISO-8859-1/latin1 geliefert → OkHttp charset handling testen (Umlaute).
- Realer Speicher-Test ist eine echte (unveränderte) Zugabgabe – Zeitpunkt bewusst wählen, Inhalt = aktuelle Aufstellung.
- Forum/WebView erst in M10 (nur falls gewünscht).
- Kein Emulator installiert → Tests via ADB an physischem Gerät oder (falls vorhanden) Emulator einrichten.

---

## 14. Erledigter Stand & offene Punkte (Status)

### Umgesetzt (kompiliert, `assembleDebug` grün)

| Bereich | Stand |
|---|---|
| M1/M2 Grundgerüst + Auth/Session | Login, Silent-Relogin via `lc`, SessionGuard, Secrets verschlüsselt |
| M3 Dashboard | persönliche Daten (Kontostand, Team, Spiele, ZAT-Status) |
| M4 Aufstellung | Beta-Aufstellung lesen + Feld-Ansicht, Detailkarte, **Tauschen** (Ersatzbank), **Speichern** (Beta-JSON POST) mit Bestätigungsdialog |
| **M4 Taktik-Auswahl (neu)** | **Formation/Taktik-Dropdown im Aufstellung-Reiter** wie auf der Webseite: `select[name=raster1]` (eigene Taktiken mit langen IDs + Standard-Taktiken 1–8, z. B. 4-4-2, 4-4-2 Raute, 3-4-3 …) wird in `Aufstellung.taktiken` geparst; „Laden“-Button ruft `wendeTaktikAn(id)` (GET `zugabgabe.php?p=0&raster1=<id>&raster=Laden`, mit Bestätigungsdialog) und lädt neu. **Geräte-verifiziert:** Dropdown zeigt exakt die Webseiten-Optionen, „4-4-2“ laden läuft, `checkza.php`-Antwort erscheint im UI |
| **M4 Nutzerauftrag (4 Punkte)** | **Umsetzung der 4 Nutzerwünsche, live auf dem Gerät verifiziert:** (1) **Mannschaftskader im Aufstellung-Reiter** – `KaderZuordnung`: alle Kaderspieler (`ra[pid]`-Selects) mit Name/Nr., Position, Skill; `ExposedDropdownMenuBox` je Spieler mit Optionen „Nicht aufgestellt“ + Slot-Codes (T→Torwart, U–Z→Ersatzbank, A–L→Feld), Zähler „Zugeordnet: X von Y“, Button „Aufstellung speichern (Kader)“. (2) **„Laden aus ZAT“** – `ZatAuswahl`: `<select name=lauf>` (0=„ZAT wählen“, ZAT 1–72) + „Laden“ → `wendeZatAn` (GET `zugabgabe.php?p=0&lauf=<ZAT>`, Bestätigungsdialog). (3) **Spielfeld-Matrix** – Ziffern (1–11) / Buchstaben (O–A) größer und **außerhalb** des Spielfelds (Margins `feldLinks`/`feldOben`), Feldfläche relativ verkleinert (aspectRatio 11/16). (4) **Reduzierte Liga-Tabelle** – `kompakteTabellenSpalten`: nur `Platz \| Team \| Sp. \| Tore (A:B) \| Diff. \| Pkt.`. **Devices-Verifikation:** ZAT 1&2 laden (feld=1,bank=0 → feld=10,bank=6), Kader-Slot „F“ gesetzt (17→18) und via `ra[pid]`-GET mit `aufspeichern` gespeichert (Server-Relaod bestätigt 1 TW+10 Feld+6 Bank), Tabelle `10:0`, `3:2` … korrekt |
| M4a **Vorbereitung (neu)** | Ersetzt Transfer-Tab (Transfers benötigen Live-Spielbetrieb). Formationswahl (4-4-2 … 4-5-1), automatisches **bestes 11er-Gerüst** (bester TW/AW/MF/ST nach Skill+Opti), **Spielweise-Vorschlag**, Button **„In Zugabgabe übernehmen“** → speichert Beta-Aufstellung (JSON) + Taktik (`taktik[]`) direkt auf dem Server (mit Bestätigung) |
| M4a Mannschaft | Kader-Ansicht (`showteam.php?s=0`), Positionsfilter + Sortierung (Skill/Opti/Alter/Nr.), Spielerzeile mit Fitness/Moral |
| M5 Taktik-Editor | Eigene visuelle 11×16-Ansicht: `Taktik.kt`/`TaktikRepository` (lesen/speichern `taktiken.php`), `TaktikViewModel`, `TaktikEditor` **eingebunden als 2. Modus** in Zugabgabe (SegmentedButton „Aufstellung \| Taktik") |
| M6 Zugabgabe E2E | Serverantwort des Speicherns wird geprüft (`erfolgreich gespeichert`-Marker, Login-View-Erkennung, Fehlertext aus `.error`) |
| M7 Spiele/Live | **Livegame-JSON-Matchcenter**: `LivegameRepository` (org.json-Parser für `data.php?action=gamedata`), Scorecard, Spielinfos, Taktik- & Statistik-Karten, Spielverlauf, letztes Spielfeld; Spiele-Tab ersetzt Placeholder |
| M9 Nachrichten/PM | **PM-Inbox nativ LIVE-verifiziert**: `PmRepository` (`/osneu/pm` Liste + `read/<id>`), `PmViewModel`, `PmScreen` (Liste/Detail), Aufruf via Postfach-Icon. Parser an echte Struktur angepasst: `div.pmrow`-Zeilen, `input[name=pmid]`, `pmunread`-Klasse, Detail-Text-Fallback `body().text()`. **Fix:** `SessionGuard.isPureLoginView()` (nur `loginemail`), da „DemoTeam" als echter PM-Gegner-Teamname vorkommt |
| M10 Design-Polish | **Theme-Toggle** (Hell/Dunkel) in der TopBar: folgt initial dem System (`BrightnessAuto`), manuelle Wahl wird in `SharedPreferences` persistiert |
| **Bewerbe-Hub (C)** | **Spieltage**: Parser auf echte `ls.php`-Struktur umgestellt (JS-Links `teaminfo`/`os_bericht`/`spielpreview`, Ergebniszelle `x : y`), **Spieltag-Auswahl** (aus `stauswahl`-Optionen), eigene Club-Hervorhebung; `berichtUrl` aus `os_bericht(H,G,typ,saison)` abgeleitet (3. Argument = Spiel-ZAT, gegen Live-Dumps verifiziert; `/rep/saison/<s>/<z>/<h>-<g>.html`) und durch die Navigation gereicht. **Ligatabelle**: `eigenZeile` wird über die eigene Team-ID (Dashboard-Wappen) markiert. **Pokal**: Ergebnis aus Spalte 2 gerendert. **Regressionstests** (`BewerbeRepositoryParseTest`) gegen echte Server-Dumps (`app/src/test/resources/dumps/*.html`) grün. **Geräte-Verifikation (R5CY53NZSRW):** Ligatabelle lädt; Spieltage zeigen Ergebnisse und öffnen den Vollbericht per statischer URL; Spieltag-Dropdown lädt andere Runden; Landespokal zeigt Runde-1-Paarungen; keine Crashes |
| Insgesamt | `./gradlew assembleDebug` grün; App auf Gerät (R5CY53NZSRW) installiert und gestartet, kein Crash |

### Noch offen (benötigen Live-Server-Verifikation bzw. Folgephasen)

| Bereich | Hinweis |
|---|---|
| Schreibaktionen | **`checkza.php` („Zugabgabe speichern“) live verifiziert** (Server-Antwort „Zugabgabe erfolgreich gespeichert" im App-UI + Dump), ebenso Taktik-Laden `raster1=…&raster=Laden`. POST-Speichern Zugabgabe-Beta/Taktik-Editor/PM-schreiben noch **nicht** live getestet |
| M8 Transfers | **Bewusst durch „Vorbereitung“ ersetzt** (Transfer findet Live statt; siehe Wunsch-Änderung) – TM/VM/Leihe nur mit Live-Playback sinnvoll |
| PM schreiben | `/osneu/pm?action=writeNew` (POST) noch nicht abgebildet (Lesen/Löschen nativ vorhanden) |
| Spieleberichte | Spielberichte (HTML/XML) und ZAT-Zusammenfassung für ausgewachsene Saisonansicht; statische Bericht-URLs aus Spieltagen sind **auf dem Gerät klick-verifiziert** (SC Viktoria Ulm 2:2 Kaiserslautern öffnete den Vollbericht) |

> Alle Parser/Formulare sind defensiv (Jsoup) und nach Phase-2-Vorgaben gebaut; eine echte
> Verifikation gegen den Live-Server (mit eigenem Konto, Lesezugriffe) ist im Betriebsmodus nötig,
> vor allem die POST-Formulare (`zugabgabe_beta.php`, `taktiken.php`). Hinweis: „Laden“ einer
> Standard-Taktik erzeugt serverseitig eine Kopie „Vorbereitung <Name>“ in der eigenen
> Taktikliste (wir haben das live beobachtet).