# Bericht: Datenbasis & Anpassungsoptionen für die „Elf des Spieltags“

Stand: 17.09.2026 · Quelle: Quellcodeanalysen (`ElfBewertung.kt`, `ElfAuswertung.kt`,
`Bericht.kt`, `BerichtRepository.kt`, `ElfDesSpieltagsRepository.kt`) – es wurden keine
Dateien geändert.

---

## 1. Kernfrage & Fazit

**Frage:** Nutzt die Bewertung der „Elf des Spieltags“ die bestmögliche Datenbasis aus den
vorhandenen Daten des Spieltagsberichts?

**Antwort:** Nein. Die Note nutzt heute bewusst nur einen kleinen Teil der bereits
vorhandenen Daten (Tore, Vorlagen, Ergebnis, Zu-Null-Spiel, Karten). Mehrere im Bericht
**bereits geparste** Werte fließen nicht ein, obwohl sie vorliegen. Umgekehrt sind einige
der Wunsch-Faktoren der „optimalen Gewichtung“ **im Datenmodell gar nicht vorhanden**
und daher nur als Proxy oder gar nicht umsetzbar.

**Zentrale Erkenntnisse:**

1. **Minuten-Gewichtung fehlt komplett** – eine Einwechslung für 20 Minuten mit Tor erhält
   heute exakt denselben Torbonus wie ein 90-Minuten-Starter (Test
   `bewertung_kurzerEinsatzErhaeltKeinenVollspielbonus`). **Das ist der größte
   Anpassungspunkt.**
2. **Die Bericht-Note wird geparst, aber nie verwendet** (`BerichtRepository.kt:341,354`
   liest sie, `ElfKandidat` kennt kein Noten-Feld). Damit wäre der „Subjektiver Faktor
   8 %“ ohne neue Daten sofort nutzbar.
3. **Zweikämpfe/ZK-% liegen im Modell vor, fließen aber nicht in die Note**
   (`ElfKandidat.zweikaempfe`/`zweikampfQuote`, bewusst nicht gewichtet laut
   `ElfBewertung.kt:89-92`). Der „Defensiv-Block 20 %“ wäre damit grob abbildbar.
4. **Nicht vorhanden im Bericht:** Passquote, Dribblings, Tacklings/abgefangene Bälle,
   kreierte Großchancen. `BerichtSpielerStatistik` (`Bericht.kt:48-63`) kennt nur
   Note, ZK, ZK-%, Schüsse, Schüsse aufs Tor, Tore, Vorlagen. Ein volle
   „Effizienz 25 %“-Umsetzung ist ohne zusätzliche Daten/Parsererweiterung nicht möglich.
5. **„herausgeholte Elfmeter“ fehlen** – erkannt wird nur der **verwandelte** Elfmeter
   (`elfmeter`-Flag).

---

## 2. Heutige Bewertung (verifiziert)

Ort: `app/src/main/java/com/onlinesoccer/app/data/repository/ElfBewertung.kt`

Formel (`bewerten`, L176-221): **5,5 Grundbewertung + Aktionen + Ergebnis − Strafen**

| Kriterium | Wert | Quelle |
|---|---|---|
| Grundbewertung (jeder mit Einsatz, auch Einwechsler) | **+5,5** | L133 |
| Tor-Stufe | STU +1,0 · MIT/OMI +1,2 · ABW +1,5 · TW +2,0 je Tor | L136-139 |
| Vorlage | +0,6 je Assists | L142 |
| Sieg / Unentschieden / Niederlage | +0,3 / +0,1 / −0,2 | L145-147 |
| Zu-Null | TW +0,5 · ABW +0,3 | L150-151 |
| Gelb / Rot-Ereignis | −0,2 / −1,0 | L154, L164 |
| Obergrenze | 10,0 nur ab ungerundet 9,8; sonst 9,9 | L167, L296-304 |

**Einsatzzeit** (`effektiveMinuten`, L241-246): `minuten` (nur aus echten Wechsel-Hinweisen
des Tickers) > Startelf → 90 > Einwechsler mit Ereignis → 30 > sonst 0. Die Minuten sind
**kein Bonus** – sie entscheiden nur `hatEinsatz` (Filter) und schließen nie eingesetzte
Bank-Spieler aus.

**Auswahl:** `ElfAuswahl.erstelleElf` (L450-552) bildet formationsbasiert (10 Formationen,
L398-409) die Elf mit maximaler Notensumme; je Position die bestbewerteten Spieler.

---

## 3. Tatsächlich im Spieltagsbericht vorhandene Daten (je Spieler)

Aus `SpielBericht`/`BerichtSpielerStatistik`/Ticker-Ereignissen stehen pro Spieler bereit
– **bereits geparst und im `ElfKandidat`/`ElfSpieler` vorhanden**:

| Datenfeld | Status | Heute in Note? |
|---|---|---|
| Startelf vs. Bank | ✔ geparst (`Bericht.kt:22-24`) | nur Filter |
| Einsatzminuten (Wechsel-Ticker) | ✔ (`ElfAuswertung.kt:286-336`) | nur Filter |
| Tore | ✔ Ticker + Statistik-Tabelle | **ja** |
| Vorlagen | ✔ Ticker + Tabelle | **ja** |
| Ergebnis / Gegentore / Zu-Null | ✔ | **ja** |
| Karten (Gelb/Rot) | ✔ | **ja** |
| Verwandter Elfmeter | ✔ (`elfmeterRegex`, L33/267) | **nein** |
| Gewonnene Zweikämpfe (ZK) | ✔ Tabelle → `zweikaempfe` | **nein** |
| Zweikampfquote (ZK-%) | ✔ Tabelle → `zweikampfQuote` | **nein** |
| Schüsse / Schüsse aufs Tor | ✔ Tabelle | **nein** |
| Auffälligkeit (Ticker-Erwähnungen) | ✔ (`auffaelligkeit`, L343) | **nein** |
| **Bericht-Note (Kicker-Note)** | ✔ geparst (`BerichtRepository.kt:341/354`), **aber nicht ins Elf-Modell übertragen** | **nein** |
| Gehaltene Bälle (TW, abgeleitet) | ✔ (`ElfAuswertung.kt:80-83`) | **nein** |
| Kapitän | ✔ geparst (`kapitän`, L368) | **nein** |

**Nicht vorhanden (weder geparst noch im Modell):** Passquote, Dribblings, Tacklings,
abgefangene Bälle, kreierte Großchancen, herausgeholte Elfmeter. (Grep über
`app/src/main` bestätigt: keine solchen Felder/Parsing-Regeln.)

---

## 4. Abgleich mit der vorgeschlagenen „optimalen Gewichtung“

| Faktor | Ziel-Gewicht | Daten im Bericht? | Heute gewichtet? | Umsetzbarkeit |
|---|---|---|---|---|
| **Direkter Impact** (Tore, Vorlagen, herausgeholte Elfmeter) | 35 % | Tore ✔ Vorlagen ✔ · herausgeholte Elfmeter ✘ (nur verwandelte) | Tore/Vorlagen **ja** (positional hoch) | Tore/Vorlagen ✔ · verwandelter Elfmeter als kleiner Zusatz machbar |
| **Effizienz & Spielkontrolle** (Passquote, Großchancen, Dribblings) | 25 % | ✘ alles drei | **nein** | **Nicht wie vorgeschlagen** machbar. Proxys möglich: Schussquote (`aufsTor/schuesse`), Auffälligkeit, Ticker-Akzente |
| **Defensiv & Zweikampf** (gewonnene ZK, abgefangene Bälle, Tacklings) | 20 % | ZK ✔ ZK-% ✔ · abgefangene Bälle/Tacklings ✘ · gehaltene Bälle (TW) ✔ als Proxy | **nein** | Grob machbar über `zweikaempfe` + `zweikampfQuote` (+ `gehalteneBalle` für TW) |
| **Ergebnis & Teambonus** (Sieg, Zu-Null) | 12 % | ✔ vollständig | **ja** (+0,3/+0,1/−0,2, Zu-Null) | Wird heute bereits abgedeckt; relative Anteil eher < 12 % |
| **Subjektiver Faktor** (Spielernote/Voting) | 8 % | Bericht-Note ✔ · Fan-Voting ✘ | **nein** | **Bericht-Note sofort einbindbar** (Feld + Cache-Version) |

**Wichtiger Hinweis zur Prozent-Logik:** Die aktuelle Formel ist **additiv**
(5,5 + Beiträge), die vorgeschlagenen Prozentwerte sind eine **relative
Kategorie-Gewichtung** (wie ein Scoring-Budget). Beides lässt sich verbinden, aber nicht
1:1 übertragen. Praktisch: pro Kategorie ein **Beitragskontingent** definieren (z. B.
Impact max. ±1,5 Punkte, Effizienz ±0,8, Defensiv ±0,7, Team ±0,3, Subjektiv ±0,3), statt
echter Prozentrechnung.

---

## 5. Minuten-Gewichtung für Einwechslungen (Kernanliegen)

**Ist-Zustand:** Ein Tor in der 20. Minute (Einwechslung 70.-90.) und ein Tor bei 90
Minuten ergeben **identische** Noten. Das ist in Tests ausdrücklich so festgeschrieben
(`bewertung_kurzerEinsatzErhaeltKeinenVollspielbonus`, `ElfDesSpieltagsTest.kt:350-361`).

**Ziel laut Auftrag:** Startelf-Basis 5,5 beibehalten; Einwechslungen „nach Minuten
entsprechend weniger“.

### Variante A – Delta-Skalierung (empfohlen)
Die 5,5-Basis bleibt für alle eingesetzten Spieler der Anker; nur der **Leistungsanteil
über der Basis** wird mit dem Einsatzfaktor skaliert:

```
einsatzFaktor = minuten / 90   (geklemmt auf ein Minimum, z. B. 0,35)
Endnote      = 5,5 + (Rohwert − 5,5) × einsatzFaktor
```

Beispiele (Stürmer, Tor +1,0):
- Starter, 90 min, 1 Tor → 5,5 + 1,0 = **6,5** (unverändert)
- Einwechsler, 25 min, 1 Tor → 5,5 + 1,0×0,28 = **5,78**
- Einwechsler, 25 min, ohne Ereignis → **5,5** (kein Delta)
- Einwechsler mit Gelb-Rot, 25 min → 5,5 + (−1,2)×0,28 = **5,16**

Effekte: Startelf-Basis bleibt, Joker mit Tor zählt noch, aber deutlich unter einem
Starter mit derselben Aktion. Für Einwechsler **ohne bekannte Minuten** (aktuelle
Neutralannahme 30 min, `effektiveMinuten` L244) ergibt sich dann Faktor 30/90 = 0,33 –
eine bewusste, dokumentierte Verhaltensänderung gegenüber heute (voller Bonus).

### Variante B – volle Proportionalität
`Endnote = Rohwert × minuten/90` (Basis **und** Aktionen skaliert).
Sehr hart: ein 20-min-Joker landet bei ~1,5 und praktisch nie in der Elf – das würde den
„Einwechsel-Erzählstrang“ (Joker-Tor) vollständig eliminieren. Nur wählen, wenn
ausdrücklich „Spielzeit = Wert“ gewollt ist.

### Empfehlung
**Variante A** mit einem floor von ca. **0,35–0,5**. Sie erfüllt die Anforderung
(„Einwechslung nach Minuten weniger“) und erhält die 5,5-Startbasis als Anker.

---

## 6. Konkrete Anpassungsliste (priorisiert)

### P0 – schnell, klarer Mehrwert
1. **Minuten-Faktor einbauen** (Variante A):
   `ElfBewertung.bewerten`/`endnote` (L176-221, L296-304) um `einsatzFaktor` erweitern;
   Quelle `effektiveMinuten` (L241-246). Amortisiert sich sofort bei jeder Elf.
2. **Bericht-Note (subjektiver Faktor 8 %) einbinden**:
   - `BerichtSpielerStatistik.note` (bereits geparst, `BerichtRepository.kt:341/354`)
     in `ElfKandidat` (neu) + `ElfAuswertung.kandidatenAusBericht` (L91/128) übertragen.
   - Mapping Note → Beitrag in `ElfBewertung` (Noten auf 1-10-Skala; Vorsicht bei
     Komma-/Punkt-Parsing und ggf. umgekehrter Skala je nach Berichtssystem).
3. **Defensiv-Block (20 %) grob abbilden**:
   `zweikaempfe`/`zweikampfQuote` über kantenloses, moderates Kontingent gewichten
   (z. B. nur ab Mindestzweikampfzahl, dann ZK-% zwischen 40–70 % → 0 … +0,5);
   für TW `gehalteneBalle` analog nutzen.

### P1 – sinnvoll, aber nicht kritisch
4. **Effizienz-Proxy** einbinden: `aufsTor/schuesse` (Schussquote) mit kleinem Budget;
   optional `auffaelligkeit` als Kontextsignal. Explizit als Proxy kennzeichnen, da
   echte Passdaten fehlen.
5. **Verwandelter Elfmeter** erhält einen kleinen Zusatz innerhalb „Direkter Impact“
   (das `elfmeter`-Flag existiert bereits, Bonus fehlt).
6. **Konsistenzprüfung Ticker ↔ Statistik-Tabelle**: Tore/Vorlagen liegen doppelt vor
   (Ticker + `BerichtSpielerStatistik`); bei Abweichung minor behandeln oder logging.

### P2 – nur mit neuen Daten
7. **Passquote, Dribblings, Tacklings, Großchancen** – allesamt nicht im Spieltagsbericht.
   Umsetzbar erst nach Erweiterung des Parsers/der Berichtsdaten (oder eines eigenen
   Source-Trackings). Bis dahin sind die 25 % nur über die Proxys aus P1 abdeckbar.
8. **Fan-Voting** – benötigt eine externe Quelle; aktuell nicht abbildbar.

### Dabei nicht vergessen
- **Cache-Format**: Bewertung wird im Cache gespeichert
  (`ElfDesSpieltagsRepository.kt:209` `bewertung`, Präfix `ElfDesSpieltags|v6`, L169,
  25 Felder/Spieler, L171). Jede Formel- oder Modelländerung macht **Cache-Bump
  v6 → v7** nötig (alt-Versionen werden sonst mit alten Noten angezeigt/verworfen).
- **UI-Aufschlüsselung**: `ElfBewertung.kandidatVon` (L328-353) und die
  ＂So wurde bewertet“-Zeilen (`SpielerBewertung.zeilen`) müssen um neue Zeilen
  (z. B. „Einsatzzeit“, „Zweikämpfe“, „Note“) erweitert werden, sonst fehlen sie im
  Detail-Dialog.
- **Tests anpassen**: `ElfDesSpieltagsTest.kt` – insbesondere
  `bewertung_kurzerEinsatzErhaeltKeinenVollspielbonus` (L350-361) ist betroffen und muss
  die neue Minuten-Semantik abbilden; danach Real-Lauf über den E2E-Test
  (`ElfDesSpieltagsE2eTest.kt`, England Saison 24 ZAT 1) mit dokumentierten Noten.

---

## 7. Warum die vorgeschlagenen 35/25/20/12/8 nicht 1:1 sind

- Die heutige Formel gibt dem **Direkten Impact implizit den Löwenanteil** (ein Tor kann
  bis zu +2,0 bei nur ~6,5 erreichbaren Punkten ausmachen), abgefedert durch Position.
- **Team-Bonus ist aktuell schwach** (+0,3 Sieg, +0,3 Zu-Null) – ein echter 12%-Anteil
  wäre erst nach Kalibrierung realistisch.
- **Subjektiven Anteil** gibt es noch gar nicht (0 %), genau wie Effizienz (0 %) und
  Defensiv (0 %).

Eine zielführende Neueichung wäre daher:
- Impact-Kontingent **senken** (z. B. Tore leicht deckeln oder Zusatzpunkte
  quantitativer staffeln),
- Effizienz + Defensiv + Subjektiv **ergänzen**,
- Team-Bonus leicht **anheben** (z. B. Sieg +0,4/+0,5, Unentschieden halten),
- Minuten-Faktor **global anwenden** (P0 Nr. 1).

Das Ergebnis wäre eine Note, die näher an der gewünschten Kategorienverteilung liegt,
transparent bleibt (Aufschlüsselungs-Zeilen) und Einwechsler fairer behandelt.