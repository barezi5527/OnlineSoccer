# Elf des Spieltags – Feature-Dokumentation

Stand: 15.09.2026 · Online‑Soccer Android‑App (`app/`)

## Überblick

Die Funktion „Elf des Spieltags" wertet die echten Spielberichte eines Spieltags
der gewählten Land/Liga/Saison/Kombination lokal im Gerät aus und zeigt die
bestbewerteten Elf auf einem Spielfeld (dynamisch gewählte Formation). Die Elf
wird erst nach der Auswahl Land → Liga → Spieltag für genau diese Kombination
berechnet; andere Ligen/Spieltage werden nie vorab berechnet.

- **Kein zentraler Server** für die Elf – alles wird aus den öffentlichen
  Spielberichten abgeleitet und transparent bewertet.
- **Freie Land‑Liga‑Auswahl** (Dropdowns) statt fest hinterlegter Liga: Die
  Optionen kommen bedarfsgerecht aus der Server‑Spieltagsansicht (`ls.php`);
  ohne Auswahl gilt die eigene Liga des Benutzers (Server‑Standardansicht).
- **Sauberer Kontext‑Wechsel**: Beim Wechsel von Land/Liga/Saison wird der
  laufende Ladevorgang abgebrochen, das bisherige Ergebnis sofort verworfen und
  nur die neue Kombination geladen; der Cache trennt zwingend je
  Server, Nutzer, Liga, Land, Saison und Spieltag.
- **Bewertungsskala 1,0–10,0 ohne Normalisierung** – der beste Spieler bekommt
  nicht automatisch 10 Punkte, sondern genau die Note seiner tatsächlich
  berichteten Leistung.
- **Reproduzierbar**: Die gespeicherte (gecachte) Elf lässt sich aus den
  Kandidatendaten jederzeit exakt neu berechnen (Bewertungsdetails-Dialog).

## Datenfluss

```
ls.php  (Spieltag der Liga, Parameter liga/land/saison/zat)
  └─ LigaSpieltag (Spiele + berichtUrl)
       └─ staticher Bericht  rep/saison/<saison>/<zat>/<heim>-<gast>.html
            └─ SpielBericht (Aufstellung, Ticker-Ereignisse, Endstand)
                 └─ ElfAuswertung.kandidatenAusBericht  → List<ElfKandidat>
                      └─ ElfBewertung.bewerten  (1,0–10,0)
                           └─ ElfAuswahl.erstelleElf  (dynamische Formation, Fallback)
                                └─ ElfErgebnis  → Cache (ElfCache v6)
                                     └─ UI (Spielfeld, Dialoge)
```

## Realer End‑zu‑End‑Test gegen den Server

Test: `app/src/test/java/.../ElfDesSpieltagsE2eTest.kt`
(Startet `ls.php` + 10 Spielberichte gegen `https://os.ongapo.com`, Gast‑Session;
wartet nicht auf Zugangsdaten.)

### Prüfobjekt 1: England 1. Liga, Saison 24, Spieltag 1 (anonym)

| Kennzahl | Wert |
|---|---|
| Begegnungen | 10 (alle gespielt) |
| Spielberichte erfolgreich geladen | 10 / 10 |
| Kandidaten (Spieler) gesamt | 220 (22 je Spiel) |
| Elf | vollständig, dynamische Formation 3‑4‑3 (1 TOR, 3 ABW, 4 MIT, 3 STU) |
| Spieler des Spieltags | Ebulfez Mammadov (FC Moor) – 9,4 |
| 10,0‑Bewertungen | 0 von 11 (keine Normalisierung) |

Formationswahl: Es wird die erste erlaubte Formation (Reihenfolge 4‑2‑3‑1,
4‑3‑3, 4‑4‑2, 4‑4‑2 Raute, 4‑1‑4‑1, 3‑5‑2, 3‑4‑3, 4‑5‑1, 5‑3‑2, 5‑4‑1)
gewählt, deren Positionen sich mit den bestbewerteten Spielern vollständig
füllen lassen (maximale Notensumme; nichts wird erzwungen, wenn eine Formation
nicht füllbar ist).

Mittelfeld-Struktur: Die Spielberichte nennen nur Torwart/Abwehr/Mittelfeld/
Sturm. Aus dieser Mittelfeld-Gruppe verteilt die gewählte Formation die
bestbewerteten Spieler auf die taktischen Linien OMI (offensiv) → MIT →
DMI (defensiv). Das ist eine reine Darstellung der Formation im Spielfeld –
die Bewertung bleibt unverändert, weil OMI/DMI bei der Notenberechnung
wie MIT behandelt werden (funktional identisch, siehe [Bewertungsmodell]).

Resultierende Elf (Noten absteigend):

| Platz | Spieler | Verein | Position | Note | Kennzahlen |
|---|---|---|---|---|---|
| 1 | Ebulfez Mammadov | FC Moor | STU | 9,4 | 3 Tore, 1 Vorlage |
| 2 | Chisha Chisala | AC Northampton | MIT | 8,8 | 1 Tor, 3 Vorlagen |
| 3 | Elvis Voorhees | FC Leicester | STU | 8,8 | 3 Tore |
| 4 | Sven Simonsen | Blackburn City | STU | 7,8 | 2 Tore |
| 5 | Chad Halford | FC Leicester | MIT | 7,6 | 3 Vorlagen |
| 6 | Tibor Andrasi | Blackburn City | MIT | 7,4 | 3 Vorlagen, 1 Gelb |
| 7 | Daryll Trumble | FC Moor | MIT | 7,0 | 2 Vorlagen |
| 8 | Jupp Petrasen | AC Northampton | ABW | 6,4 | 1 Vorlage |
| 9 | Elvis Arnaldi | FC Leicester | TOR | 6,3 | Zu‑Null‑Spiel |
| 10 | Panfilo Licci | FC Leicester | ABW | 6,1 | Zu‑Null‑Spiel |
| 11 | Elvis Delman | FC Leicester | ABW | 6,1 | Zu‑Null‑Spiel |

Stichprobe „Newcastle Glory – FC Moor" (1:4): Ebulfez Mammadov exakt **3 Tore,
1 Vorlage, 60 Einsatzminuten, 10 Schüsse (9 aufs Tor)** – im Abgleich mit der
Spielerstatistik-Tabelle des Rohberichts bestätigt. Wegen der reinen
Aktions‑+‑Endstands‑Formel schafft selbst der beste Stürmer des Spieltags keine
10,0 (9,4 < 9,8-Schwelle).

### Prüfobjekt 2: Persönliche Liga (Gerät, angemeldete Session)

- **Liga:** Deutschland · 2. Liga B · Saison 24 (SC Viktoria Ulm)
- **Spieltag 2:** 9 Begegnungen, 9/9 Berichte ausgewertet, vollständige dynamische Elf
  (Geräte-Lauf; Noten unter dem Bewertungs-Rework stehen noch aus)
- Spieltag 1 genauso erfolgreich geladen und ausgewertet (9 Begegnungen)

Hiermit ist auch die **dynamische Liga‑Erkennung** (Kontext des Benutzers) am
Gerät nachgewiesen.

## Im Quellen-Code gefundene und behobene Parser‑Bugs

Reale Berichte verwenden andere Formate als die ursprünglich erwarteten:

1. **Tore + Vorlagen:** `Neuer Spielstand: 0:1 (Ebulfez Mammadov, David Evian)`
   → Torschütze = erster Klammername, Vorlage = zweiter. Alte Logik nahm die
   ganze Klammer als einen Namen und verlor das Tor.
   → `ElfAuswertung.torUndVorlagen`
2. **Wechsel:** `… wechselt: Marc Taucher kommt für David Evian` (mehrere pro
   Eintrag möglich). Alte Regex erkannte „kommt für" nicht → Einsatzminuten
   fehlten. → `ElfAuswertung.kommtFuerRegex` + `einsatzMinuten`
3. **Karten:** `FREISTOSS Anthony Colt kassiert dafür die gelbe Karte`. Alte
   „genau‑ein‑Name"-Regel scheiterte, weil ein Minuten‑Segment viele Namen
   enthält. → `ElfAuswertung.kassiertRegex` / `spielerDerKarte`
4. **Subtil:** `String.substring(range.last)` war 1 Zeichen zu früh (Kotlin
   `MatchResult.range.last` ist inklusiv) → `range.last + 1`.

## Bewertungsmodell (ElfBewertung)

Jeder eingesetzte Spieler startet bei **5,5**. Genutzt werden **exakt sechs
Kriterien**, alle ausschließlich aus den im Spielbericht tatsächlich vorhandenen
Werten:

1. **Grundbewertung** – immer **5,5**.
2. **Tore** – positionsabhängig je Tor: STU **+1,0** · MIT/OMI/DMI **+1,2** ·
   ABW **+1,5** · TOR **+2,0**. Mehrere Tore/Vorlagen addieren sich.
3. **Vorlagen** – **+0,6** je Vorlage.
4. **Ergebnis** – nur bei bekanntem Endstand (`hatErgebnis`): Sieg **+0,3**,
   Unentschieden **+0,1**, Niederlage **−0,2**. Ohne Endstand weder Bonus noch Malus.
5. **Zu null** – nur bei bekanntem Endstand und 0 Gegentoren: TOR **+0,5**,
   ABW **+0,3** (andere Positionen kein Bonus).
6. **Karten** – je Gelb **−0,2**; je Rot-Ereignis **−1,0** (kein Doppel-Abzug: ein
   Rot-Ereignis wird nicht zusätzlich als Gelb gewertet – Summe aus `gelbeKarten`
   und `roteKarten`, siehe Einschränkung unten).

- **Keine künstlichen Zusatzgrößen:** Zweikämpfe, Schüsse, Passquote/xG, Paraden,
  Auffälligkeit, Elfmeter-Treffer und Einsatzminuten fließen bewusst **nicht** ein.
  Die Einsatzzeit ist kein Bonus – ein kurzer Einsatz mit Tor erhält denselben
  Torbonus, aber nie einen „Vollspielbonus".
- **Nicht eingesetzte** Spieler (keine berichtete Einsatzzeit, `hatEinsatz` == false)
  werden von der Elf ausgeschlossen. Die Einsatzzeit selbst wird weiter aus
  berichteten Wechseln ermittelt (`einsatzMinuten`, nie geschätzt; ohne Angabe
  startelf = volle Partie, eingewechselt ohne Ereignis = 0 Min.).
- Rohbewertung auf **eine Nachkommastelle** gerundet, Skala 1,0–10,0. Eine
  **10,0 verlangt eine ungerundete Rohbewertung ≥ 9,8**; darüber wird bei 10,0
  gekappt. **Keine Normalisierung** – eine Elf ganz ohne 10,0 ist ausdrücklich in
  Ordnung (im realen E2E-Lauf: 0 von 11).
- **Einschränkung Rot-Ereignis:** Der Parser unterscheidet Gelb-Rot („zweite Gelbe")
  nicht von der direkten Roten Karte – beide landen als ein `roteKarten`-Ereignis.
  Der Malus folgt der üblichen Form Gelb-Rot (−1,0); die strengere direkte Rote
  Karte (−2,0) ist ohne erfundene Schätzung nicht abgrenzbar und wird bewusst nicht
  angesetzt.
- **Formation**: Reihenfolge 4‑2‑3‑1, 4‑3‑3, 4‑4‑2, 4‑4‑2 Raute, 4‑1‑4‑1,
  3‑5‑2, 3‑4‑3, 4‑5‑1, 5‑3‑2, 5‑4‑1. Gewählt wird die erste Formation, die
  sich aus den bestbewerteten Spielern vollständig füllen lässt (maximale
  Notensumme). Ist keine füllbar, gilt die bestbewerteten 11 ohne Formationslabel
  (`formation = null`). Die Mittelfeld-Linien (OMI/MIT/DMI) entstehen rein aus
  der Formation für die taktische Anzeige; OMI/DMI werden in der Bewertung
  wie MIT behandelt — die Note ändert sich dadurch nie.

## Cache (ElfDesSpieltagsRepository / ElfCache)

- Klartext‑Serialisierung ohne Fremdbibliothek, Versions-Prefix `ElfDesSpieltags|v5`,
  24 Felder je Spieler (inkl. teamId für das Vereinswappen, Zweikämpfe, Schüsse,
  Auffälligkeit, gehaltene Bälle) plus Formations-Label im Kopf.
- Schlüssel: Server + Benutzer‑Team‑ID (falls bekannt) + Land + Liga + Saison + Spieltag.
- Qualitätseigenschaften (per Unit‑Tests abgesichert): Deserialisierung == Original,
  Note aus dem Cache exakt per `ElfBewertung.bewerten(kandidatVon(spieler))` reproduzierbar,
  abgelaufene/ungültige Versionen (v1/v3) werden verworfen und neu ermittelt.

## Bekannte Grenzen (Server-seitig)

- **Keine Spieler‑IDs in den statischen Berichten** (`spielerinfo` kommt 0× vor):
  „Spielerkarte öffnen" kann ohne echte ID nicht springen – die App zeigt
  stattdessen einen ehrlichen Hinweis statt erfundener Daten.
- **Statische Berichte enthalten keinen `os_bericht(...)`‑Anker** → `heimId`,
  `gastId`, `saison`, `zat` eines Berichts sind auf diesem Weg null; die ID‑basierten
  Vereinfachungen werden über die berichteigenen IDs nur genutzt, wenn die
  Session‑geschützte Hülle `bericht.php?s=` verfügbar ist.
- **Ohne Anmeldung** ist keine persönliche Liga ableitbar (keine
  `option[selected]`); dann liefert `ladeKontext()` null und die App zeigt den
  Anmeldehinweis.

## Automatisierte Tests

- `ElfDesSpieltagsTest.kt` – Bewertung (Formel 5,5 + Tore/Vorlagen/Ergebnis/Zu‑null/
  Karten, Positionsprofile, Runden/10,0-Schwelle, Karten-Doppel-Abzug, kein
  Vollspielbonus, Ausschluss nicht eingesetzter Spieler), Transparenz, keine
  Normalisierung, dynamische Formationswahl (3‑5‑2-Fall, Lücken-Fallback, OMI/MIT/DMI-Verteilung,
  Deduplizierung, Elf ohne 10,0), Auswertung (Startelf/Bank, Tore, Karten,
  Kapitäns-Erkennung, Spielerstatistik, gehaltene Bälle, Auffälligkeit), Cache (v6-Round-Trip,
  v3/v4/v5-Verwerfung), Spieler des Spieltags, alle Parser‑Formate.
- `ElfDesSpieltagsE2eTest.kt` – realer Lauf gegen den Server (s. o.), Cache‑Round‑Trip,
  Transparenz‑Check (Bewertung == Neuberechnung aus Fake‑Kandidat),
  10,0‑Zählung (0 von 11 ohne Normalisierung).

Ausführen: `./gradlew :app:testDebugUnitTest` (aktuell komplett grün).

## Ist-Zustand auf dem Gerät

Debug‑APK mit dem Bewertungs-Rework ist gebaut (`assembleDebug`), die
Geräte-Verifikation der neuen Noten/Formation steht noch aus (Porträt hell/dunkel,
Querformat, dynamisches Spielfeld, Spieler‑Dialoge, „Bewertungsdetails",
Spieltag‑Auswahl 1–17, Auswahl anderer Spieltage).

**Dabei behobener Layout‑Bug:** Im Querformat verschlang die feste
„So wurde bewertet"-Box fast die gesamte Höhe und drückte das Spielfeld auf ≈0.
Fix: Ergebnisinhalt scrollbar, Spielfeld mit Mindesthöhe
(`ElfDesSpieltagsScreen.ElfErgebnisInhalt`).