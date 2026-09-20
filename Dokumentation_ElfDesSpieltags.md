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
                                └─ ElfErgebnis  → Cache (ElfCache v7)
                                     └─ UI (Spielfeld, Dialoge)
```

## Realer End‑zu‑End‑Test gegen den Server

Test: `app/src/test/java/.../ElfDesSpieltagsE2eTest.kt`
(Startet `ls.php` + 10 Spielberichte gegen `https://os.ongapo.com`, Gast‑Session;
wartet nicht auf Zugangsdaten.)

### Prüfobjekt 1: England 1. Liga, Saison 24, Spieltag 1 (anonym)

| Kennzahl | Wert |
|---|---|---|
| Begegnungen | 10 (alle gespielt) |
| Spielberichte erfolgreich geladen | 10 / 10 |
| Kandidaten (Spieler) gesamt | 220 (22 je Spiel) |
| Elf | vollständig, dynamische Formation 3‑4‑3 (1 TOR, 3 ABW, 4 MIT, 3 STU) |
| Spieler des Spieltags | Ebulfez Mammadov (FC Moor) – 9,3 |
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

Resultierende Elf (Noten absteigend, Stand Bewertungsmodell K1–K5):

| Platz | Spieler | Verein | Position | Note | Kennzahlen |
|---|---|---|---|---|---|
| 1 | Ebulfez Mammadov | FC Moor | STU | 9,3 | 3 Tore, 1 Vorlage |
| 2 | Chisha Chisala | AC Northampton | MIT | 9,2 | 1 Tor, 3 Vorlagen |
| 3 | Elvis Voorhees | FC Leicester | STU | 9,0 | 3 Tore |
| 4 | Sven Simonsen | Blackburn City | STU | 8,3 | 2 Tore |
| 5 | Tibor Andrasi | Blackburn City | MIT | 8,2 | 3 Vorlagen, 1 Gelb |
| 6 | Chad Halford | FC Leicester | MIT | 8,0 | 3 Vorlagen |
| 7 | Alexander Lenert | Luton City | MIT | 7,6 | 2 Vorlagen |
| 8 | Risto Rakitic | Tottenham United | TOR | 6,9 | Zu‑Null‑Spiel |
| 9 | Mario Rizzi | FC Moor | ABW | 6,9 | Zu‑Null‑Spiel |
| 10 | Elvis Delman | FC Leicester | ABW | 6,8 | Zu‑Null‑Spiel |
| 11 | Stratos Kromidas | Gravesend AFC | ABW | 6,8 | Zu‑Null‑Spiel |

Stichprobe „Newcastle Glory – FC Moor" (1:4): Ebulfez Mammadov exakt
**3 Tore, 1 Vorlage, 60 Einsatzminuten** – im Abgleich mit der
Spielerstatistik-Tabelle des Rohberichts bestätigt. Seine Aufschlüsselung
(9,3): Grundbewertung +5,5 · Direkter Impact +1,9 · Effizienz +1,0 ·
Zweikämpfe +0,6 · Ergebnis & Teambonus +0,4. Auch der beste Stürmer des
Spieltags schafft keine 10,0 (9,3 < 9,95-Schwelle; reale Berichte liefern
keine Bericht-Note, siehe [Bewertungsmodell]).

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

Die Note entsteht ausschließlich aus der im Spielbericht tatsächlich
vorhandenen Information nach der additiven Formel

    Endnote = Basis + K1 + K2 + K3 + K4 + K5 − Karten

Basis: Startelf **5,5** · Einwechsler **4,0** (fix, kein Minuten‑Faktor). Die
Leistungszuschläge werden nach Einsatzzeit **nicht** skaliert; die Einsatzminuten
entscheiden nur, ob ein Spieler überhaupt als eingesetzt gilt – nicht eingesetzte
Bank‑Spieler werden von der Elf ausgeschlossen. Die fünf Kategorien sind durch
feste Budgets (Caps) begrenzt; deren Summe (4,9) setzt die Zielverteilung
41/22/18/11/8 um:

1. **K1 Direkter Impact** (Cap **2,0**): Tore – positionsabhängig je Tor: STU
   **+0,5** · MIT/OMI/DMI **+0,6** · ABW **+0,8** · TW **+1,0**; für 1–2 Tore gilt
   zusätzlich ein Torbonus‑Deckel von **1,0** (mehr als ein Tor zahlt nur noch
   anteilig in den Deckel). Ab dem **3. Tor** bricht der Hattrick (`+1,5`) und
   ab dem 4. Tor (`+2,0`) den Deckel – positionsunabhängig. Vorlagen: **+0,35**
   je Vorlage, mehrfach, bis zur 4. linear (max. **1,4**), positionsunabhängig.
   Verwandelter Elfmeter zusätzlich **+0,2** (aus dem Tickerverlauf „verwandelt").
2. **K2 Effizienz & Spielkontrolle** (Cap **1,1**, Proxy): Schussquote
   (`aufsTor/schuesse`, nur bei `schuesse ≥ 3`): ≥40 % 0,2 · ≥50 % 0,3 ·
   ≥60 % 0,4 · ≥75 % 0,5; Abschlusspräsenz (`aufsTor ≥2` 0,1 · `≥4` 0,2);
   Auffälligkeit aus den Ticker‑Nennungen (2–3 0,1 · 4–5 0,2 · 6+ 0,3).
3. **K3 Zweikämpfe** (Cap **0,9**): **+0,03** je gewonnener Zweikampf (ZK), max.
   **0,6** (Deckel ab ~20 Duellen), plus Zweikampfquote‑Stufe (Stufen: ≥45 % 0,10 ·
   ≥50 % 0,15 · ≥60 % 0,2 · ≥65 % 0,25 · ≥70 % 0,3; nur eine Stufe zählt) – nur bei
   vorhandener Statistik (`zweikaempfe > 0`). Beim Torwart ersetzt die Zahl der
   **gehaltenen Bälle** die ZK‑Werte (3–4 0,3 · 5–6 0,5 · 7+ 0,7).
4. **K4 Ergebnis & Teambonus** (Cap **0,55**): Sieg **+0,35** · Unentschieden
   **+0,1** · Niederlage **−0,2** (Malus ungedeckelt); Zu‑null: TW **+0,3** ·
   ABW **+0,2** – jeweils nur bei bekanntem Endstand (`hatErgebnis`).
5. **K5 Bericht‑Note** (Cap +0,35/−0,15): Bericht‑Note 1,0–6,0 aus der
   Spielerstatistik (1,0 = beste Note): 1,0 +0,35 · 1,5 +0,30 · … · 4,0 +0,05 ·
   4,5 0 · 5,0 −0,05 · 5,5 −0,10 · 6,0 −0,15. Ohne Note im Bericht bleibt der
   Beitrag 0 (in realen Berichten gibt es keine Note → K5 = 0).
6. **Karten** (unskaliert, extra): je Gelb **−0,2** · je Rot‑Ereignis **−1,0**
   (kein Doppel‑Abzug: ein Rot‑Ereignis wird nicht zusätzlich als Gelb gewertet).

- **Deckel statt Normalisierung**: Die Caps (K1 2,0 · K2 1,1 · K3 0,9 · K4 0,55 ·
  K5 0,35) sorgen dafür, dass extreme Einzelleistungen (4+ Tore, 4+ Vorlagen,
  20+ Duellen…) bis zur genannten Stufe voll zählen, darüber hinaus aber nicht
  übermäßig reinkommen – ein Kanterspiel erzeugt keine 10,0-Flut. Die Summe der
  sichtbaren Kriterienzeilen entspricht exakt der Rohbewertung.
- **Rohbewertung ist transparent**: auf **eine Nachkommastelle gerundet**; eine
  **10,0 verlangt eine ungerundete Rohbewertung von mindestens 9,95**, darunter
  wird bei 9,9 gekappt. **Keine Normalisierung** – eine Elf ganz ohne 10,0 ist
  ausdrücklich in Ordnung (im realen E2E-Lauf: 0 von 11).
- **Fehlende Daten** (kein Endstand, keine Zweikampf‑Statistik, keine
  Schussstatistik, keine Bericht‑Note) erzeugen weder Bonus noch Malus.
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

- Klartext‑Serialisierung ohne Fremdbibliothek, Versions-Prefix `ElfDesSpieltags|v7`,
  26 Felder je Spieler (inkl. teamId für das Vereinswappen, Zweikämpfe, Schüsse,
  Auffälligkeit, gehaltene Bälle, Bericht-Note) plus Formations-Label im Kopf.
- Schlüssel: Server + Benutzer‑Team‑ID (falls bekannt) + Land + Liga + Saison + Spieltag.
- Qualitätseigenschaften (per Unit‑Tests abgesichert): Deserialisierung == Original,
  Note aus dem Cache exakt per `ElfBewertung.bewerten(kandidatVon(spieler))` reproduzierbar,
  abgelaufene/ungültige Versionen (v3/v4/v5/v6) werden verworfen und neu ermittelt.

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

- `ElfDesSpieltagsTest.kt` – Bewertung (K1–K5-Formel mit Caps und Stufen:
  Tor-Deckel/‑Tiers bis Hattrick & 4+ Tore, Vorlagen-Deckel bis zur 4. Vorlage,
  Zweikampf-Quantität/‑Quote, Elfmeter, Zu‑null, Ergebnis, Bericht-Note,
  Positionsprofile, Runden/10,0-Schwelle ≥ 9,95, Karten-Doppel-Abzug, kein
  Vollspielbonus, Ausschluss nicht eingesetzter Spieler, Kanterspiel-Stresstest:
  Reihenfolge stabil und keine 10,0-Flut), Transparenz, keine Normalisierung,
  dynamische Formationswahl (3‑5‑2-Fall, Lücken-Fallback, OMI/MIT/DMI-Verteilung,
  Deduplizierung, Elf ohne 10,0), Auswertung (Startelf/Bank, Tore, Karten,
  Kapitäns-Erkennung, Spielerstatistik, gehaltene Bälle, Auffälligkeit), Cache (v7-Round-Trip,
  v3/v4/v5/v6-Verwerfung), Spieler des Spieltags, alle Parser‑Formate.
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