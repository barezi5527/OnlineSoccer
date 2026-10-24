# Bericht: Vereinswappen in der App

Stand: 24.10.2026 · inoffizieller Online-Soccer-Client

## Fragestellung

Online Soccer ist grafisch kein Wunder. Die Vereinswappen sind deshalb ein
starkes Erkennungsmerkmal. Zu klären war:

1. Wo zeigt die Website `os.ongapo.com` überhaupt Wappen?
2. Wo lohnt sich das Wappen in der Android-App trotz begrenztem Platz?
3. Was ist technisch schon vorhanden, was fehlt?

## Methodik

- Auswertung aller HTML-Server-Dumps unter `app/src/test/resources/dumps/`
  (34 Dateien mit Wappen-Vorkommen, insgesamt ~176 `images/wappen`-Treffer).
- Abgleich mit `Analyse_OnlineSoccer.md`, `Analyse_OnlineSoccer_Phase2.md`,
  `PLAN_Teamwechsel.md`, `Dokumentation_ElfDesSpieltags.md`.
- Code-Prüfung der Screens, Modelle und Repositories.
- Drei unabhängige Agents: Website-Vorkommen, App-Screens, kritischer Review.
  Ein gemeldeter Widerspruch (Gegner-ID) wurde am Code aufgelöst: `gegnerId`
  existiert und wird bereits geparst
  (`data/model/DashboardData.kt:40`, `data/repository/DashboardRepository.kt:159-174`).

## 1. Wo die Website Wappen zeigt

Belegt durch die Server-Dumps. Das Muster ist einheitlich:
`images/wappen/{8-stellige Team-ID}.{png|gif}` (auch `/images/wappen/...`),
`<img alt="Wappen <Team>">`, meist 20×20 in Tabellen, 75×75 im Kopf.

| Website-Seite | Wappen | Beleg (Dump) | Kontext |
|---|---|---|---|
| `haupt.php` (Managerbüro) | 2×, **nur eigenes Team** | `haupt_team1_aktiv.html`, `haupt_team2_aktiv.html`, `haupt_ohne_zweitteam.html` | Kopf links/rechts; **kein Gegnerwappen** |
| `lt.php` (Ligatabelle) | 1 pro Team (18) | `lt.html` | 20×20 in der Teamspalte |
| `ls.php` (Ligaspieltag) | 1 pro Team (18) | `ls1.html`, `ls2.html` | 20×20 je Paarung, alt enthält Tabellenplatz |
| `lp.php` (Landespokal) | 1 pro Team (52) | `lp2.html` | je Paarung Heim/Gast |
| `oscq.php` (OSC-Quali) | 1 pro Team | `oscq*.html` | Paarungen |
| `oscgp.php` (OSC-Gruppenphase) | 1 pro Team | `oscgp23.html` | Paarungen |
| `oscfr.php` (OSC-Finalrunde) | 1 pro Team | `oscfr23.html` | Turnierbaum |
| `st.php` / `showteam` / `kader` / `ju` | 2× angezeigtes Team | `st_3449.html`, `st902.html`, `kader.html`, `showteam_s*.html`, `ju*.html` | Teamkopf, 75×75 |

**Keine Wappen** (in den Dumps geprüft) zeigen: Spielbericht
(`bericht_real.html`), Transfermarkt/Transferlisten, Managerliste, freie Teams,
Topscorer/Topspieler/Teamstatistik, Letzte Aktionen, Taktik/ZAT. Dort erscheinen
stattdessen Länderflaggen (`images/flaggen/...`) oder gar keine Bilder.

**Wichtig für die Bewertung:** Die Website zeigt Wappen also nicht „überall, wo
ein Teamname steht", sondern gezielt dort, wo ein Verein prominent oder
unterscheidbar dargestellt wird. Genau dieses Muster sollten wir übernehmen.

## 2. Was die App heute schon kann

| Ort | Status | Technik |
|---|---|---|
| Dashboard, eigenes Team | **umgesetzt** | `DashboardScreen.kt:230`, 72 dp; lädt `teamLogoUrl` per `URL.openStream()` |
| Elf des Spieltags | **umgesetzt** | `ElfDesSpieltagsScreen.kt:786-895`; `OsApi.wappenUrls(teamId)`, png/gif/jpg-Fallback, `ConcurrentHashMap`-Cache, User-Agent |
| Alle anderen Screens | keine Wappen | `teamId`/`heimId`/`gastId` sind meist vorhanden, werden aber nicht als Bild genutzt |

Zentrale Hilfe: `OsApi.wappenUrls(teamId)` (`core/network/OsApi.kt:31-34`)
erzeugt die Kandidaten-URLs. Es gibt **keine** Bildbibliothek (kein Coil/Glide),
sondern `BitmapFactory`. Zwei getrennte Ladelogiken (Dashboard und Elf des
Spieltags) sind ein technisches Risiko.

## 3. Empfehlung: wo Wappen in die App gehören

Bewertung nach Nutzen, verfügbarem Platz und Aufwand.

### Hoch – klarer Mehrwert, Platz vorhanden

| Screen | Umsetzung | Begründung |
|---|---|---|
| **Team-/Vereinseite** (`VereinScreen.kt:95-129`) | Wappen 56–64 dp in den vorhandenen Header, neben Vereinsname | `teamId` kommt aus der Route; Website zeigt es genauso; viel Platz; Identifikation fremder Vereine |
| **Internationale Wettbewerbe** (`InternationaleScreen.kt`) | 16–20 dp je Team in Paarungen, Gruppen und Turnierbaum | Website zeigt Wappen in OSC-Quali/Gruppe/Finalrunde; `heimTeamId`/`gastTeamId`/`teamId` vorhanden; hoher Wiedererkennungswert |
| **Elf des Spieltags** | bereits umgesetzt – beibehalten | Funktioniert und nutzt bereits den robusten Loader |

### Mittel – sinnvoll, aber Layout-Anpassung nötig

| Screen | Umsetzung | Begründung / Grenze |
|---|---|---|
| **Ligatabelle** (`BewerbeScreen.kt:457-458`, `TabellenRaster.kt`) | 18–20 dp vor dem Teamnamen; Teamspalte ist 160 dp breit | Website zeigt 20×20; `zeilenTeamIds` vorhanden. Auf kleinen Displays Truncation testen, ggf. Icon 16 dp und Spaltengewicht leicht erhöhen |
| **Ligaspieltage + Landespokal** (`BewerbeScreen.kt`, `PokalSpiel`) | 16–18 dp je Paarung | Website zeigt Wappen; `heimId`/`gastId` vorhanden. Zeilen sind sehr eng (Name `weight(1f)`, Ergebnis mittig) – nur nach Layout-Test |
| **Dashboard, Gegner im Match** (`MatchCard`) | optional 20 dp | `gegnerId` ist vorhanden. Aber die Karte ist textdicht; nur wenn der Gegnername dadurch nicht leidet |

### Niedrig – bewusst nicht umsetzen

| Bereich | Warum nicht |
|---|---|
| Spielbericht | Website zeigt dort keine Wappen; Fokus liegt auf Verlauf/Aufstellung |
| Transfermarkt, Transferlisten, Letzte Aktionen | sehr dichte Karten; Website zeigt keine Wappen |
| Managerliste, freie Teams, Zweitteams | Website zeigt keine Wappen; Listen bleiben kompakt |
| Topscorer, Topspieler, Teamstatistiken | Website nutzt dort Länderflaggen, keine Wappen |
| Taktik/ZAT, Kaderzeilen | kein Platz, kein Mehrwert; das Wappen gehört in den Kopf, nicht in jede Spielerzeile |
| Spielerprofile | Nationalität ist die relevante Bildinformation, nicht der Verein |

## 4. Technische Umsetzung

1. **Gemeinsamen Wappen-Loader bauen** (Alternative zu zwei Insellösungen):
   - Ort: `core/ui/` oder `core/network/`, z. B. `TeamWappen.kt`.
   - `ConcurrentHashMap<Long, ImageBitmap>` als In-Memory-Cache.
   - `HttpURLConnection` mit `OsApi.USER_AGENT`, Timeouts und
     png/gif/jpg-Fallback über `OsApi.wappenUrls`.
   - `inSampleSize`/Zielgröße, damit 18 Tabellenbilder nicht unnötig Speicher
     belegen.
   - Platzhalter mit Initialen, wenn kein Wappen existiert.
2. **Dashboard migrieren** von `URL.openStream()` auf den gemeinsamen Loader
   (User-Agent, Timeout, Fallback, Cache).
3. **`TeamWappen(teamId, size)`** als kleine Compose-Komponente, damit die
   Screens nur Größe und ID setzen.
4. **Parser bleiben unverändert.** Die Wappen-URL wird aus der Team-ID
   konstruiert; nur dort, wo die ID fehlt, müsste nachgeparst werden (aktuell
   nicht nötig).
5. **Keine neue Abhängigkeit** nötig. Coil wäre komfortabler, ist aber weder
   vorhanden noch erforderlich; eine neue Bibliothek müsste laut `AGENTS.md`
   begründet werden.

## 5. Risiken und Grenzen

- **Netzwerklast:** Eine Ligatabelle lädt bis zu 18 kleine Bilder. Mit Cache,
  Deduplizierung und Laden nur sichtbarer Zeilen ist das unkritisch. Ohne Cache
  entstehen bei jedem Screen-Wechsel neue Requests.
- **Fallback:** `00000000.png` ist in der Doku als Fallback genannt, kommt in den
  Dumps aber nicht vor. Der Loader muss fehlende Dateien sauber abfangen.
- **Pfadvarianz:** Wappen stehen teils relativ (`images/wappen/...`), teils mit
  führendem Slash (`/images/wappen/...`). `OsApi.wappenUrls` liefert absolute
  URLs und deckt das ab.
- **Endungen:** png und gif gemischt; der Kandidaten-Fallback ist daher Pflicht.
- **Belegbasis:** Die Aussagen stützen sich auf die vorhandenen Server-Dumps,
  nicht auf einen Live-Login. Die Struktur ist über viele Seiten konsistent;
  bei künftigen Layoutänderungen sollten die Dumps ergänzt werden.

## 6. Fazit

Die Website setzt Wappen sparsam und gezielt ein: im eigenen Managerbüro, in
Ligatabelle, Spieltagen, Pokal, OSC und im Teamkopf. Genau diese Stellen lohnen
sich auch in der App. Der größte Gewinn bei geringstem Risiko ist die
**Vereinsseite**, gefolgt von **internationalen Wettbewerben** und der
**Ligatabelle**. Elf des Spieltags ist bereits gut gelöst. Dichte Listen
(Transfers, Manager, Statistiken, Kader) bleiben bewusst ohne Wappen – dort
zahlen die kleinen Bilder den Platzpreis nicht zurück.

Vor der Umsetzung sollte der gemeinsame Loader stehen; danach sind die
einzelnen Screens kleine, isolierte Änderungen.
