# Analyse Online-Soccer 2.0 – Phase 2: Authentifizierte Funktionsanalyse

- **Ziel:** Alle geschützten Bereiche und Schreibformulare eines echten Manager-Accounts untersuchen, um die native Android-App (Phase 3+) exakt abbilden zu können.
- **Methode:** Einzelne Anmeldung mit dem privaten Konto; ausschließlich **Lesezugriffe** (GET) und Formular-Strukturanalyse. **Keine** Schreib-/Finanzaktionen wurden ausgelöst (kein Speichern, kein Gebot, kein Transfer).
- **Datenschutz:** Keine Zugangsdaten, keine Session-/Cookie-Werte, keine persönlichen Spiel-/Kontostandswerte in diesem Bericht. Persönliche Ablenkungsdaten (Team-/Spielernamen) wurden bewusst nicht übernommen.
- **Stand:** Gültig für Saison 24, ZAT 2 (September 2026). Strukturen können sich während einer Saison ändern (abwärtskompatibel erkennen!).

---

## 1. Login

| Aspekt | Wert |
|---|---|
| Endpunkt | `POST https://os.ongapo.com/validate.php` |
| Content-Type | `application/x-www-form-urlencoded` |
| Pflicht-Parameter | `action=os_login`, `loginemail=<E-Mail>`, `passwort=<Passwort>` |
| Bild-Button (Browser-Replikat, empfohlen) | `imageField.x=10`, `imageField.y=10` |
| User-Agent | Browser-Stil **Pflicht** (ohne Browser-UA → HTTP 403 auf öffentlichen Seiten; nach Login auch okhttp-UA toleriert, aber Browser-UA beibehalten) |
| Erfolg | `HTTP 302` → `Location: os_menu_haupt.html`, Setzt **2 Cookies**: `os` (SID, Session-Cookie) + `lc` (Persistent-Token, s. §2) |
| Fehler | `HTTP 200` → Body enthält exakt „Der Username oder das Passwort ist falsch.“ (Seite ist ~1 KB, DOCTYPE XHTML) |
| Leer-POST (Demo-Zustand) | `HTTP 302` → `os_menu_haupt.html` (Demo-Manager-Ansicht, kein Fehlertext) |

**Login-Fehler ist eindeutig erkennbar** (fester Text). Die App kann daher:
- vor dem POST eine **syntaktische Plausibilität** prüfen,
- nach dem POST anhand von 302+`lc`-Cookie bzw. Fehlertext bewerten.

Hinweis: `action=login` (alte Form) existiert noch auf `index.php`; funktional maßgeblich ist `os_login`.

## 2. Session & dauerhafte Anmeldung („Relogin“)

| Cookie | Typ | Laufzeit | Zweck |
|---|---|---|---|
| `os` | Session-Cookie | bis Browser-/App-Schließen | Authentifizierungs-SID; identifiziert den eingeloggten Nutzer |
| `lc` | Persistentes Cookie | **2 Jahre** (`Max-Age=63072000`, echte Ablaufzeit im Set-Cookie) | „Angemeldet bleiben“-Token, 32 Hex-Zeichen |

**Kritische Erkenntnis (getestet):**
- Sendet man **nur `lc`** (ohne gültige `os`), antwortet der Server mit `Set-Cookie: os=<neue SID>; path=/` und liefert die **persönliche Ansicht** aus → **stiller Relogin ohne Benutzereingabe**.
- `index.php` („Relogin“/Login-Seite) **beendet die bestehende Session nicht**; sie ist nur ein Formular.
- HTTP-Status ist für die Session-Erkennung **ungeeignet**: `haupt.php` liefert auch ohne/unberechtigteSession **HTTP 200**.

**Erkennung „eingeloggt“ / „Demonstration“ pro Seite (Inhalts-Marker):**
- Eingeloggt: persönliche Elemente (z. B. Markierung „Kontostand“ auf `haupt.php`).
- Nicht eingeloggt/abgelaufen: Demo-Manager-Ansicht (`DemoTeam`), Login-Formular sichtbar.
- „Ohne Team gesperrt“-Texte erscheinen nur in Team-Funktionsseiten ohne Team.
- App-Workflow: Bei jedem Seitenabruf prüfen → Ist nur die Demo-/Login-Ansicht da: `lc` reichen (still), sonst echten Login anstoßen.

## 3. Dashboard (`haupt.php`)

- Frameset-Shell: `os_menu_haupt.html` → Frames `wappen.php`, `menue.php`, `haupt.php`.
- `menue.php` lädt das Menü **rein per JavaScript** (Generic-Renderer `js/osmenu1.js`); die App darf daher **keine Abhängigkeit vom We>-Menü** eingehen, sondern baut eine eigene Navigation (ohnehin geplant).
- `haupt.php` enthält (Struktur, keine Werte): Manager-Begrüßung, **Kontostand**, aktueller **ZAT + Termin** („ZAT ist ZAT <n> und liegt auf …“), Liga (Bsp. „Liga B <Land>“), **Tabellenplatz**-Link (`tplatz.php?t=<teamid>`), einstehenden Ligaspieltag inkl. Heim-/Auswärtsteam, **FSS-Einladungen**, PM-Zähler („0 neu“), Verweise auf `friendly.php`, `ka.php`, `/osneu/pm`, `zugabgabe.php`, Link „Zweitteam“ (`?changetosecond=true`).
- Hilfreich: `osfunc.js` definiert zentrale **Info-Endpunkte** (alle per Popup/GET):
  - `sp.php?s=<spielerid>` Spielerinfo
  - `st.php?c=<teamid>` Teaminfo
  - `spielpreview.php?t1=<heim>&t2=<gast>&type=<1|2|…>` Spiel-Vorschau
  - `tplatz.php?t=<teamid>` Tabellenplatz
  - `rep/saison/<saison>/<zat>/<heim>-<gast>.html` Spielbericht (static)
  - `transferview.php?tr=<transfernr>` Transfer-View
  - `bericht.php?s=<spiel>` ZAT-Bericht (statisch)
  - „Zugabgabe speichern (neu)“ öffnet `/osneu/checkza` (Bestätigungs-/Check-Ansicht, s. §5)

## 4. Mannschaft / Teamseite

- Spielerdaten kommen aus `sp.php?s=<id>` (HTML) und aus Listen (`ju.php`, `training.php`, `zugabgabe.php`, …) - überall sind Spielerzeilen in klassischen Tabellen mit **Klassen `TOR/ABW/DMI/MIT/OMI/STU`** markiert (Position). Übergabeparameter sind die **internen Spieler-IDs** (6-stellig, z. B. 104203 – bei jeder Saison neu; NIE hart-kodieren).
- Positionen (1..6): `TOR`, `ABW`, `DMI`, `MIT`, `OMI`, `STU` (deckungsgleich mit Filterwerten in §9/16 und Spielerklassen).
- `te.php` (Teamseiten-/Wappen-Editor): Drag&Drop (Scriptaculous) arrangiert Bildelemente in `#list`; `save()` serialisiert die Knoten-IDs und schreibt sie ins versteckte Feld `pnr` des Formulars `nrform` (POST). Zusätzlich Formular „Wappenhintergrund transparent“ (`trans`, Felder `x`,`y`). **Nativ machbar, aber höherer Aufwand** (Drag&Drop „ablegen“).

## 5. Zugabgabe (Aufstellung) – der komplexeste Bereich

Es existieren **zwei koexistierende Editoren** mit gleichem Datenmodell:

### 5.1 Klassischer Editor `zugabgabe.php` (Tabs via `?p=0|1|2`)
- Tabs: `p=0` Aufstellung, `p=1` Aktionen, `p=2` Einstellungen.
- Der gesamte Kader wird als Tabelle mit **einem `<select name="ra[<spielerid>]">` pro Spieler** gerendert. Options = **Aufstellungsplatz-Codes**:
  - Abwehr: `A..H`, Mittelfeld: `K`,`L`, Angriff/Sturm: `T..Z`, `""` = nicht aufgestellt.
- Speichern (GET-Formular): `?p=0&ra[<pid>]=<Platz>&…&aufspeichern=<…>` – alle Select-Werte des Kaders werden mitgesendet.
- Formation laden: `<select name="raster1">` (gespeicherte Formationen, IDs dynamisch) + Submit `raster`.
- Anderen ZAT laden: `<select name="lauf">` (1..72) + Submit `Laden`.
- Löschen: GET-Formular `delete_form` mit `del_za=1&p=0`; durch JS `confirmdelete()` bestätigt („Aufstellung unwiderruflich verwerfen?“).
- URLs `/osneu/checkza` („Zugabgabe speichern (neu)“) und `checkza.php` („Zugabgabe speichern“) = **Bestätigungs-View**: zeigen Aufstellung (Platz=Spieler), Aktionen, Einstellungen und Endzustand („Zugabgabe erfolgreich gespeichert!“) – reine Lese-Ansicht, kein Formular.
- Kopfzeile zeigt: ZAT, Termin, Gegner, Hide/away, „Zugabgabe: Gültig“.
- Navigations-Unterfunktionen in `zugabgabe.php` nutzen weiße Popups: weitere Dateien `ka.php` (Kader), `friendly.php`, `/osneu/pm`.

### 5.2 Beta-Editor `zugabgabe_beta.php` (Drag&Drop mit freier Gitterposition)
- jQuery-UI Feld (`#field`) = 11 × 15 Gitter; 1 Torwart (`#goal`) + 10 Feldspieler + 6 Ersatz (`#ersatz`).
- **Speichern**: JS baut JSON-Array und schreibt es ins versteckte Feld `aufstellung`:
  ```json
  [ [<pid_tor>,0,0],
    [<pid_feld1>,<links 1..11>,<oben 1..15>], ... (10 Feldspieler),
    [<pid_ersatz1>,-1,-1], [<pid_ersatz2>,-2,-1], ... (6 Ersatz, Index negativ) ]
  ```
  → `POST https://os.ongapo.com/zugabgabe_beta.php` mit `aufstellung=<JSON>` (+ `save`-Button als JS-Klick, Formular `style="display:none"`).
- Löschen: Button `delete`, bestätigt per JS-Doppelklick-Mechanik.
- Formulare existieren in Werksmarkup: `<form method="POST" action="zugabgabe_beta.php" style="display: none">` mit `<input type="hidden" name="aufstellung">`.

### 5.3 Bewertung für die App
**Beide Wege sind voll nativ umsetzbar** (A.):
- Klassisch: Kader-IDs + Slot-Codes (A–Z) sind triviale Datenstrukturen; Speichern per GET mit Name-Array.
- Beta: freie Gitterkoordinaten (Spalte 1..11, Zeile 1..15) + Ersatzbank mit negativen Indizes; JSON-Assemblierung exakt replizierbar.
- Empfehlung Phase 3: eigener nativer Aufstellungs-Editor mit Gitter (Beta-Datenschema) + Verwendung des klassischen Save-Wegs als Fallback; **kein WebView nötig**. Freies Platzieren mit Slot-Mapping serverseitig.

## 6. Taktik (`taktiken.php`)

- Zwei POST-Formulare:
  - **form1** (Verwaltung): `<select name="raster1">` + `<select name="raster2">` (gespeicherte Taktiken, dynamische IDs) + Buttons `Laden` (`load`), `Löschen` (`delete`).
  - **form2** (Editor): Feldraster mit **`<input type="checkbox" name="taktik[]" value="<Zeile><Spalte>">`**, Werte z. B. `A1`, `O10` (Zeilen A–O/P, Spalten 1–11). Genau die belegten Positionen sind vorausgewählt (Beispiel: 15 Checkboxen). Zusatzfelder: `speichername` (Text) + Button `Speichern unter...` (`speichern`).
- onChange ruft `changeCheckbox('A10','<farbe>')` – reine Darstellung (Einfärbung), kein Serverzugriff.
- **Bewertung: A (nativ).** Formation = Menge an `taktik[]`-Codes; Speichern via POST `taktik[]`-Array. Erfordert nur ein Feldraster in Compose.

## 7. Training (`training.php`)

- Ein großes POST-Formular über den ganzen Kader, pro Spieler **zwei Selects** (erkennbar am Präfix):
  - `tr1<pid>` = **Trainee-Slot** (Trainer/Slot auswählen): Optionen `---`(0), `T 1 60` … `T 6 60` (6 Trainingseinheiten à 60 Min.).
  - `tr2<pid>` = **trainierter Skill**: Optionen `ABS`(1), `BAK`(2), `FAN`(3), `STB`(4), `DEC`(5), `GES`(6), `AGG`(7), `PAS`(8), `AUS`(9), `UEB`(10), `ZUV`(11).
- Zwei weitere Aktionen:
  - `trainingspeichern` („Trainingseinstellung speichern“) + `trainingdelete` (löschen) – für benannte Presets,
  - Preset-Auswahl `trainingsaveas`, neues Preset `trainingsave` (Textfeld, „Neue Speicherung anlegen“), Laden `trainingload`, Löschen `trainingdelete` + `<select name="trainingsdelete">` (JS `condel()`).
- Hinweis im HTML: „Änderungen vorher unten speichern“ → pro Zeile ändern, einmal speichern.
- **Bewertung: A (nativ).** Einfache Select-Matrix; pro Spieler zwei Werte. Presets nativ abbildbar.

## 8. Trainer (`trainer.php`)

- 6 Trainer-Slots (1..6). Pro Slot ein POST-Formular:
  - Hidden `trainer=<1..6>`,
  - `<select name="skill">` mit Werten 65, 67.5, 70, 72.5, 75, 77.5, 80, … (Trainingsskill-Vorgabe),
  - `<select name="dauer">` Vertragsdauer in Monaten: 6, 12, 18, 24, 30, 36…,
  - Submit `einstellen` („einstellen als Trainer <n>“).
- Übersichtstabelle pro Trainer: Gehalt, Skill, aktuelle Vertragsdaten.
- **Bewertung: A (nativ).** Qualifizierte einfache Formulare.

## 9. Transfers

**Sicherheitsregel der App (Pflicht): Jede finanzielle Aktion braucht eine klare zweistufige Bestätigung durch den Nutzer.** Die Webschnittstelle hat auf den Finanzseiten **keine eigenen JS-`confirm()`e** – der Schutz liegt serverseitig (Phasen-Fluss/erneutes Rendering). Die App muss daher eigene Bestätigungsdialoge forcieren.

### 9.1 Transfermarkt Eingabe (`transfer.php`) – KAUF
- „Transfereingabe Phase I“: POST, Felder `geld` (Text, Betrag) + Buttons `abort` („Abbrechen“) und `weiter1` („Weiter nach Phase II“). Phase II ist der **Bestätigungsschritt** (Random-Transfer-Angebot wird dann validiert).
- **In der App: Phase II als explizite Einverständnis-Sperre abbilden.**

### 9.2 Transfersuche (`tm.php`) & VM-Suche (`viewvm.php`)
- POST-Filterformular, Felder:
  - `alter` (10 Stufen: 0=Alter, 16-18, 19-20, 21-22, 23-24, 25-26, 27-28, 29-30, 31-32, 33+)
  - `skill` (Stufen: 0-20 / 20-30 / 30-40 / 40-50 / 50-60 / 60+)
  - `marktwert` (Stufen bis 50 Mio.+)
  - `position` (TOR/ABW/DMI/MIT/OMI/STU)
  - `sortierung` (auf-/absteigend nach Alter/Dauer/Marktwert/Opti/Position)
  - Submit `sshow` („Spieler anzeigen“).
- Ergebnislisten enthalten Links `bieten` → `gebot.php?s=<snr>` (TM) bzw. `vmgebot.php?s=<snr>` (VM) – **Gebotsformulare** (Kaufpreis), niemals automatisch auslösen.
- Neue Liste in `/osneu`: `osneu/transferliste` (GET, s. §16).

### 9.3 Schnelltransfer (`blitz.php`)
- POST-Formular, Checkboxen `blitz[]` mit Spieler-IDs + Submit „Spieler per Schnelltransfer loswerden“. **Finanzielle Aktion → App-Bestätigung erforderlich.**

### 9.4 VM-Setzen (`vmsetzen.php`)
- 8 unabhängige POST-Formulare (je ein verfügbarer Spieler): Hidden `vmsetzen=<spielerid>`, `<select name="startpreis">` (Staffelwerte in Tausendsteln: 25=350.923 … 80=1.122.954 u. w.) + Submit „auf den VM setzen“.
- **Finanzielle Aktion → App-Bestätigung.**

### 9.5 Leihspieler (`leihspieler.php`)
- Wie Transfermarkt: POST, `lplayer` (Radio mit Spieler-IDs), `geld` + `abort`/`weiter1` (Leihe, Phase II = Bestätigung).

### 9.6 Vertragsverlängerung (`vt.php`)
- POST-Formular, pro Spieler Radios `gehalt[<pid>]` mit Werten 1..4 (Gehaltsstufen) + Zusammenführen `gehalt4[<pid>]` (Sonderfall), Submit `vertragsauswahl` („Verträge verlängern“) + Reset.
- **Finanzielle Aktion → App-Bestätigung.**

### 9.7 Transfereinstellungen eigene Spieler (`tstatus.php`)
- Zwei POST-Formulare:
  - Checkboxen `tdetails[<pid>][K|L|T|V]` (Kauf/Leihe/Tausch/Verleihen)
  - Radios `tstatus[<pid>]` Werte 0..3 (Setzstatus)
  - Textfelder `tmindest[<pid>]` (Mindestablöse) und `ttext[<pid>]` (Verkaufstext)
  - Submit `switch` („Einstellungen speichern“).
- **Bewertung: A (nativ)** für 9.2–9.7; die Gebotsformulare `gebot.php`/`vmgebot.php` sind als Popup-Formulare zu patchen (simple Feldstruktur: Preis + eigenes Bestätigen).

## 10. Stadion (`/osneu/stadion`)

- Ein POST-Formular `action=/osneu/stadion` mit Hidden `action=doAngebot`:
  - `number`-Felder: `NeuSteh`, `NeuSitz`, `NeuUSteh`, `NeuUSitz` (jeweils 0..10000), sowie readonly-Anzeigen (`USteh`, `USitz`, `StehZuSitz`, `UStehZuSitz`),
  - `<select name="tafel">`, `<select name="heizung">` (Anzeigetafel/Heizung),
  - Submit „Angebote einholen“ (+ Reset „Zurücksetzen“).
- Stadionaufträge sind Server-Angebote (finance); die App sollte die Werte als Bestätigungsschritt spiegeln.
- **Bewertung: A (nativ).**

## 11. Jugend

### 11.1 Jugendteam (`ju.php`)
- Ein POST-Formular: Radios `ziehmich` mit Spieler-IDs (Jugendsicht) + Submit `ziehen` („Markierten Spieler ins A-Team berufen“).

### 11.2 Jugendscouting (`juscout.php`)
- **Kein Formular** – reine Infoseite (6 Kandidaten). Eventuelle Gebote: `juscout.php?g=<snr>` (Popup-Gebotsformular, `jubieten()`), bitte als eigene Bestätigungsflüsse abbilden.
- **Bewertung: A** (Listen + einfache Formulare).

## 12. Finanzen (`steuer.php`, Konto, ZAT-Kontoauszug, `zuzu.php`-Nebenformular)

- `steuer.php`: **kein Formular**, reine Steueranzeige.
- `haupt.php` zeigt Kontostand; weitere Finanzdetailseiten sind im klassischen Menü (Kontoauszug/Gehälter) vorhanden.
- `zuzu.php` („Zusätze“): POST-Formulare
  - Checkboxen `physio[<pid>]` (Zum Physio schicken),
  - Textfelder `int` / `liga` / `pokal` (Eintrittspreise Heimspiele: international / Liga / Pokal),
  - Buttons `speichern` und `pspeichern` („Zum Physio schicken“).
- **Bewertung: A (nativ)**; alle Wert-Elemente sind einfache Felder.

## 13. Private Nachrichten (PM) – `/osneu/pm` (REST-ähnlich)

| Funktion | Aufruf |
|---|---|
| Inbox/Outbox | `GET /osneu/pm` (Tabular-Ansicht; JS-Tabs `#inbox`/`#outbox`) |
| **Lesen** | `GET /osneu/pm/read/<pmid>` → HTML (markiert gelesen) |
| **Löschen** | `GET /osneu/pm/delete/<pmid>` (per JS `confirm("PM entfernen?")` + `.get`) |
| **Antworten** | `GET /osneu/pm?action=reply&pn_id=<pmid>` (bestückt das Nachrichtenfeld im Popup) |
| Schreiben | `POST /osneu/pm?action=writeNew` |
| Empfänger-Vorschläge | `POST /osneu/ajax/findUser` mit `keyword=<≥3 Zeichen>` → HTML-Liste `userID|<name>` |

**writeNew-Formular (Popup-Seite `pm_write`)** – sehr leichtgewichtig:
- `pn_empfaenger` (Text, Autovervollständigung), `pn_betreff` (Text),
- Hidden: `pn_empfaenger_id` (empfänger-ID), `pn_transfer_id` (0 = neu),
- `pn_text` (textarea), verstecktes `quoteText` (falls Antwort/Zitat),
- Submit „Senden“.
- Editor: **Sceditor** (BBCode-Richtext mit Toolbar, `autoUpdate:true` hält `pn_text` aktuell). Der Server erwartet **BBCode** in `pn_text`.

**Bewertung: A (nativ).** Lesen/Löschen/Antworten sind simple GET-URLs; Schreiben = ein POST mit reinen Textfeldern. In der App reicht ein nativer BBCode-Editor (oder Plain-Text-Senden) + BBCode-Rendering für die Lesenseite. Nur wenn der volle Sceditor-WYSIWYG unverzichtbar ist → WebView-Fallback möglich (Cookie-Sync s. §20).

## 14. Benachrichtigungen (`/osneu/benachrichtigungen`)

- Ein POST-Formular (`action=speichern`), mail-Checkboxen: `mailTransfer`, `mailGebotVM`, `mailMGebotVM`, `mailSchnellTransfer`, `mailVertragsverlaengerung`, `mailFriendly`, `mailPmDirekt`, `mailPmHour`,
- SMS-Bereich: `<select name="smsAbonnement">`, `smsMN` (Mobilnummer mit Ländervorwahl), `smsHinweisNutzung`,
- Weitere: `showKonto`, `urlaubVon`/`urlaubBis` (`<input type="date">` = HTML5-Datumsfelder),
- Submit „Speichern“.
- **Bewertung: A (nativ).**

## 15. Spiele/ZAT/Spielberichte

### 15.1 Livegame (Live-Ticker)
- Einstieg: `GET https://os.ongapo.com/livegame/?spiele=<heim1>,<heim2>,…` → 302 auf `/livegame/php/index.php?spiele=…` (Pseudo-SPA, `js/livegame.js` + `configuration.js`, jQuery).
- **JSON-Datenendpunkt (verifiziert):**
  `GET https://os.ongapo.com/livegame/php/data.php?action=gamedata&teamid=<homeid>&zat=<zat>`
- **JSON-Struktur (vollständig dokumentiert):**
  ```
  game        { zat, home{id,name,emblem}, away{...}, url (XML-Report-URL), played }
  reporturl   (HTML-Report-URL)
  cup, visitors, gametype, date, stadium
  squad       { home:[], away:[] }          // Aufstellungen
  tactics     { home,away: { commitment, hardness, playtype, defence, midfield, offence } }
  actions     []                             // Live-Aktionen (Minute, Text, …)
  gamestatistics { home,away: { goals, offside, corners, fouls, penalties, posession } }
  teamdata    { home,away: { skill, opti, morale, fitness } }
  playerstatistics [], maxEventsPerMinute
  ```
- **Bewertung: A (nativ)!** Alle Live-Daten (Aktionen, Taktik, Statistik, Aufstellung) liegen als strukturiertes JSON vor – ein nativer Live-Ticker ist direkt umsetzbar. Audio-/Sprachausgaben (`*.wav`, Athmo) sind optional & rein dekorativ.

### 15.2 Spielberichte
- HTML: `rep/saison/<saison>/<zat>/<heim>-<gast>.html` (statisch).
- XML-Referenz in `game.url` (`rep/saison/24/2/<heim>-<gast>.xml`) existiert **nur wenn `played=true`** (sonst 404).
- Ungespielte Spiele → Spielstand/Detailseite leer. Spielpreview: `spielpreview.php?t1=&t2=&type=` (HTML).
- **Bewertung:** statisches HTML → entweder **B** (HTML parsen für native Ansicht) oder **C** (WebView) – Empfehlung: eigenen Bericht-View (B) mit WebView als Fallback; XML (wenn vorhanden) lässt sich direkt nativ parsen.

### 15.3 ZAT-Zusammenfassung (`zar.php`)
- POST-Formular: `<select name="saison">` + `<select name="zat">` + „ansehen“ (lädt die statische Übersicht). **A (nativ)** nach Parsing.

### 15.4 Saisonplan (`/osneu/saisonplan`) & OSCGP (`/osneu/oscgp`)
- `saisonplan`: reine Anzeige (11 KB, keine Formulare).
- `oscgp`: POST-Filter (`season`, `ergebnisse`, „Anzeigen“) + Button „Konferenz ansehen“ (JS). Tore/Ergebnisse je Spiel.

### 15.5 Freundschaftsspiele/FSS
- Klassisch `friendly.php`: 4 POST-Formulare
  - Blind-Einladung: `<select name="blindzat">` + Submit `blind` („Blinde Einladung absenden“) + Checkbox `doppelt`,
  - Team-Übersicht: `sland` („Teams anzeigen“) + Selects `land`/`lliga`,
  - ZAT-Reservierung: `<select name="reserve[]" multiple size=5>` + Submit `sreserve`,
  - Alle löschen: Hidden `deleteall` + Submit `confirmdelete` („Alle Reservierungen löschen“).
- Neu `/osneu/friendlies`: 5-6 POST-Formulare auf gleicher Logik (`FssLiga`, `FssLand`, `showFssTeams`, `blindZat`, `blindDouble`, `insertBlind`, `reserveZat[]`, `insertResZat`, `delResAll`, Storno je fest gebuchter ZAT: Hidden `FssId` + `requestStornoFssFixed`).
- Turniere (`fssturnier.php`): POST-Formulare für Offene Turniere (Checkboxen `ntzat[]`, Hidden `turnierid`, Radio `ntart`, Submit „Anmelden“) und **eigene Turniererstellung** (`ntname`, `ntpassword`, `ntteilnehmer`, `ntbeschreibung`, Submit „erstellen“/„anmelden“).
- **Bewertung: A (nativ)**, alle simple Select/Checkbox-Formulare.

## 16. Wettbewerbe
- Liga-Tabelle: `tplatz.php?t=<teamid>` / Tabellen-Funktionen (`tabellenplatz(team)`).
- **Oscar-/Tabelle-Deltas** im klassischen Paint: `tplatz.php`, Ligenviews, `ka.php` (Kader).

## 17. Statistiken
- Klassische Statistiken (`tstatus`-Ähnlichkeit), Spielberichte (§15), Bericht-URLs und Saison-Archiv-Ansichten (`zar.php`, `rep/...`).
- `osneu/transferliste` (Filtern, s. §9.2) – GET-Formular mit verstecktem `gesucht=1`; Filter: `alter`, `skill`, `opti`, `abloese`, `position`, `tstatus` (A/T), `tdetail` (Kauf/Tausch/Leihe), `sortierung`, `tinfo`, `proSeite` (25/50/75/100).

## 18. Sonstiges
- `einstellungen.php`: OPTION1 (Spieleralter: Jahre / 2-Dezimalstellen), OPTION2 (Teamperformance in Tabelle ein/aus) + `speichern`. (Weitere Einstellungen liegen über `/osneu/...`)
- `/osneu/passwort`: 2 POST-Formulare (Passwort ändern: `pwCurrent`/`pwNew`/`pwNewCheck`; Vertreterpasswort: `pwVertreter`/`pwVertreterCheck`).
- `support.php`: POST `stitel` + `stext` (Support-Ticket).
- `bewerbung.php`: keine Formulare (aktuell keine Bewerbung erlaubt).
- `zweitteam.php`: Checkbox `getteam` + „Zweitteam übernehmen“ (nur bestätigtes Einverständnis).
- `osbetafaces.php` (Trikot-Editor): Farbfelder `farbe1..3`, `tfarbe1..3` (HTML5 `color`), `muster`, `tunte`/`kragen`, `tkragen` + Speichern.
- `zfeatures.php` (Zusatzfunktionen/Guthaben): PayPal-Käufe (extern, `paypal.com/cgi-bin/webscr`, hosted IDs – **externer Bezahlfluss, App muss diesen nicht abbilden**), „Spitzname“ (`snspieler` Radio + `sntype` 1..3, „verbindlich Buchen“ = finanzielle Aktion), Trainingsintensität-Buchung (`wfr` Select + „buchen“).
- **A (nativ)** für alle, außer PayPal (extern → Wrapper/Deep-Link oder Hinweis).

## 19. Klassifikation A/B/C/D (finales Ergebnis Phase 2)

| Bereich | Seite(n) | Einstufung | Anmerkung |
|---|---|---|---|
| Login/Session | `validate.php`, Cookies | A | + `lc`-Relogin (s. §2) |
| Dashboard | `haupt.php` | A (B parsen) | HTML-Parsing |
| Kader | `ju.php`, Kaderlisten | A/B | Listen parsing + einf. Formulare |
| Aufstellung (klas.) | `zugabgabe.php` | **A** | Slot-Codes A–Z, GET-Save |
| Aufstellung (beta) | `zugabgabe_beta.php` | **A** | Gitter + JSON (`aufstellung`) |
| ZA-Check/Bestätigung | `/osneu/checkza` | A (B parsen) | Nur Anzeige |
| Taktik | `taktiken.php` | **A** | `taktik[]`-Raster |
| Training | `training.php` | **A** | `tr1/tr2<pid>`-Selects |
| Trainer | `trainer.php` | **A** | Simple Formulare |
| Transfers TM/Suche | `tm.php` | A | Filter-Formular |
| VM-Suche/Setzen | `viewvm.php`, `vmsetzen.php` | A | Filter + Startpreis |
| Gebote | `gebot.php`, `vmgebot.php` | A | Bestellformulare, App-Confirm! |
| Schnelltransfer | `blitz.php` | A | Checkboxen, App-Confirm! |
| Leihe | `leihspieler.php` | A | Radio + geld, Phase II |
| Verträge | `vt.php` | A | gehalt[pid] |
| Stadion | `/osneu/stadion` | **A** | Einfache Formulare |
| PM | `/osneu/pm/*` | **A** | REST-GET + POST writeNew |
| Benachrichtigungen | `/osneu/benachrichtigungen` | **A** | Einfaches Formular |
| Livegame | `/livegame/php/data.php` | **A** | **JSON-Api!** |
| Spielberichte | `rep/...html`, `.xml` | B oder C | Parser oder WebView |
| ZAT-Summary | `zar.php` | A/B | Formular + Parsing |
| Saisonplan/OSCGP | `/osneu/saisonplan`, `oscgp` | A | Anzeige/Filter |
| FSS | `friendly.php`, `/osneu/friendlies`, `fssturnier.php` | A | Einfache Formulare |
| Wappen-Editor | `te.php` | B | Drag&Drop (Scriptaculous) → nativ machbar, Aufwand mittel |
| Trikot-Editor | `osbetafaces.php` | A | Farb-Formulare |
| EM-Tippspiel | `emtipp.php` | A | 72 Felder `torea[n]/toreb[n]` + speichern |
| Einstellungen/Optik | `einstellungen.php`, `osneu/passwort` | A | Simple Formulare |
| Support | `support.php` | A | stitel/stext |
| Forum | `forum/` | C (WebView) | WoltLab, eigener Login/CSRF |
| Wiki/Bug/Shop | extern | unverändert | nur Verweisen |
| Zusatzfunktionen | `zfeatures.php` | A | App-Umsetzung wählbar; PayPal extern |

**Ergebnis: Fast alle für das Spiel wesentlichen Funktionen sind rein nativ (A) umsetzbar.** WebView nur für: klassisches Forum, statische Spielberichte (optional), ggf. Sceditor-WYSIWYG (optional).

## 20. Risiken & Schutzmaßnahmen

1. **Finanzielle Aktionen**: Webschnittstelle hat keine JS-Confirms → die App MUSS für `gebot/vmbieten/blitz/transfer/leih/wt/zfeatures` einen **eigenen irreversiblen Bestätigungsdialog** (Spieler + Betrag + „endgültig“-Haken) haben. Nie versehentlich senden.
2. **Session-Handling**: `os` ist flüchtig; **`lc` (2 Jahre) ist ein Secret wie ein Passwort** → App speichert Cookies in **EncryptedSharedPreferences / Android Keystore**, nie in Klartext, nie in Logs/Reports. Silent-Relogin nur mit dem kombinierten Risiko-Token machen.
3. **Server ist Quelle der Wahrheit**: Keine lokale Spiellogik replizieren; Cache nur für Anzeige. Nach jeder Aktion Daten vom Server neu laden statt „blind“ lokal zu optimieren.
4. **IDS sind volatil** (Spieler-, Team-, Transfer-, PM-, Turnier-, Formation-IDs): nie hartkodieren; immer aus den Antworten ableiten.
5. **Strukturwandel**: Während der Saison ändern Formulare/Slots. Alle Parser defensiv schreiben; unbekannte Select-Werte als „unbekannt“ darstellen, nicht abbrechen.
6. **Rate-Limits**: Keine automatisierten Massenabrufe; Polling-Häufigkeit niedrig halten (FSS/PM-Check z. B. ≥ 60 s, optimal „on demand“).
7. **UA**: Browser-User-Agent überall setzen (auch OkHttp), sonst 403 auf öffentlichen Seiten.
8. **Demo-Verwechslung**: Nur „DemoTeam“ + fehlende persönliche Markierungen = nicht eingeloggt. Aufpassen: `DemoTeam` kann auch als echter Teamname in PMs/Beiträgen vorkommen → Klassen-/Positionsmarker + Login-Marker kombiniert prüfen.

## 21. Empfohlene Android-Architektur (auf Basis Phase 2)

- **Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, OkHttp (+ OkHttp CookieJar), Kotlinx Serialization, Jsoup (nur wo HTML-Parsing nötig), Room für kleinen UI-Cache.
- **Netzwerk/Session:**
  - Shared OkHttp-Client mit festem Browser-UA.
  - Custom `CookieJar` → persistiert `os`/`lc` verschlüsselt (Keystore).
  - **AuthState-Machine:** `ANONYMOUS → LOGGING_IN → AUTHENTICATED → SILENT_REAUTH → SESSION_EXPIRED`. Jede abgeschlossene Antwort bewertet die Login-Marker. Bei Demo-/Login-Ansicht: erst `lc`-stillen Relogin, dann echten Login anstoßen.
- **Repositories:** pro Domäne (Team/Kader, Aufstellung, Taktik, Training, Transfers, Stadion, PM, Livegame, FSS). Alle Schreiboperationen laufen über den **ActionDispatcher** mit implizitem Bestätigungschritt für Finanzen.
- **Livegame:** `data.php`-JSON als Quelle; Compose-Bildschirm rendert Aktionen/Statistik direkt.
- **WebView-Integration (nur falls nötig, z. B. Forum/Reports):**
  - Cookie-Sync OkHttp→WebView: vor dem Laden einer `os.ongapo.com`-URL die aktiven Cookies (inkl. `os`/`lc`) per `CookieManager.setCookie("https://os.ongapo.com", "<name>=<wert>; path=/")` setzen (gleiche Domain, `Path=/`).
  - WebView-UA auf dieselbe Browser-Zeichenkette setzen (UA-Filter!).
  - Nur eine Loginstelle – nie doppelt einloggen; `lc`-Relogin nur über OkHttp, Cookies danach erneut synchronisieren.
  - Optional: Cookies **nicht** für dritte Domains (PayPal, Forum-Login `t`) streuen.
- **Encoding-Konvention:** Alle Seiten sind UTF-8 bzw. klassisch ISO-8859-1 (alte `training.php`/Menü) → jede Antwort einzeln mit korrektem Charset behandeln.

---

*Phase 2 abgeschlossen. Bereit für Phase 3 (Android-App-Umsetzung) nach Freigabe.*