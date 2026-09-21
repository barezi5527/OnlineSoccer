# Bericht: Vertragshinweis „Vertrag läuft noch X ZAT“ im Managerbüro

Stand: 21.09.2026 · Nur Analyse, noch keine Umsetzung

## 1. Auftrag

Auf der Website wird im **Managerbüro (Dashboard, `haupt.php`)** für einen Spieler
mit einer Vertragslaufzeit über 1 ZAT ein Hinweis **„Vertrag läuft noch X ZAT“**
angezeigt. Geprüft werden soll, ob die App diesen Hinweis bereits kennt und wie
er bei Bedarf übernommen werden kann. Vor einer Umsetzung ist dieser Bericht zu
erstellen.

## 2. Ausgangslage Website

- ZAT = Zugabgabetermin (2× pro Woche); Vertragslaufzeiten werden in ZAT angegeben.
- Der Hinweis erscheint im **Managerbüro**, nicht in der Spielerkarte, im Kader
  oder auf den Transferseiten.
- Inhalt: „Vertrag läuft noch X ZAT“ – also die verbleibende Vertragsdauer als
  Hinweis/Warnung.

## 3. Bestandsaufnahme App

### 3.1 Dashboard ist bereits angebunden

- `DashboardRepository` lädt `haupt.php` (`OsApi.MAIN`) mit 30-s-Cache
  (`DashboardRepository.kt:50-69`, Cache `FRISCH_MS` in `DashboardRepository.kt:207`).
- Der Parser liest aus dem Managerbüro bereits:
  - Begrüßung/Teamname und Liga (`DashboardRepository.kt:73-93`)
  - Wappen/Team-ID und Logo (`:95-97`)
  - Forum-Link (`:98-103`)
  - nächsten ZAT samt Datum (`:105-107`)
  - Kennzahlen Logins, Zugabgabe, Kontostand, PMs, FSS-Einladungen (`:109-123`)
  - letztes/nächstes Spiel (`:125-154`)
- Ergebnis ist `DashboardData` (`DashboardData.kt:4-32`) und wird im
  `DashboardScreen` als Karten-Stapel gerendert (`DashboardScreen.kt:120-184`).

### 3.2 Vertragsdaten existieren in der App, aber an anderer Stelle

- `VertragZeile.laufzeit` (`TeamDetails.kt:88-93`) wird aus der Vertragsansicht
  geparst (`TeamRepository.kt:989`, `:1473`) und angezeigt in
  `VertraegeAnsicht` (`TeamScreen.kt:374-377`) sowie in der Spielerkarte
  (`SpielerkarteScreen.kt:350`).
- Diese Daten stammen aus **anderen Seiten** als dem Dashboard und werden dort
  **nicht** zu einem „läuft noch X ZAT“-Hinweis verdichtet.

### 3.3 Ergebnis

Der gesuchte Hinweis wird **derzeit nicht geparst und nicht angezeigt**. Es gibt
weder ein Feld in `DashboardData` noch eine Karte im `DashboardScreen` dafür.
`grep` nach „läuft noch“/„Vertrag läuft“ liefert in Code und Test-Dumps **keinen
Treffer** – die Funktion fehlt vollständig.

## 4. Live-Prüfung (21.09.2026, eingeloggt als SC Viktoria Ulm)

`haupt.php` und die relevanten Seiten wurden live abgerufen und auf
„Vertrag läuft noch" / „läuft" / „Vertrag" geprüft:

| Seite | URL | Hinweis gefunden? |
| --- | --- | --- |
| Managerbüro/Dashboard | `haupt.php` | nein (nur Begrüßung, ZAT, Spiele, Kennzahlen, Tipp) |
| Kader | `showteam.php?s=0` | nein |
| Vertragsdaten | `showteam.php?s=1` | nein (Spalte „Vertrag" mit ZAT-Zahlen) |
| Verträge verlängern | `vt.php` | nein (Spalte „Laufzeit") |
| Transferstatus | `tstatus.php` | nein (nur Mindestablöse/Transferinfotext) |
| Transferliste | `osneu/transferliste` | nein (Spalte „D" = Dauer in ZAT) |
| Schnelltransfer | `blitz.php` | nein (Spalte „Laufzeit") |
| Spielerkarte | `sp.php?s=<pid>` | nein (nur „Vertragslaufzeit: 23") |
| Trainer | `trainer.php` | nein (Spalte „Vertrag") |
| PM, Zugabgabe, Kontoauszug, Jugend, zfeatures, transfer/leihspieler | – | nein |

Der exakte Wortlaut „Vertrag läuft noch X ZAT" ist damit **im aktuellen
Zustand des Kontos nicht auffindbar**. Er erscheint offenbar nur konditional
(z. B. nur wenn ein Vertrag kurz vor dem Auslaufen steht; der kürzeste Vertrag
im Kader beträgt aktuell 6 ZAT) oder auf einer noch nicht identifizierten
Seite.

### Offene Fragen (Blocker für exakten Parser)

- Auf welcher Seite erscheint der Hinweis **konkret** und unter welcher
  Bedingung (Restlaufzeit ≤ 1 ZAT, Transferstatus T/A, Leihe, Trainer)?
- Container/Element, in dem der Hinweis steht (Zelle, `<b>`, `<div>`, Liste?),
- exakter Wortlaut (Singular/Plural, mehrere Spieler gleichzeitig?),
- ob Name/ID des Spielers und die ZAT-Zahl verlinkt/ausgezeichnet sind,
- ob der Hinweis nur bei > 1 ZAT erscheint oder auch bei genau 1 ZAT.

## 5. Lösungsvorschlag (nach Vorliegen des Dumps)

1. **Datenmodell** (`DashboardData.kt`): neues Feld
   `vertragsHinweise: List<VertragsHinweis> = emptyList()` mit
   `data class VertragsHinweis(val name: String, val pid: Int?, val zat: Int?, val text: String)`.
2. **Parser** (`DashboardRepository.parse`): aus dem Managerbüro-Segment die
   Hinweiszeile(n) per Regex `Vertrag läuft noch (\d+) ZAT` extrahieren; Name/PID
   über `javascript:spielerinfo(...)`/`teaminfo(...)`-Link, falls vorhanden.
3. **UI** (`DashboardScreen.kt`): neue `VertragsHinweisCard` analog zu
   `ZugabgabeStatusCard`/`SummaryCard`, farblich hervorgehoben
   (`errorContainer`/`tertiaryContainer`), Einblendung nur wenn Liste nicht leer.
   Optionaler Klick → Spielerkarte über vorhandene PID-Navigation.
4. **Tests** (`DashboardRepositoryParseTest.kt`): neuer Testfall mit
   Managerbüro-HTML, der Zahl, Namen und Leerfall (kein Hinweis) prüft.
5. **Cache**: keine Änderung nötig, Hinweis kommt mit dem 30-s-Dashboard-Cache.

## 6. Risiken

- Ohne echten Dump ist der Parser spekulativ und könnte bei Layout-Änderungen
  brechen (gleiches Risiko wie bei den übrigen Dashboard-Feldern, dort mit
  `selectFirst`/Regex gelöst).
- Mehrere gleichzeitige Hinweise erfordern eine Liste statt eines Einzelwerts.
- Verwechslungsgefahr mit `Transferinfotext` (`tstatus.html`) – bewusst
  getrenntes Feature.

## 7. Empfehlung / nächster Schritt

Zuerst einen **`haupt.php`-Dump (eingeloggt) oder HTML-Schnipsel des
Managerbüro-Hinweises** bereitstellen. Danach Umsetzung nach Abschnitt 5 inkl.
Test; anschließend `./gradlew :app:testDebugUnitTest` und
`./gradlew :app:compileDebugKotlin` als Verifikation.
