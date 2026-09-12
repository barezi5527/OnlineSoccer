# Konzept: Team, Nationale Bewerbe, Zugabgabe/ZAT in der OS-App

Stand: 05.09.2026 · Entwurf zur Freigabe – noch **keine** Umsetzung.

---

## 1. Ausgangslage & Ziel

Die drei wichtigsten Bereiche der Website sind (lt. Vorgabe):

1. **Team** (+ Submenüs, v. a. Mannschaft mit Spieler-Details)
2. **Nationale Bewerbe** (Liga, Pokal)
3. **Zugabgabe / ZAT**

Ziel: Diese Menüs samt Untermenüs sinnvoll in die Compose-App überführen, bestehende
Fähigkeiten (Kaderliste, Aufstellung, Taktik, Vorbereitung, Spiele/Live) einordnen und
durch die fehlenden Spieler-Details ergänzen (Position, Vertrag, Stärken, Moral,
Fitness).

---

## 2. Was die Website bietet

### 2.1 Menü „Team" (Navigationsbaum aus `Analyse_OnlineSoccer.md`)

```
Team
├── Mannschaft                → showteam.php (Tabs s=0..10)  ★ Kern
├── Jugendteam                → ju.php
├── Jugendscouting            → juscout.php
├── Stadionausbau             → osneu/stadion
├── Taktik-Editor             → taktiken.php (bereits umgesetzt)
├── Team-Editor               → te.php (Rückennummern + Wappen)
├── Trainer                   → trainer.php
├── Training                  → training.php
├── Verträge verlängern       → vt.php
├── Kontoauszug               → ka.php
├── Steuerübersicht           → steuer.php
└── Werbeverträge             → wiki (Finanzen)
```

`showteam.php`-Tabs (die „Kopfzeile" von Mannschaft):
`s=0` Kader · `s=1` Vertragsdaten · `s=2` Einzelwerte (18 Skills) ·
`s=3` Statistik Saison · `s=4` Statistik Gesamt · `s=5` Teaminfo (Stadion) ·
`s=6` Saisonplan · `s=7..10` Historien (Verein/Transfer/Leihe/Saison)

### 2.2 Menü „Nationale Bewerbe"

```
├── Ligaspieltage             → ls.php   (Spieltag, Ergebnis/Vorschau/Bericht)
├── Ligatabelle               → lt.php   (8+ Varianten, Kreuztabelle)
└── Landespokale              → lp.php   (Runden)
```

### 2.3 Menü „Zugabgabe / ZAT"

```
├── Zugabgabe (klassisch)     → zugabgabe.php   (p=0 Aufstellung, p=1 Aktionen, p=2 Einstellungen)
├── Zugabgabe Beta            → zugabgabe_beta.php  (JSON-Gitter)
├── Zugabgabe Zusatz          → zuzu.php
├── ZAT-Ergebnisse            → zer.php   (alle Ligen eines Landes)
└── ZAT-Report                → zar.php   (eigene Mannschaft)
```

### 2.4 Spieler-Detail (Team → Mannschaft)

- Position im Kader als CSS-Klasse **`TOR / ABW / DMI / MIT / OMI / STU`** (in der App
  schon als `SpielerPosition`-Enum vorhanden, inkl. Positionsfarben).
- Vertragsdaten (`s=1`, showteam): Gehalt, Vertragslaufzeit, Marktwert, Geburtstag.
- Stärken (`s=2`): 18 Einzelwerte. Kaderzeile (`s=0`) liefert bereits Skillschnitt +
  Opti-Skill.
- Moral & Fitness (`s=0`): `mor`, `fit` – in der App bereits als Felder vorhanden.
- Spielerprofil/Sperre/Verleih: `sp.php?s=<spielerid>` (öffentliches Popup, ergänzt
  um `/osneu`-Felder), ID-Nummern sind **saisongebunden, nie hart-kodieren**.

---

## 3. Ist-Zustand der App

Bottom-Navigation: `Dashboard | Mannschaft | Zugabgabe | Spiele | Vorbereitung`
(5 Tabs, `ui/AppRoot.kt`).

| Tab | Inhalt | passt zu Website |
|---|---|---|
| Dashboard | Kontostand, Team, nächste Spiele, Live-Einstieg | Managerbüro |
| Mannschaft | Kader `s=0` (Position, Skill, Opti, Fit, Mor), Filter | Team → Mannschaft (Teilmenge) |
| Zugabgabe | Aufstellung + Taktik (Modus-Toggle) | Zugabgabe/Beta + Taktik |
| Spiele | nächstes/letztes Spiel, Matchcenter (Livegame-JSON) | + Livegame, ZAT-Report (teilw.) |
| Vorbereitung | Formations-Vorschlag aus Kader | (kein Website-Pendant, eigener Mehrwert) |

Fehlend gegenüber Website: Verträge, Stärken, Statistik, Teaminfo, Historien,
Spielerprofil, Liga (Tabelle/Spieltage), Pokal, ZAT-Ergebnisse/-Report,
Aktionen/Einstellungen der Zugabgabe (`zuzu`).

---

## 4. Ziel-Informationarchitektur (Empfehlung)

### 4.1 Bottom-Navigation: 4 Tabs, die den Website-Dreiklang spiegeln

```
[Dashboard]  [Team]  [Zugabgabe/ZAT]  [Bewerbe]
```

- **Team** ← ersetzt „Mannschaft" (wird der Team-Hub).
- **Zugabgabe/ZAT** ← aus „Zugabgabe" + „Vorbereitung" (Vorschlag wird ein Schritt
  im Hub) + ZAT-Report/-Ergebnisse.
- **Bewerbe** ← übernimmt Liga/Pokal; die bisherige „Spiele"-Funktion (nächstes/
  letztes Spiel, Livegame) wandert als **Live-Karte aufs Dashboard** (Spielkarte +
  „Matchcenter") – dort ist der Kontext „mein nächstes Spiel" am natürlichen Platz.
- **Vorbereitung** verschwindet als eigener Tab (wird Schritt 0 im ZAT-Hub, gleiche
  Logik unverändert).

Begründung: 1:1-Zuordnung zu den drei wichtigen Menüs, weniger Tab-Rauschen,
Vorschlag & Aufstellung & ZAT hängen fachlich zusammen (Website tut sie auch unter
einem Menü).

> **Alternativen (Entscheidung offen, s. §8):**
> A) 5. Tab „Spiele" behalten (Dashboard bleibt schlank) –
>    Nachteil: Tab-Überladung zur Website-Struktur.
> B) „Team" nicht als Tab, sondern als Hub nur auf Dashboard –
>    verfehlt die Priorität des Menüs.

### 4.2 Hub-Muster (Sub-Navigation, spielt den Website-„Header" nach)

Jeder Hub hat oben eine **horizontale Sub-Tab-Leiste** (Compose `ScrollableTabRow`
bzw. LazyRow-Chips) mit den wichtigen Unterseiten + ein **⋮ „Mehr"-Menü**
(TopBar `DropdownMenu`/Bottom-Sheet) für den Langschwanz (Jugend, Stadion, Trainer,
Kontoauszug, Steuer, Team-Editor …).

- Sub-Tabs je Hub (Start-Tab = am häufigsten genutzt):
  - **Team:** `Kader | Verträge | Stärken | Statistik | Teaminfo` + ⋮ (Jugend, Training,
    Trainer, Stadion, Team-Editor, Kontoauszug, Steuer, Verträge verlängern)
  - **Zugabgabe/ZAT:** `Vorschlag | Aufstellung | Taktik | Aktionen | Einstellungen` +
    ⋮ (ZAT-Report, ZAT-Ergebnisse)
  - **Bewerbe:** `Ligatabelle | Spieltage | Pokal` (+ optional später: Internationale
    Bewerbe)
- Umsetzung: ein eigener NavController-Level pro Tab (im Plan §7 bereits vorgesehen);
  Sub-Tab-Wechsel im Fliege „innerhalb des Tabs", State via `rememberSaveable`.

### 4.3 Zuordnung Website-Menü → App (komplette Liste)

| Website | App |
|---|---|
| Team → Mannschaft (Kader) | Team-Hub → Kader |
| Team → Mannschaft (Verträge) | Team-Hub → Verträge |
| Team → Mannschaft (Stärken) | Team-Hub → Stärken |
| Team → Mannschaft (Statistik, Teaminfo, Historien) | Team-Hub → Statistik/Teaminfo/⋯ |
| Team → Jugendteam/Scouting | Team ⋮ |
| Team → Training/Trainer | Team ⋮ (erst Lesen, Schreiben später) |
| Team → Stadionausbau | Team ⋮ |
| Team → Kontoauszug/Steuer | Team ⋮ |
| Taktik-Editor | ZAT-Hub → Taktik (bestehend) |
| Zugabgabe(-Beta) | ZAT-Hub → Aufstellung (bestehend) |
| Zugabgabe Zusatz | ZAT-Hub → Aktionen/Einstellungen |
| ZAT-Report | ZAT-Hub ⋮ bzw. Dashboard-Karte |
| ZAT-Ergebnisse | ZAT-Hub ⋮ |
| Ligaspieltage | Bewerbe → Spieltage |
| Ligatabelle | Bewerbe → Ligatabelle |
| Landespokale | Bewerbe → Pokal |
| Internationale Bewerbe | Bewerbe ⋮ (später, nur falls gewünscht) |
| Managerbüro | Dashboard (bleibt) |

---

## 5. Detailkonzept je Hub

### 5.1 Team-Hub

- Kader = heutiger Mannschaft-Tab (Filter/Sortierung/Positionschips) unverändert +
  **Tap auf Spieler → Spielerprofil** (Neu, Kernfeature, §5.2).
- Verträge (`s=1`): Tabelle Gehalt · Vertragslaufzeit · Marktwert · Geburtstag,
  sortierbar, auslaufende Verträge hervorgehoben (Farblogik), Zeile → Spielerprofil.
- Stärken (`s=2`): je Spieler die 18 Einzelwerte; als **Tabellenansicht** (Spieler ×
  Skill) wie auf der Website UND als **Balken-Detail** im Spielerprofil.
- Statistik (`s=3/4`): Spiele/Tore/Vorlagen/Score/Gelb/Rot je Spieler, Umschalter
  „Saison/Gesamt".
- Teaminfo (`s=5`): Stadiongröße, Sitz-/Stehplätze, überdacht, Rasenheizung,
  Anzeigetafel, MW-/Gehaltssumme.
- ⋮: Jugendteam (Leseliste `ju.php`), Training (Leseliste), Trainer, Stadionausbau,
  Kontoauszug, Steuer, Team-Editor (Rückennummern), Verträge verlängern.
  **Schreibaktionen erst nach Freigabe** (s. Sicherheitsregeln, PLAN §11).

### 5.2 Spielerprofil (Neu – Kern der Spieler-Details)

Datenquellen: `sp.php?s=<pid>` (Profil-Schnellwerte) + `showteam.php` `s=1` (Vertrag)
+ `s=2` (Stärken) + `s=4` (Statistik) – ein Repository `TeamRepository` liefert einen
zusammengeführten `SpielerProfil`-Datensatz.

Screen:
1. **Kopf:** Rückennummer, Name, Positions-Badge (farbig, TOR…STU), Alter, Land/Jugend-,
   ggf. Sonderstatus (Sperre, Verleih, K/G) – farbige Badges wie auf der Website.
2. **Zustand:** Moral (Mor) + Fitness (Fit) als Ziffernbalken + Form-Trend.
3. **Stärken:** die 18 Einzelwerte als kompakte Skalen (9er-Balken), Skillschnitt
   + Opti-Skill prominent, Highlight der besten 3 Werte.
4. **Vertrag:** Gehalt, Laufzeit (bis ZAT/Saison), Marktwert, Geburtstag.
5. **Statistik (Saison/Gesamt):** Spiele, Tore, Vorlagen, Score, Gelb/Rot.
6. **Kontext-Aktionen** (nur sofort machbare, Rest ausgegraut/⋮):
   - In Aufstellung einsetzen (Navigationspfad zum ZAT-Hub, Spieler vorbelegt),
   - Vertrag verlängern (Schnelleinstieg `vt.php`),
   - Training ansehen (lesend).

### 5.3 Zugabgabe/ZAT-Hub

- `Vorschlag` = bisheriger Vorbereitung-Tab (Formation wählen → Vorschlag) +
  Button „In Aufstellung übernehmen".
- `Aufstellung` = heutiger Zugabgabe-Tab (Feld-/Bank-Ansicht, Tauschen) **plus native
  Formation-/Taktikauswahl** wie auf der Webseite: Dropdown aus `select[name=raster1]`
  (eigene Taktiken + Standard 4-4-2 … 3-4-3, geparst in `Aufstellung.taktiken`) mit
  „Laden"-Button → `wendeTaktikAn(id)` (GET `zugabgabe.php?p=0&raster1=<id>&raster=Laden`
  + Nachladen) und Bestätigungsdialog. `Taktik` = Taktik-Editor (`taktiken.php`).
- `Aktionen` (`zuzu.php` p=1) + `Einstellungen` (`p=2`): vorerst **lesend** anzeigen;
  Speichern-Flow existiert bereits (§8 PLAN) und wird nur gegen reale Daten getestet
  (Demo-Konto).
- ⋮: `ZAT-Report` (`zar.php`, eigener Bericht einer vergangenen ZAT), `ZAT-Ergebnisse`
  (`zer.php`, Ergebnisse der Ligen des Landes, eigener Verein markiert).

### 5.4 Bewerbe-Hub

- **Ligatabelle** (`lt.php`): Standardtabelle, eigener Verein hervorgehoben, Spalten
  wie Website; später Varianten + Kreuztabelle (Sekundärziel).
- **Spieltage** (`ls.php`): Spieltag-Auswahl (Dropdown/Spinner), Liste der Spiele mit
  Ergebnis bzw. Vorschau/Bericht-Link; Bericht → eigener Bericht-View (HTML-Parsing,
  PLAN §15.2) oder WebView-Fallback.
- **Pokal** (`lp.php`): Rundenliste/-baum des Landespokals.
- (später) Internationale Bewerbe unter ⋮ – nur falls explizit gewünscht.

---

## 6. Technische Umsetzung

- **Endpunkte** (alle schon in Analyse dokumentiert): `showteam.php?s=0..10`,
  `sp.php?s=<pid>`, `zugabgabe.php?p=0|1|2`, `zuzu.php`, `zar.php`, `zer.php`,
  `ls.php`, `lt.php`, `lp.php`.
- **Parsing-Klassen (A/B):** alle Seiten sind klassische HTML-Tabellen/Formulare →
  vorhandenes Jsoup-Repository-Muster wiederverwenden (wie `TeamRepository`,
  `ZugabgabeRepository`).
- **Modelle:** `KaderSpieler` erweitern (Vertragsfelder, Stärken-Map, Statistik) bzw.
  `SpielerProfil` als aggregierendes Modell neu; Positions-Enum wiederverwenden.
- **Navigation:** ein NavHost pro Tab (Sub-Routen), aktiver Tab behält seinen State
  (BackStack im `NavHostController` je Tab). Sub-Tab-Leiste = gemeinsames Composable
  `HubTabs()` + ⋮-Menü, wiederverwendet für alle drei Hubs.
- **Bewertung:** alles **A/nativ** möglich; kein WebView nötig (bis auf optional
  Spielberichte als Fallback). Positiv: eine gemeinsame Tab-/Hub-Komponente reduziert
  UI-Zusatzaufwand.

---

## 7. Roadmap (Reihenfolge nach Nutzwert)

| Phase | Inhalt | Aufwand (grob) |
|---|---|---|
| **A – Team-Hub + Spielerprofil** | Tab-Umbau; Substabs Kader/Verträge/Stärken/Statistik/Teaminfo; Spielerprofil (Werte/Vertrag/Statistik/Zustand); ⋮-Kurzliste | Hoch (Kernfeature, ~2 Arbeitspakete) |
| **B – ZAT-Hub** | Vorschlag reinziehen; Tabs Aufstellung/Taktik/Aktionen/Einstellungen; ZAT-Report/-Ergebnisse (lesend); Speichern gegen Demo-Konto | Mittel |
| **C – Bewerbe-Hub** | Ligatabelle, Spieltage, Pokal; Spielbericht-View; eigener Verein markiert | Mittel |
| **D – optional** | Internationale Bewerbe, Schreibaktionen (Training, Verträge, Stadion), Spieler-Screens für Jugend/Leihe | Später, nur nach Bedarf |

---

## 8. Offene Entscheidungen (zur Freigabe)

1. **Tab-Layout:** 4 Tabs `Dashboard | Team | Zugabgabe/ZAT | Bewerbe` (Empfehlung)
   oder 5. Tab „Spiele" behalten?
2. **Spielerprofil-Umfang:** sofort mit Vertrag+Stärken+Statistik (Empfehlung) oder
   erst nur Zustand+Stärken?
3. **Schreibaktionen:** bleiben vorerst hinter „Nur-Lesen" (Empfehlung); konkrete
   Schreibtests erst nach Freigabe gegen das Demo-Konto.
4. **Reihenfolge:** Phase A → B → C ok? (or A → C → B)
5. **Demoseiten-Vorgaben:** Mit actual aus account verifizierte Tabellenstrukturen
   sind in Analyse festgehalten; neue Parser werden wie bisher per Logcat-Dump
   gegen den Live-Server verifiziert (Empfehlung).

---

## 9. Bewertung

- Die Website-Menüs sind strukturell einfach (HTML-Tabellen + wenige Formulare) →
  **vollständig nativ abbildbar**, Konsistenz zur bestehenden App-Basis (Jsoup-Repos,
  Compose, Positions-Enum) ist gegeben.
- Großteils vorhandener Code (Kader, Aufstellung, Taktik, Vorschlag, Live) bleibt
  erhalten; neu ist v. a. die **Hub-Struktur** und das **Spielerprofil**.
- Kein nennenswertes technisches Risiko; der größte Aufwand ist die Verdichtung der
  drei Menüs in eine handliche Mobile-IA, ohne die Website unnötig 1:1 zu kopieren.