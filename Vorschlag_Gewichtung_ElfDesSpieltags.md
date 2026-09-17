# Vorschlag: Finale Gewichtung der „Elf des Spieltags"-Bewertung

Version 2 · Stand 17.09.2026

**Gegenüber Version 1 geändert:**
- Einwechsler-Basis **4,0** statt 5,5 + Minuten-Faktor (keine Skalierung der Zuschläge).
- **Zweikämpfe** werden konkret als eigene Kategorie **K3** mit genauen Zahlen integriert
  (Statistik am Ende des Spielberichts: `ZK` = gewonnene Zweikämpfe, `ZK-%` = Quote).
- Zielverteilung 35/25/20/12/8 bleibt; Kategorie-Caps weiterhin über additive Beiträge.

---

## 1. Zielbild

- **Grundmodell bleibt additiv:**
  ```
  Endnote = Basis + K1 + K2 + K3 + K4 + K5 − Karten
  ```
- **Basis:** Startelf **5,5** · Einwechsler **4,0**. Die niedrigere Basis bildet die
  kürzere Spielzeit ab – **ohne** die Leistungszuschläge eines Einwechslers zu
  beschneiden (Star-Joker darf entsprechend stark steigen).
- **Zuschläge (K1–K5) werden nicht nach Minuten skaliert.** Die Einsatzzeit bleibt nur
  Filter für `hatEinsatz` (wer gar nicht spielt, kommt nicht in die Elf).
- **Zielverteilung** 35/25/20/12/8 wird über **maximale Kategorienbeiträge** (Caps)
  erreicht. Summe der Caps = **4,5** → theoretisches Maximum Starter 10,0, Einwechsler 8,5.
- **Strafpunkte (Karten)** liegen außerhalb des Budgets und werden nicht skaliert –
  ein Platzverweis wiegt unabhängig von der Spielzeit voll.

### Kategorie-Budgets (Ziel-Maxima über der Basis)

| Kategorie | Ziel-Anteil | Max. Beitrag | Belegte Daten |
|---|---|---|---|
| K1 Direkter Impact | 35 % | **+1,6** | Tore, Vorlagen, verwandelter Elfmeter |
| K2 Effizienz & Spielkontrolle | 25 % | **+1,1** | Schussquote, Abschlüsse aufs Tor, Auffälligkeit *(Proxy)* |
| K3 Zweikämpfe | 20 % | **+0,9** | Gewonnene Zweikämpfe (`ZK`), Zweikampfquote (`ZK-%`), gehaltene Bälle (TW) |
| K4 Ergebnis & Teambonus | 12 % | **+0,55** | Sieg/Unentschieden/Niederlage, Zu-Null |
| K5 Subjektiver Faktor | 8 % | **+0,35** | Bericht-Note |
| **Summe** | 100 % | **4,5** | Starter max. Roh ≈ 5,5+4,5 = 10,0 · Einwechsler max ≈ 8,5 |

Eine **10,0 bleibt einer überragenden Leistung vorbehalten**: Sie erfordert eine
ungerundete Rohnote ≥ 9,95, also das gleichzeitige Ausschöpfen fast aller Kategorie-Caps
(Summe 4,5 über der Basis). Selbst ein sehr starker Spieltag erreicht nach der neuen
Verteilung nur ~8,5–9,3 (Beispiel A: 8,6). **Einwechsler** (Basis 4,0) kommen rechnerisch
nur bis **8,5** – die 10,0 ist damit eine Startelf-Domäne. Die bestehende 9,8-Regel
(`ElfBewertung.kt:296-304`) bleibt: Eine 10,0 wird nur ausgegeben, wenn der ungerundete
Rohwert ≥ 9,95 ist; darunter wird nie auf 10,0 aufgerundet.

---

## 2. Basis-Note: Startelf 5,5 / Einwechsler 4,0

- **Startelf** → **5,5** (wie bisher, „5,5 für Startelf ist in Ordnung“).
- **Einwechsler** → **4,0** (fix, unabhängig davon, in welcher Minute er kam).
- Entscheidend ist nur das `startelf`-Flag aus der Aufstellung (`Bericht.kt:22-24`).
- Der Einwechsler mit starkem Einfluss wird **voll** honoriert: Jede Aktion addiert sich
  ungeschmälert auf die 4,0-Basis. Ein beliebiger Joker ohne Aktion bleibt bei 4,0.

| Szenario | Basis | Zuschläge (Beispiel) | Endnote |
|---|---|---|---|
| Starter, 0 Aktionen, Sieg | 5,5 | +0,35 | **5,85** |
| Einwechsler, keine Aktion | 4,0 | – | **4,0** |
| Einwechsler, 1 Tor (STU) + Sieg | 4,0 | 0,5+0,35 | **4,85** |
| Einwechsler, 2 Tore + 1 Vorlage + Sieg | 4,0 | 1,0+0,35+0,35 | **5,7** |
| Einwechsler, 3 Tore + Vorlage + Sieg + Note 2,0 | 4,0 | 1,5+0,35+0,35+0,25 | **6,45** |
| Starter, 1 Tor + Sieg | 5,5 | 0,5+0,35 | **6,35** |

---

## 3. Kategorien im Detail

### K1 – Direkter Impact (Cap 1,6)
- **Tor** positionsabhängig (wie bisher, aber gedeckelt):
  STU **+0,5** · MIT/OMI/DMI **+0,6** · ABW **+0,8** · TOR **+1,0** je Tor,
  **max. +1,0** für Tore insgesamt.
- **Vorlage** **+0,35** je Vorlage, max. **+0,7**.
- **Verwandelter Elfmeter** **+0,2** (zusätzlich zum Tor, nur falls `elfmeter`-Flag).
- K1 wird auf **1,6** gekappt (erhält den 35-%-Anteil: stark, aber nicht alleinentscheidend).

### K2 – Effizienz & Spielkontrolle (Cap 1,1) – Proxy, da Passdaten fehlen
- **Schussquote** (`aufsTor/schuesse`, nur wenn `schuesse ≥ 3`):
  ≥ 40 % +0,2 · ≥ 50 % +0,3 · ≥ 60 % +0,4 · ≥ 75 % +0,5.
- **Abschlusspräsenz:** `aufsTor ≥ 2` +0,1 · `aufsTor ≥ 4` +0,2 (Cap 0,2).
- **Auffälligkeit** (Ticker-Nennungen): 2–3 +0,1 · 4–5 +0,2 · 6+ +0,3.
- K2 wird auf **1,1** gekappt. *Hinweis:* Sobald der Bericht Passquoten/Dribblings
  liefert, kann dieser Block 1:1 ersetzt werden.

### K3 – Zweikämpfe (Cap 0,9) — NEU, konkret
**Datenquelle:** Die Spielerstatistik am Ende des Spielberichts
(`BerichtRepository.kt:306-313`, Tabelle „Spielername | **ZK** | **ZK-%** | …“).
`ZK` = gewonnene Zweikämpfe, `ZK-%` = Zweikampfquote in Prozent. Beide Werte sind bereits
geparst (`BerichtRepository.kt:342-343/355-356`), stehen im Kandidaten
(`ElfAuswertung.kt:111-112/148-149`) und im Cache – sie fließen nur noch **nicht** in die
Note ein. Es ist **keine** neue Datenerfassung nötig.

**Regeln (Feldspieler):**
- Nur anrechnen, wenn eine Statistik existiert: `zweikaempfe > 0` (sonst 0,0 – „nicht
  berichtet“ bleibt neutral).
- **Zweikampfquantität:** **+0,03 je gewonnener `ZK`**, max. **+0,4** (≈ ab 14 ZK).
- **Zweikampfquote:** ≥ 45 % +0,05 · ≥ 50 % +0,1 · ≥ 55 % +0,15 · ≥ 60 % +0,2 ·
  ≥ 65 % +0,25 · ≥ 70 % +0,3.
- K3 wird auf **0,9** gekappt.

**Torhüter:** Statt der ZK-Werte zählt der **gehaltene Bälle**-Wert (aus Schüssen aufs Tor
minus Gegentoren, `ElfAuswertung.kt:80-83`): 3–4 → +0,3 · 5–6 → +0,5 · 7+ → +0,7
(ZK-Zahlen sind für TW selten aussagekräftig).

**Realistische Beispiele (Werte wie in den Tests/Berichten):**

| Spieler | ZK | ZK-% | K3-Beitrag |
|---|---|---|---|
| Ted Struan | 2 | 50 % | 0,06 + 0,1 = **+0,16** |
| Steffen Fromm | 8 | 37,5 % | 0,24 + 0 = **+0,24** |
| Aaron | 8 | 62,5 % | 0,24 + 0,2 = **+0,44** |
| Zweikampf-Juwel | 12 | 70 % | 0,36 + 0,3 = **+0,66** (Cap noch nicht erreicht) |
| Zweikampf-Monster | 15 | 74 % | 0,4 + 0,3 = **+0,7** → Cap 0,9 erlaubt mehr Raum |

*Kalibrierung:* Die heutigen realen Werte liegen meist zwischen 5–15 `ZK` und
45–70 % – die Stufen sind so gesetzt, dass K3 typischerweise +0,2 … +0,6 liefert und
damit ~15–20 % Anteil an der Gesamtbewertung halten kann.

### K4 – Ergebnis & Teambonus (Cap 0,55)
- **Sieg** +0,35 · **Unentschieden** +0,1 · **Niederlage** −0,2.
- **Zu-Null:** TW +0,3 · ABW +0,2.
- K4 wird auf **0,55** gekappt (Sieg + TW-Zu-Null = 0,65 → 0,55).

### K5 – Subjektiver Faktor / Bericht-Note (Cap +0,35, Malus −0,15)
Lineare Umrechnung (Bericht-Note „1“ = beste Note, Skala 1,0–6,0, 0,5-Stufen):

| Bericht-Note | Beitrag |
|---|---|
| 1,0 / 1,5 | +0,35 / +0,30 |
| 2,0 / 2,5 | +0,25 / +0,20 |
| 3,0 / 3,5 | +0,15 / +0,10 |
| 4,0 / 4,5 | +0,05 / 0 |
| 5,0 / 5,5 | −0,05 / −0,10 |
| 6,0 | −0,15 |

- Kein Einfluss (0,0), wenn der Bericht keine Note ausweist.
- **Voraussetzung:** Skala/Dirnktion der geparsten Bericht-Note an echten Berichten
  verifizieren (`BerichtRepository.kt:341,354`), bevor K5 aktiviert wird.

---

## 4. Karten & Sonderregeln

- **Gelb** −0,2 (je) · **Rot-Ereignis** −1,0 – **nicht** skaliert, nicht gedeckelt.
- Kein Doppel-Abzug (Rot zählt nicht zusätzlich als Gelb).
- Eine 10,0 nur bei ungerundet ≥ 9,8; sonst 9,9 (`endnote`, `ElfBewertung.kt:296-304`).

---

## 5. Beispielrechnungen (neue Formel, komplett)

**A – Stürmer des Spieltags (Starter):** Sieg, 2 Tore, 1 Vorlage, 5 Schüsse/3 aufs Tor
(60 %), 3 Nennungen, 10 ZK/63 %, Bericht-Note 2,0.

```
K1 = 1,0 (Tore, Deckel) + 0,35 (Vorlage)                    = 1,35
K2 = 0,4 (Quote 60%) + 0,1 (aufsTor≥2) + 0,1 (3 Nennungen)   = 0,60
K3 = 0,30 (10 ZK × 0,03) + 0,2 (63%)                         = 0,50
K4 = 0,35 (Sieg)                                             = 0,35
K5 = 0,25 (Note 2,0)                                         = 0,25
Σ = 3,05  →  Note 5,5 + 3,05 = 8,6  (Anteile: 44/20/16/11/8 %)
```

**B – Innenverteidiger (Starter):** Sieg, Zu-Null, 1 Tor (Ecke), 8 ZK/70 %, 2 Nennungen,
Note 2,5.

```
K1 = 0,80 (ABW-Tor)                                          = 0,80
K2 = 0,10 (2 Nennungen)                                      = 0,10
K3 = 0,24 (8 ZK) + 0,30 (70%)                                = 0,54
K4 = 0,35 + 0,20 (Zu-Null ABW) → Cap 0,55                    = 0,55
K5 = 0,20 (Note 2,5)                                         = 0,20
Σ = 2,19  →  Note 5,5 + 2,19 = 7,7
```

**C – Star-Einwechsler (25 min):** Sieg, 2 Tore, 1 Vorlage, 10 ZK/60 %, 2 Nennungen,
Note 2,0. **Basis 4,0, Zuschläge unskaliert.**

```
K1 = 1,0 (Tore) + 0,35 (Vorlage)                             = 1,35
K2 = 0,4 (60%) + 0,1 (aufsTor≥2) + 0,1 (2 Nennungen)          = 0,60
K3 = 0,30 (10 ZK) + 0,2 (60%)                                 = 0,50
K4 = 0,35 (Sieg)                                              = 0,35
K5 = 0,25 (Note 2,0)                                          = 0,25
Σ = 3,05  →  Note 4,0 + 3,05 = 7,05
```

**D – Joker ohne Einfluss (15 min):** keine Aktionen, keine Statistik, Verlierer?

```
Basis 4,0; K4 −0,2 (Niederlage) →  Note 3,8
```

**E – Torwart (Starter):** Zu-Null, Sieg, 4 gehaltene Bälle, Note 2,0.

```
K3 (TW) = 0,30 (4 gehaltene)                                 = 0,30
K4 = 0,35 + 0,3 (TW Zu-Null) → Cap 0,55                      = 0,55
K5 = 0,25 (Note 2,0)                                         = 0,25
Σ = 1,10  →  Note 5,5 + 1,10 = 6,6
```

---

## 6. Aufschlüsselung im Spieler-Detail („⭐ Spieltagsbewertung“)

Der Detail-Dialog (`ElfDesSpieltagsScreen.kt:1314-1410`) zeigt die Bewertung transparent
zeilenweise. Mit dem neuen Modell werden diese Werte aufgelistet – **nur nicht-null
Beiträge** (wie heute, `bewerten` fügt 0-Beiträge nicht hinzu), Reihenfolge =
Rechenreihenfolge:

```
Grundbewertung         Startelf: +5,5   ·   Einwechslung: +4,0
─────────────────────────────────────────────────────────────
Direkter Impact (K1)
  Tore                 +0,5 STU · +0,6 MIT · +0,8 ABW · +1,0 TW (max. +1,0)
  Vorlagen             +0,35 je
  Elfmeter             +0,2 (nur verwandelter Elfmeter)
Effizienz (K2, Proxy)
  Schussquote          +0,2 · +0,3 · +0,4 · +0,5 (je Stufe)
  Abschlüsse aufs Tor  +0,1 (≥2) · +0,2 (≥4)
  Auffälligkeit        +0,1 (2–3) · +0,2 (4–5) · +0,3 (6+)
Zweikämpfe (K3)
  Gewonnene Zweikämpfe +0,03 je (max. +0,4)
  Zweikampfquote       +0,05 · +0,1 · +0,15 · +0,2 · +0,25 · +0,3
  Gehaltene Bälle      +0,3 (3–4) · +0,5 (5–6) · +0,7 (7+)   [nur Torwart,
                       ersetzt dann die beiden ZK-Zeilen]
Ergebnis & Teambonus (K4)
  Ergebnis             Sieg +0,35 · Unentschieden +0,1 · Niederlage −0,2
  Zu null              +0,3 TW · +0,2 ABW
Subjektiver Faktor (K5)
  Bericht-Note         +0,35 bis −0,15 (je Note, nur falls Bericht eine Note ausweist)
Strafen (unskaliert)
  Karten               −0,2 je Gelb · −1,0 je Rot
─────────────────────────────────────────────────────────────
Gesamt                 <Note> / 10
```

**Ergänzend über der Liste (bleibt wie heute):**
- Name / Verein (klickbar), Positions-Badge, Note oben rechts.
- Einsatzzeile `einsatzText` (L1413-1417): „Einsatz: X Minuten“ · „Startelf (volle Partie)“ ·
  „eingewechselt, Dauer im Bericht nicht genannt“.

**Mit umzuschreiben:** Der Erklärtext „So wurde bewertet“
(`SO_WURDE_BEWERTET_TEXT`, L119-125) passt nicht mehr zum neuen Modell. Er sagt heute
„startet bei 5,5 … exakt sechs Kriterien … Einsatzzeit ist kein Bonus“. Neu sollte er das
4,0/5,5-Basis-Prinzip, die Kategorien (inkl. Zweikämpfe) und „10,0 nur bei überragender
Leistung“ nennen.

**Darstellungshinweis:** Beiträge werden auf eine Nachkommastelle gerundet
(`formatiereBeitrag`, L314-321). Feine ZK-Werte (z. B. +0,06) erscheinen dann als `+0,1` –
inkonsistent nur optisch, die Summe rechnet mit den ungerundeten Werten. Optional die
ZK-Skala auf 0,05er-Schritte anheben, damit jeder sichtbare Zuschlag bei 0,1 beginnt.

---

## 7. Technische Umsetzung (was sich ändert)

| Datei | Änderung |
|---|---|
| `ElfBewertung.kt` | Basis-Auswahl `if (startelf) 5,5 else 4,0`; K1–K5-Funktionen inkl. **K3-Zweikämpfe**; Caps; `kartenMalus` unskaliert |
| `ElfBewertung.kt` (Kandidat) | Für K5 neues Feld `berichtNote: Double?` (umgerechnet) – **Zweikämpfe/Schuesse sind bereits da** |
| `ElfAuswertung.kt` | `stat.note` in `ElfKandidat` übernehmen (L91/L128); Noten-Parsing verifizieren |
| `ElfDesSpieltagsRepository.kt` | Cache-Bump `v6 → v7` (L169; nötig wegen K5-Feld, Bewertung wird mitgespeichert L209) + neues Feld serialisieren |
| `ElfBewertung.kandidatVon` | Neues Feld durchreichen (L328-353) |
| UI Aufschlüsselung | Neue/umbenannte Zeilen: „Einsatzzeit (Basis 4,0/5,5)“, „Tore“, „Vorlagen“, „Zweikämpfe“, „Ergebnis“, „Zu null“, „Note“, „Karten“ |
| Tests | `bewertung_kurzerEinsatz…` (`ElfDesSpieltagsTest.kt:350-361`) ersetzen: Joker mit Tor → höher als Joker ohne Einfluss, aber unter gleichwertigem Starter; neue K3-Tests |

**Wichtig:** Die Zweikampf-Werte brauchen **keinen** zusätzlichen Datenfluss – sie werden
schon heute je Spieler geparst und im Cache (v6, Felder 18/19) abgelegt. Es fehlt nur die
Auswertung in `ElfBewertung`.

### Konfigurationskonstanten

```kotlin
// Basis
BASIS_STARTELF   = 5.5
BASIS_EINWECHSLER= 4.0
// Budgets (Summe = 4,5)
IMPACT_MAX       = 1.6
EFFIZIENZ_MAX    = 1.1
ZWEIKAMPF_MAX    = 0.9
TEAM_MAX         = 0.55
SUBJEKTIV_MAX    = 0.35
// Zweikämpfe
ZK_PRO_ZWIKAMPF  = 0.03   // +0,03 je gewonnener ZK, max 0,4
ZK_QUOTE_STUFEN  = mapOf(45 to 0.05, 50 to 0.1, 55 to 0.15, 60 to 0.2, 65 to 0.25, 70 to 0.3)
TW_GEHALTEN      = mapOf(3 to 0.3, 5 to 0.5, 7 to 0.7)
// Karten (unskaliert)
GELBE_MALUS      = 0.2
ROTE_MALUS       = 1.0
```

---

## 8. Empfohlene Umsetzungsreihenfolge

1. **Basis 4,0/5,5 + kein Minuten-Faktor** (ersetzen den alten Einsatzfaktor).
2. **K3 Zweikämpfe aktivieren** – größter fachlicher Zusatznutzen, kein neues Datenfeld nötig.
3. **K1-Kephalte** (Tore deckeln) + **K4** (Ergebnis-Anhebung).
4. **K5 Bericht-Note** nach Skalen-Verifikation.
5. **K2 Effizienz-Proxy** – optional, v. a. für Stürmer/Mittelfeld.
6. Cache v7, Tests anpassen, E2E-Referenzlauf (England Saison 24 ZAT 1) neu auswerten.

---

## 9. Offene Entscheidungen

1. **Einwechsler-Basis fest 4,0** (empfohlen) oder linear `4,0 + 1,5 × minuten/90`
   zwischen 4,0 (Einwechslungsschluss) und 5,5 (volle Partie)?
2. **ZK-Stärke kalibrieren:** Faktor 0,03 je ZK und Quote-Stufen OK? An echten Berichten
   prüfen (Bandbreite aktuell 5–15 ZK / 45–70 %).
3. **Bericht-Note-Skala** bestätigen (1–6, Dezimalpunkt/Komma) – vor K5-Freigabe.
4. Soll die 9,8-Regel für 10,0 bleiben? (Ja, empfohlen.)