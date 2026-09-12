# Technische Analyse – Online Soccer 2.0 (os.ongapo.com)

Analyse-Datum: 05.09.2026 · Nur Lesezugriff / Public-Fläche · Kein Login mit echten Zugangsdaten durchgeführt
Wichtig: keine Speicherung von Zugangsdaten, keine Umgehung von Sicherheitsmechanismen, keine Angriffe. Nur das geprüft, was ein normaler (auch nicht eingeloggter) Besucher über die Website nutzen kann. Viele Seiten liefern ohne Login eine Demo-Ansicht („DemoTeam“), wodurch die Datenstrukturen trotzdem vollständig dokumentiert werden konnten.

---

## Kurzüberblick

- **Spieltyp:** Klassischer Browser-Fußballmanager (PHP, ca. 2005–2010er-Technik, seit Jahren aktiv weiterentwickelt), rein serverseitig gerenderte HTML-Tabellen.
- **Zwei Oberflächen:**
  1. **Klassischer Teil** `/` – Frameset-App (Menü, Kopf, Hauptframe), XHTML 1.0 Strict, Design: dunkelblau `#111166` + weiß + gelb `#FBF9B3`, Tabellenlayout.
  2. **Neuer Teil** `/osneu/` – Moderner umgebauter PHP-Bereich (eigene Templates, CSS-Variablen, JSON-AJAX für Suche/Spielervergleich), gleiches Farbschema. Wird für immer mehr Funktionen genutzt (Stadion, Transfers, Ranking, PM, Wettbewerbe).
- **Keine offizielle API.** Es gibt einzelne AJAX/JSON-Endpunkte (Spielersuche, Autocomplete, Livegame), der Rest ist klassisches HTML über GET/POST.
- **Auth:** PHP-Session über Cookie `os=<sid>`; Login via `POST validate.php`. Kein CSRF-Token in den Spielformularen.
- **Kein SSO:** Forum (WoltLab), Wiki (MediaWiki), Bugtracker (MantisBT) und Shop (PrestaShop) haben **eigene, getrennte Logins**.

---

## A – Funktionsübersicht (vollständige Liste)

Legende Zugriff ohne Login: ✅ = öffentlich / Demo-Daten · 🔒 = geschützt („ohne Team“, „angemeldet sein“ usw.) · ⚠️ = teilweise

### Managerbüro / Starter ✅
- Dashboard (Büro): Team, letztes/nächstes Spiel, Livegame-Link, Infozeile (Logins, Zugabgabe-Status, Kontostand, PMs, FSS-Einladungen, NMR), Tipp des Tages
- Kalender (eigene Termine + Einladungen), Spielerbeobachtung, Notizblock, Kalender verwalten
- Livegame-Konferenz (animierte Spielwiedergabe)

### Team / Kader
- Teamübersicht (Kader, Nr., Name, Alter, Pos, „Auf“-Status, Land, Jugend? U, Moral, Fitness, Skillschnitt, Opt.Skill, Sonderstatus K/G, Sperre, Verleihstatus, Tore/TS) – `showteam.php?s=0` / `st.php?c=<tid>` ✅
- Vertragsdaten (Gehalt, Vertragslaufzeit, Marktwert, Geburtstag) – `s=1` ✅
- Einzelwerte (alle 18 Skill-Werte je Spieler) – `s=2` ✅
- Statistik Saison / Gesamt (Spiele/Tore/Vorlagen/Score/Gelb/Rot je LI/LP/IP/FS) – `s=3`/`s=4` ✅
- Teaminfo (Stadiongröße, Sitz-/Stehplätze, überdacht, Rasenheizung, Anzeigetafel, MW-/Gehaltssummen) – `s=5` ✅
- Saisonplan (Spielplan je Saison via POST `saison`) – `s=6` ✅
- Vereinshistorie, Transferhistorie, Leihhistorie, Saisonhistorie – `s=7..10` ✅
- Tabellenplätze-Verlauf (Bild-Grafik) – `tplatz.php?t=` / `tabellenplatz.php?t=&s=` ✅

### Jugend
- Jugendteam (`ju.php`) 🔒
- Jugendscouting + Gebot auf Talent (`juscout.php`; `juscout.php?g=<id>`) 🔒

### Trainer / Training / Taktik 🔒
- Trainer einstellen/verwalten (`trainer.php`)
- Training einstellen (`training.php`)
- Taktik-Editor (`taktiken.php`)
- Team-Editor: Rückennummern per Drag&Drop (`te.php`, POST `pnr`; ohne Login möglich ⚠️) + Wappen-Upload (aktuell deaktiviert)
- Verträge verlängern (`vt.php`)

### Wettbewerbe (national) ✅ (Anzeige öffentlich)
- Ligaspieltage mit Ergebnis-/Vorschau-/Bericht-Links (`ls.php`)
- Ligatabelle in 8+ Varianten inkl. Kreuztabelle (`lt.php`)
- Landespokal (`lp.php`)
- ZAT-Ergebnisse aller Ligen eines Landes (`zer.php`)
- ZAT-Report eigener Mannschaft (`zar.php`) 🔒

### Wettbewerbe (international) ✅ größtenteils
- OS-Ranking, Club-Ranking (`/osneu/`)
- Internationale Teilnehmer
- OSC Qualifikation, Championscup Gruppenphase (GP), Finalrunde (FR)
- OSE Qualifikation, Europacup GP, FR
- Supercup
- Alt (bis Saison 22): Championscup Hauptrunde/Zwischenrunde (`oschr.php`, `osczr.php`)
- Konferenz-/Livegame-Integration, Tableaus per JS

### Zugabgabe / ZAT 🔒
- Zugabgabe (Aufstellung des vollen Teams fürs ZAT) `zugabgabe.php`, Beta `zugabgabe_beta.php`, Zusatz `zuzu.php`

### Transfers
- Transfereingabe (eigener Spieler auf Transfermarkt) `transfer.php` 🔒
- Transferstatus `tstatus.php` 🔒
- Schnelltransfer `blitz.php` 🔒 (letzte Schnelltransfers: ✅ `/osneu/lastblitz`)
- Transfermarkt durchsuchen `tm.php` ✅ (POST-Filter)
- Versteigerungsmarkt `viewvm.php` ✅ (POST-Filter)
- Eigenes Spieler auf VM setzen `vmsetzen.php` 🔒
- Eigene Gebote `viewtm.php` 🔒
- Letzte Transfers/Leihen/VM-/TM-Käufe `osneu/lasttrans|lastleih|lastvm|lasttm` ✅
- Transferliste (großes Filterformular) `osneu/transferliste` ✅
- Leihspieler-Eingabe/Übersicht `leihspieler.php`, `viewleih.php` 🔒
- Gebote abgeben: `gebot.php?s=` (TM), `vmgebot.php?s=` (VM), `juscout.php?g=` (Jugend) 🔒

### Finanzen
- Kontoauszug `ka.php` ⚠️ (Demo enthält Tabelle: Saisonstart, Derzeit, Transfers, TM, VM, Bargeld, Steuern, Endergebnis)
- Steuerübersicht `steuer.php` ✅ (Demo)

### Stadion
- Stadionausbau `osneu/stadion` 🔒 (klassische Teaminfo zeigt Stadiondaten ✅)

### Nachrichten & Community
- PM (OS Mail) `osneu/pm` (Posteingang/Ausgang/Schreiben mit WYSIWYG) 🔒
- Forum (WoltLab, eigener Login, komplett geschlossen für Gäste)
- EM-Tippspiel `emtipp.php` 🔒
- Chat (extern: os20.mainchat.net)
- Shop (PrestaShop) – derzeit 503 (Offline)
- Bugtracker (MantisBT) – derzeit HTTP 500

### Suche & Statistiken ✅
- Spielersuche (kombinierte Kriterien, JSON) + Spielervergleich (Balkendiagramme, JSON/HTML)
- Topteams, Topspieler, Topscorer, Fairplaytabelle
- Spiel-/Tabellenstatistiken (Gesamtwerte wie Geldumlauf, Spielerzahlen, Stadien, Trainer, Transfermärkte)
- Team-/Managersuche und -liste, Freie Teams, Freie Zweitteams, Gesperrte Teams, Passwort verloren

### Optionen / Einstellungen
- Erweiterte Einstellungen (Altersanzeige, Teamperformance) `einstellungen.php` ✅
- Benachrichtigungen `osneu/benachrichtigungen` 🔒
- Passwort ändern `osneu/passwort` 🔒
- Zusatzfeatures `zfeatures.php` 🔒, Support `support.php` 🔒

### Konto / Organisation
- Registrierung (Anmeldung) – Formular `name`,`email`,`pass`,`register`, manuelle Freischaltung ✅
- Prüfung freie Teams, Bewerbung (nur mit Session), Zweitteam übernehmen 🔒
- Abmeldung vom Spiel **nur per E-Mail** (die Seite `abmelden.php` ist ein Hinweis-Stub, keine Funktion!)

### Wiki / Hilfe
- MediaWiki mit kompletten Spielregeln (0–VI), OS-Hilfe, Tutorials (YouTube) – öffentlich lesbar ✅
- Kontexthilfe (drehende Fragezeichen) verlinkt auf `os-hilfe/*.html` bzw. das Wiki

---

## B – Seitenstruktur / Navigationsbaum (komplett)

Quelle: reales Menü (Menü-Frame + statische Kopie `os_menu_haupt.html`). Grundstruktur ist ein klassisches Frameset: `wappen.php` (Kopf) · `menue.php` (Navigation, JS) · Hauptseite (Standard: `haupt.php`).

```
Online Soccer 2.0
├── Managerbüro Home                     → haupt.php
├── Mannschaften (öffentlicher Kaderbrowser)
├── Spielregeln
│   ├── 0. Allgemeines │ I. Manager │ II. Wettbewerbe │ III. Mannschaft │
│   │   IV. Spieler │ V. Peripherie │ VI. Finanzen   → wiki/...
│   ├── OS Hilfe                          → wiki/Kategorie:OS_Hilfe
│   ├── Saisonplan                        → osneu/saisonplan
│   └── Tutorials                         → tutorial.html
├── News
│   ├── Letzte Transfers                  → osneu/lasttrans
│   ├── Letzte Leihen                     → osneu/lastleih
│   ├── Letzte VM Käufe                   → osneu/lastvm
│   ├── Letzte TM Käufe                   → osneu/lasttm
│   └── Letzte Schnelltransfers           → osneu/lastblitz
├── Anmeldung/Bewerbung
│   ├── Anmeldung                         → os_anmeldung.html
│   ├── Abmeldung                         → abmelden.php (nur Info: per Mail)
│   ├── Passwort verloren?                → osneu/lostpw
│   ├── Bewerbung                         → bewerbung.php
│   ├── Freie Teams                       → osneu/freieteams
│   ├── Freie Zweitteams                  → osneu/fzt
│   ├── Gesperrte Teams                   → osneu/gesperrteteams
│   └── Zweitteam übernehmen              → zweitteam.php
├── Optionen/Internes
│   ├── Benachrichtigungen                → osneu/benachrichtigungen
│   ├── Passwort ändern                   → osneu/passwort
│   ├── Support                           → support.php
│   ├── Team/Managerliste                 → osneu/managerliste
│   ├── Team/Managersuche                 → osneu/managersuche
│   └── Zusatzfeatures                    → zfeatures.php
├── Team
│   ├── Mannschaft                        → showteam.php (Tabs s=0..10)
│   ├── Jugendteam                        → ju.php
│   ├── Jugendscouting                    → juscout.php
│   ├── Stadionausbau                     → osneu/stadion
│   ├── Taktik-Editor                     → taktiken.php
│   ├── Team-Editor                       → te.php (Wappen + Rückennummern)
│   ├── Trainer                           → trainer.php
│   ├── Training                          → training.php
│   ├── Verträge verlängern               → vt.php
│   ├── Kontoauszug                       → ka.php
│   ├── Steuerübersicht                   → steuer.php
│   └── Werbeverträge                     → wiki (Finanzen)
├── Freundschaftsspiele
│   ├── Freundschaftsspiele               → friendly.php
│   └── FSS-Turniere                      → fssturnier.php
├── Nationale Bewerbe
│   ├── Ligaspieltage                     → ls.php
│   ├── Ligatabelle                       → lt.php
│   └── Landespokale                      → lp.php
├── Internationale Bewerbe
│   ├── OS-Ranking                        → osneu/osranking
│   ├── Club-Ranking                      → osneu/clubranking
│   ├── Internationale Teilnehmer         → osneu/intTeilnehmer
│   ├── OSC Qualifikation                 → osneu/oscq
│   ├── OS Championscup GP                → osneu/oscgp
│   ├── OS Championscup FR                → osneu/oscfr
│   ├── OSE Qualifikation                 → osneu/oseq
│   ├── OS Europacup GP                   → osneu/osegp
│   ├── OS Europacup FR                   → osneu/osefr
│   └── Supercup                          → osneu/supercup
├── International bis Saison 22
│   ├── OS Championscup HR                → oschr.php
│   └── OS Championscup ZR                → osczr.php
├── Zugabgabe / ZAT
│   ├── Zugabgabe                         → zugabgabe.php
│   ├── Zugabgabe Beta                    → zugabgabe_beta.php
│   ├── Zugabgabe Zusatz                  → zuzu.php
│   ├── ZAT-Ergebnisse                    → zer.php
│   └── ZAT-Report                        → zar.php
├── Transfers
│   ├── Leihspielereingabe                → leihspieler.php
│   ├── Leihspieler Übersicht             → viewleih.php
│   ├── Transfereingabe                   → transfer.php
│   ├── Transferstatus                    → tstatus.php
│   ├── Transferliste                     → osneu/transferliste
│   ├── Schnelltransfer                   → blitz.php
│   ├── Transfermarkt                     → tm.php
│   ├── Versteigerungsmarkt               → viewvm.php
│   ├── Auf den VM setzen                 → vmsetzen.php
│   └── Eigene Gebote                     → viewtm.php
├── Statistiken
│   ├── Topteams                          → osneu/statteam
│   ├── Topspieler                        → osneu/statspieler
│   ├── Fairplaytabelle                   → fpt.php
│   ├── Topscorer                         → topscorer.php
│   ├── Spielersuche                      → osneu/spielersuche
│   ├── Spielervergleich                  → osneu/spielervergleich
│   ├── Spielstatistiken                  → osneu/statistics
│   └── Tabellenstatistiken               → osneu/userstatistics
├── Betafunktionen
│   ├── Infos                             → osbetainfo.php
│   ├── Faces                             → osbetafaces.php
│   ├── Freundschaftsspiele               → osneu/friendlies
│   └── Erweiterte Einstellungen          → einstellungen.php
├── OS Community
│   ├── EM Tippspiel 2024                 → emtipp.php
│   ├── OS Bugtracker                     → /bug (MantisBT)
│   ├── OS Chat                           → extern http://os20.mainchat.net
│   ├── OS Forum                          → /forum (WoltLab)
│   ├── OS Mail                           → osneu/pm
│   └── OS Shop                           → shop/prestashop (offline)
├── Relogin                               → index.php
├── Impressum / Nutzungsbedingungen       → impressum.html
└── Datenschutz                           → datenschutz.html
```

Wichtige kontextuelle Popups (werden von überall per `javascript:`-Handler geöffnet):

```
sp.php?s=<spielerid>            Spielerprofil (öffentlich)
st.php?c=<teamid>               Teamprofil (öffentlich), zusätzlich s=0..10
gebot.php?s=<id>                Gebot auf TM-Spieler        (geschützt)
vmgebot.php?s=<id>              Gebot auf VM-Spieler        (geschützt)
juscout.php?g=<id>              Gebot auf Jugendtalent      (geschützt)
transferview.php?tr=<id>        Transferdetail              (geschützt/session)
tplatz.php?t=<teamid>           Tabellenplätze-Grafik
spielpreview.php?t1&t2&type     Vorbericht
bericht.php?s=<sid>             Spielbericht + Kommentare
rep/saison/<s>/<zat>/<h>-<g>.html  statischer Spielbericht
wiki/..., os-hilfe/<nr>.html    Hilfe
faceprev.php?sid=<id>           Spielergesicht (Bild)
```

---

## C – Technische Kommunikation

### Transport & Header
- **Basis-URL:** `https://os.ongapo.com/`
- **User-Agent-Filter:** Anfragen ohne Browser-User-Agent werden mit **HTTP 403** abgewiesen. Die App muss einen Browser-UA setzen.
- **Cookies:** Beim Seitenabruf wird ein PHP-Session-Cookie gesetzt:
  - Name `os`, Wert ~26 Zeichen alphanumerisch, `Path=/`, **ohne Expires** (Browser-Session-Cookie).
- **HTTP-Verhalten:** `Cache-Control: no-store, no-cache, must-revalidate`. Weiterleitungen: HTTP 302 via `Location`.
- **Zeichensatz:** Klassisch XHTML 1.0 Strict `utf-8`; das Frameset-Shell war früher `iso-8859-1`.

### Request-Arten
| Art | Beispiele |
|---|---|
| GET – Ansichten/Listen | `st.php?c=`, `sp.php?s=`, `ls.php?…`, `lt.php?…`, `lp.php?…`, `zer.php?…`, `oschr.php?…`, `topscorer.php?…`, `tplatz.php` |
| GET – Filter-Formulare (klassisch, method=GET, keine CSRF) | `lt.php?ligaauswahl=1&landauswahl=6&tabauswahl=0&saauswahl=24&stataktion=Statistik+ausgeben` u. ä. |
| POST – Filter/Formulare (self) | `tm.php`/`viewvm.php` (alter/skill/marktwert/position/sortierung), `osneu/lastblitz` (land), `osneu/*` (view) |
| POST – Login | `validate.php` |
| POST – RPC (klassischer AJAX, body `action=…`) | `rpc.php` |
| GET/POST – JSON | `/osneu/ajax/findSpieler`, `osneu/spielersuche?action=suchen`, `livegame/data.php` |
| Statische Dateien | Spielberichte `rep/saison/…`, CSS, JS, Bilder `images/wappen/{id}.png`, `images/flaggen/{ISO2}.gif` |

### RPC-Endpunkt `rpc.php` (klassisches AJAX, Prototype: `Ajax.Updater`, body als `postBody`)
Ermittelte Aktionen:
- `saveNotes` (Notizblock speichern)
- `startCalendar&month=&year=` (Kalender anzeigen) – ohne Login leer
- `showEvent&event=`, `deleteEvent&event=`, `insert&month=&year=&day=&text=&titel=&u1=&u2=&to=` (Termine; `to` = Empfänger-IDs `;`-separiert)
- `evus` (Autocomplete Termin-Empfänger) – öffentlich (Textliste `Name (id)Team, …`)
- `sidb` (Autocomplete Spielersuche)
- `addPlayer&sid=&snote=` / `changePlayer&sidc=&snote=` / `deletePlayer&sidc=` (Spielerbeobachtung)

### JSON-/AJAX-Endpunkte (neu)
- **Autocomplete Spieler:** `GET /osneu/ajax/findSpieler?term=<text>` → `[{id, name, zusatz}]` (JSON, öffentlich).
- **Spielersuche:** `GET /osneu/spielersuche?action=suchen` mit JSON-Body `{landId, ligaId, anzeigeId, nationId, kriterien:[{attributId, opVon, valVon, opBis, valBis, sort}]}` → `{html}` (erreichbar ohne Login; Kriterien-Katalog serverseitig in `osneu/php/data.inc.php`, 37 Attribute).
- **Spielervergleich:** `GET /osneu/spielervergleich?action=getSpieler&ansicht=0|1&id1=&id2=` → HTML; Teammodul lädt `teamgraph.php`-Bilder (Leistungskurven).
- **Livegame:** `livegame/data.php?action=gamedata&teamid=<id>&zat=<zat>` → JSON mit `game`, `squad` (Aufstellungen), `tactics` (commitment/hardness/playtype/defence/midfield/offence), `gamestatistics`, `actions[]` (Minuten-/Event-Queue inkl. Tore/Assists/Gelb/Rot/Auswechslungen/Break/Sound), `reporturl`. Der genaue Pfad ist aktuell ohne Login nicht verifizierbar (Session-abhängig).
- **Konferenz:** aus Listenseiten → `location.href="livegame/?spiele=<team,zat>-Kette"`.
- **Eigene Team-Erkennung im Livegame:** `$.get('<teamIdUrl>')` und Regex `tabellenplatz[(]([0-9]+)[)]`.

### Zugriffsschutz / Fehlerbehandlung (wichtig für die App!)
Die Seite signalisiert fehlenden Zugriff **nicht per HTTP-Status** (außer User-Agent-403), sondern **im Seitentext**:
- Klassisch: `„Diese Seite ist ohne Team nicht verfügbar!“`, `„Ohne Team gesperrt!“`, `„Du musst angemeldet sein um mitspielen zu können!“`, `„Du hat keine Rechte hier zuzugreifen!“`, `„Du kannst dich als Gast nicht bewerben!“`
- `/osneu/`: geschützte Seiten liefern eine **„404 – not found“-Attrappe** (Loginfalle).
→ Die App muss diese Texte erkennen und als „Login erforderlich“ behandeln.

### Extern (separate Systeme, eigene Logins)
- **Forum:** WoltLab Suite 6.2.7 unter `/forum/`; Login `POST /forum/login/` mit `username`, `password`, CSRF-Token `t`. Komplett für Gäste gesperrt.
- **Wiki:** MediaWiki 1.39 unter `/w/`; Standard-Login (`Special:UserLogin`).
- **Bugtracker:** MantisBT unter `/bug/` (aktuell HTTP 500).
- **Shop:** PrestaShop unter `/shop/prestashop` (aktuell 503 im Wartungsmodus → **offline**).
- **Chat:** externer Dienst (mainchat).

---

## D – Authentifizierung

### Login-Ablauf
1. `GET /` liefert die Loginseite (nur Formular, kein Captcha, kein Token).
2. `POST https://os.ongapo.com/validate.php` mit:
   - `action=os_login` (versteckt)
   - `loginemail` (max. 100 Zeichen)
   - `passwort` (max. 100 Zeichen)
   - Zwei Bild-Buttons: `imageField` („Einloggen“), `imageField2` („Gast“) – beide senden dasselbe Formular; der Gast-Button erzeugt einen Gastzugang (Basis für Bewerbungen).
3. **Erfolg:** Server setzt Session-Cookie `os=<sid>` und leitet weiter (sehr wahrscheinlich in den Spielbereich, Richtung `haupt.php`/`os_menu_*`). *Nicht mit echten Daten getestet (Bewusstsein der Grenzen).*
4. **Fehlschlag:** HTTP 302 → `os_menu_haupt.html` (Loginmenü wieder). Fehlermeldung wird dort gerendert.

### Session
- **Cookie `os`** = PHP-Session-ID, `Path=/`, **kein `Expires`** → gültig bis inaktiv oder Session-Distanz des Servers abläuft.
- Historisch gab es eine konfigurierbare **Session-Dauer** (`sdauer`: 30 Min … 1 Jahr) im Loginformular – derzeit **auskommentiert/deaktiviert**. Die tatsächliche Gültigkeitsdauer ist ohne Login nicht messbar; das muss im späteren Betrieb getestet werden.
- „Remember me“-Checkbox existiert **nicht** mehr (nur noch die Bild-Buttons + deaktivierter Session-Dauer-Selektor).
- **Logout/Relogin:** Menüpunkt „Relogin“ → `index.php` (Loginseite). Es gibt **keinen** expliziten Logout-Endpunkt, der die Session sicher beendet (Standard-PHP-Session-Verhalten vorausgesetzt: Server-Seite invalidiert die Session bei `session_destroy` im Logout-Flow). Der frühere Abmelden-Punkt (`abmelden.php`) ist nur ein Hinweis „Abmeldung per Mail“.
- **Ohne Login** zeigen die meisten Seiten Demo-Daten („DemoTeam“) – ein zuverlässiges Erkennungsmerkmal „noch nicht eingeloggt“.

### Andere Logins (nicht SSO)
Forum, Wiki, Bugtracker, Shop haben **eigene** Konten/Session-Cookies und müssen ggf. separat eingeloggt werden. Für die App zunächst irrelevant (bis auf Forum evtl. später).

### Speicherung in der App (Anforderung)
Siehe Abschnitt I: Credentials und Session nur im Android Keystore / EncryptedSharedPreferences ablegen, nie in Quellcode/Logs/Git.

---

## E – Datenmodell (erkannte Strukturen)

### Kernentitäten
- **Manager/User:** E-Mail (Login), In-Game-Name, Rechte, verknüpfte(s) Team(s), Gaststatus.
- **Team:** ID `c=`, Name, Wappen (`images/wappen/{id}.png`, Fallback `00000000.png`), Land/Flagge, Liga (1–7), Stadion (Name, Kapazität, Sitz/Steh, überdacht, Rasenheizung, Anzeigetafel), Marktwert-Summe, Gehalt-Summe, Fairplay-Werte, Tabellenplatz, Saisonhistorie, Transfer-/Leihhistorie.
- **Spieler:** ID `s=`, Name, Alter, „Geburtstag“ (in ZAT-Einheiten), Nationalität (ISO2, Flagge `images/flaggen/XX.gif`), Position (TOR/ABW/DMI/MIT/OMI/STU), Rückennummer, Monatsgehalt, Vertragslaufzeit (Monate), Marktwert, Skillschnitt, Opt.Skill, 18 Skills (+ 6 Torwahr-Skills bei TW), Moral, Fitness, Sperre (z. B. „3P“, „1I“), Verletzt, Leihstatus, Kapitäns-/Sonderstatus (K/G), Saison-/Karrierestatistik (Spiele/Tore/Vorlagen/Score/Gelb/Rot nach LI/LP/IP/FS), Transfer-/Leih-/Spielerhistorie, Gesicht (`faceprev.php?sid=`).
  - Skill-Kürzel: SCH (Schießen), BAK (Bälleroberung), KOB (Körperkontakt), ZWK (Zweikampf), DEC (Defensive), GES (Geschwindigkeit), FUQ (Führungsqualität), ERF (Erfahrung), AGG (Aggressivität), PAS (Passen), AUS (Ausdauer), UEB (Übersicht), WID (Willenskraft), SEL (Selbstvertrauen), DIS (Disziplin), ZUV (Zuverlässigkeit), EIN (Einschätzung); TW: Abstoss, Stellungsspiel, Fangsicherheit, Strafraumbeh., Spiel auf der Linie, Reflexe; hinzu Führungsfertigkeit/Einstellung je nach Ansicht.
- **Positionen:** 1=TOR, 2=ABW, 3=DMI, 4=MIT, 5=OMI, 6=STU (zahlreiche Filter nutzen diese Codes).

### Wettbewerbe / Spielsystem
- **ZAT:** 1–72 je Saison (Saisonplan: Liga 1/2/3*, Pokalrunden, Friendly-Zen, OSE/OSC-Quali und Gruppenphase fest verdrahtet).
- **Saison:** 1–24 (aktuell 24). 10er-/18er-/20er-Ligen.
- **Ligen:** `ligaauswahl` 0–7 (1. Liga, 2. Liga A/B, 3. Liga A–D), pro Land (105 Länder, `landauswahl`).
- **Landespokal** (`lp.php`), **OSC** (Championscup: Quali/GP/FR; alt HR/ZR), **OSE** (Europacup), **Supercup**, OS-/Club-Ranking (Faktoren Liga 1.0, Pokal 1.2, OSCQ 1.3, OSEQ 1.2, OSE 1.4, OSC 1.5 + Titelbonus).

### Transfers
- Typen: Transfermarkt (TM), Versteigerungsmarkt (VM), Schnelltransfer (Blitz), Leihe, Tausch. Zustände offenbar `tstatus` 1/2 (A/T). Detail-Bitmaske in Transferliste: Kauf=1, Tausch=2, Leihe=4.

### Finanzen
- Kontoauszug-Posten: Transfers, Transfermarkt, Versteigerungsmarkt, Bargeld, Steuern; Kennzahlen Saisonstart/Derzeit/Zwischensumme/Endergebnis.

### Kommunikation
- PM (Posteingang/Ausgang, ungelesen, WYSIWYG sceditor), Benachrichtigungen, Kalender-Events, Notizblock, Spielerbeobachtung (Notiz + Team + Skill/Opti/MW/Aktion).

### Codes/Enumerations (aus den öffentlichen Formularen)
- Teampositionen (0–6), Torwartkategorien (27–32 in Suche), Transferstatus, Transferdetails-Bitmaske, Startaufstellung-Slots, „type“ im `spielpreview` (2=Liga, 3=Pokal, 7=International), Statistikarten Topscorer (1–10), Team-/Spielerstatistik-IDs (1–16), Anzeigemodi (Top10/25/50/100, Low…), Sortier-Codes (Tens- und Unsigits bei TM/Transferliste: z. B. 11/12 Alter, 21/22 Dauer, 31/32 MW, 41/42 Opti, 51/52 Position, 61/62 Skill, 71/72 Ablöse, 81/82 MW).

---

## F – Aktionen (Befehle mit Serverwirkung)

Allgemeines Muster: einfache GET- oder POST-Formulare ohne CSRF-Token. Ändernde Aktionen sind fast alle an den „ohne Team“-Schutz gekoppelt. Nachfolgend die bekannten **Eingaben/Parameter** und wie **Erfolg/Fehler erkennbar** wäre.

| Aktion | Eingabe/Parameter | Endpunkt | Erfolg / Fehler |
|---|---|---|---|
| Login | `action=os_login`, `loginemail`, `passwort` | POST `validate.php` | 302 ins Spiel ; Fehler: 302 zurück zur Loginseite + Meldung |
| Registrierung | `name`, `email`, `pass`, `register` | POST (Selbst) | Hinweisseite; per E-Mail bestätigen (24 h) |
| Passwort zurücksetzen | `team`, `mail` | POST `osneu/lostpw` | Meldung/Bestätigung |
| Passwort ändern | (Formularfelder unbekannt, geschützt) | `osneu/passwort` | Meldung |
| Rückennummern speichern | `pnr` (Spieler-ID-Liste `;`-separiert in Reihenfolge) | POST `te.php` | Meldung; Seite neu laden |
| Einstellungen speichern | `OPTION1` (1/2), `OPTION2` (1/2), `speichern` | POST `einstellungen.php` | Meldung; Einstellung wirkt |
| Notizblock | `action=saveNotes&notes=` | POST `rpc.php` | AJAX-Update `#notemsg` |
| Kalender-Eintrag | `action=insert&month&year&day&text&titel&u1&u2&to` | POST `rpc.php` | AJAX-Refresh Kalender |
| Event löschen | `action=deleteEvent&event=` | POST `rpc.php` | AJAX-Refresh |
| Spieler beobachten | `action=addPlayer&sid&snote` / `changePlayer` / `deletePlayer` | POST `rpc.php` | AJAX-Update Liste |
| Aufstellung (ZAT) | (Formularfelder unklar, geschützt) | POST `zugabgabe.php`, `zugabgabe_beta.php`, `zuzu.php` | Meldung; probabilistisch durch erneut Laden bestätigen |
| Taktik | (unklar, geschützt) | `taktiken.php` | Meldung |
| Training | (unklar, geschützt) | `training.php` | Meldung |
| Trainer | (unklar, geschützt) | `trainer.php` | Meldung |
| Verträge verlängern | (unklar, geschützt) | `vt.php` | Meldung |
| Transfereingabe | (unklar, geschützt) | `transfer.php` | Meldung |
| Schnelltransfer | (unklar, geschützt) | `blitz.php` | Meldung |
| Auf VM setzen | (unklar, geschützt) | `vmsetzen.php` | Meldung |
| Gebot TM | (Feldnamen unklar, geschützt) | `gebot.php?s=` | Meldung; Gebot unter VM/status |
| Gebot VM | (Feldnamen unklar, geschützt) | `vmgebot.php?s=` | Meldung |
| Gebot Jugend | (Feldnamen unklar, geschützt) | `juscout.php?g=` | Meldung |
| Leihspieler | (unklar, geschützt) | `leihspieler.php` | Meldung |
| Stadionausbau | (unklar, geschützt) | `osneu/stadion` | Meldung; direkt in Teaminfo sichtbar |
| Freundschaftsspiele | (unklar, geschützt) | `friendly.php`, `osneu/friendlies` | Meldung |
| PM schreiben | `action=writeNew`, `receiver_id`, Inhalt (WYSIWYG) | `osneu/pm` | Liste/POSTEINGANG prüfen |
| PM löschen | (unklar) | `osneu/pm` | Liste prüfen |
| EM-Tipp abgeben | (unklar, geschützt) | `emtipp.php` | Meldung |

**Erfolgserkennung allgemein:** Seitenmeldungen (grüne `.success`-Boxen im /osneu, rote `.error`-Boxen), Fehlertexte (Info-/Warning/-Validation-Boxen), Zurückrendern der geänderten Liste, HTTP-Umleitungen. Fehler: Meldungsbox mit Farbklasse, „Falsche Eingabe“-Hinweise, bei abgelaufener Session die oben genannten Guard-Texte bzw. Fake-„404“.
**Hinweis:** Die genauen Feldnamen der geschützten „Schreib“-Formulare **konnten ohne Login nicht ermittelt werden** → müssen nach dem ersten Login (mit eigenen Daten) im Betriebsmodus dokumentiert werden (Klassifikation D).

### Auswertung der öffentlich sichtbaren Filterformulare (Anzeigen-Funktionen, auch so nativ umsetzbar)
- **Transfermarkt/VM:** POST `tm.php|viewvm.php` mit `alter(0–9)`, `skill(0–6)`, `marktwert(0–8)`, `position(0–6)`, `sortierung` (oder GET als URL). 
- **Transferliste:** GET/POST `osneu/transferliste` mit `alter, skill, opti, abloese, position, tstatus, tdetail(Bitmaske), sortierung, tinfo, proSeite(25/50/75/100)`, versteckt `gesucht=1`.
- **Ligatabelle:** GET `lt.php?ligaauswahl&landauswahl&tabauswahl(0-7,10)&saauswahl&stataktion`.
- **Spieltage/Pokal/ZAT-Ergebnisse:** GET `ls.php|lp.php|zer.php?…&erganzeigen=1` + `livegame`-Checkboxen (`name=live value=<heimId>,<zat>`).
- **Topscorer:** GET `topscorer.php?landauswahl&ligaauswahl&statistik(1-10)&pos(0-6)&saison&art(0-4)`.
- **Team-/Spielerstatistik:** GET `osneu/statteam` (`comboLand, comboLiga, comboStatistik, comboAnzeige`), `osneu/statspieler` (`comboLand, comboLiga, comboStatistik, comboPosition, comboAnzeige`).

---

## G – Android-Umsetzung (Klassifikation je Bereich)

Klassifikation: **A** nativ (statische/klare Daten) · **B** über HTTP/API (inkl. HTML-Parsing) · **C** WebView (JS-/Rendering-lastig) · **D** unklar → im Betrieb testen.

| Bereich | Datenquelle | Klassifikation | Begründung |
|---|---|---|---|
| Login | POST validate.php | **A/B** | Klarer Formular-POST, Session-Cookie |
| Büro/Dashboard | haupt.php (HTML) | **B** | Parse Tabelle „Logins/Konto/PMs/FSS/NMR“, ZAT-Hinweis |
| Kalender | rpc.php + HTML | **B** | Events per rpc.php setzen/löschen, Kalender nativ bauen |
| Spielerbeobachtung/Notizblock | rpc.php + HTML | **B** | klein, einfach zu parsen |
| Teamübersicht/Kader | st.php?s=0 (sortable HTML-Tabelle) | **B** | sehr gut parsebar (Klassen TOR/ABW…) |
| Vertragsdaten | st.php?s=1 | **B** | |
| Einzelwerte (18 Skills) | st.php?s=2 | **B** | |
| Spielerstatistiken | st.php?s=3/4 (+ sp.php) | **B** | |
| Teaminfo (Stadion/Finanz) | st.php?s=5 | **B** | |
| Saisonplan | st.php?s=6 / osneu/saisonplan | **B** | osneu-Tabelle besonders sauber |
| Team-/Vereins-/Transfer-/Leih-/Saisonhistorie | st.php?s=7–10 | **B** | |
| Spielerprofil | sp.php?s= (HTML) | **B** | klar strukturiert, Face-Bild separat laden |
| Tabellenplätze-Grafik | tabellenplatz.php (Bild) | **B/C** | Bild einbinden; alternativ eigener Chart |
| Jugendteam/-scouting | ju.php, juscout.php | **D** | geschützt, nach Login prüfen |
| Trainer | trainer.php | **D** | geschützt |
| Training | training.php | **D** | geschützt |
| Taktik-Editor | taktiken.php | **C/D** | wahrscheinlich JS-lastig (Aufstellung) |
| Team-Editor Rückennummern | te.php (POST pnr) | **B** | Reihenfolge → `pnr`-Liste; Drag&Drop nativ |
| Wappen-Upload | te.php | **C** (aktuell deaktiviert) | |
| Verträge verlängern | vt.php | **D** | geschützt |
| Kontoauszug | ka.php | **B** | Tabelle gut parsebar |
| Steuer | steuer.php | **B** | |
| Stadionausbau | osneu/stadion | **D** | geschützt; Formularfelder nach Login |
| Zugabgabe (Aufstellung) | zugabgabe*.php | **C/D** | evtl. zentraler Spielbereich; nach Login testen, sonst WebView |
| Ligaspieltage | ls.php | **B** | |
| Ligatabelle (inkl. Kreuztabelle) | lt.php | **B** | |
| Landespokale | lp.php | **B** | |
| OSC/OSE/Supercup (Listen/Tableaus) | osneu/oscq…supercup, oschr/osc zr | **B/C** | Tableaus JS-gerendert (`#international`) → Ende Punkte: nativ, falls JSON/HTML abrufbar, sonst WebView |
| OS-/Club-Ranking | osneu/osranking, clubranking | **B** | enthält `.info-wrapper` Hover – nativ als Card |
| ZAT-Ergebnisse | zer.php | **B** | |
| ZAT-Report | zar.php | **D** | geschützt |
| Transfermarkt | tm.php | **B** | |
| Versteigerungsmarkt | viewvm.php | **B** | |
| Transferliste (Filter) | osneu/transferliste | **B** | viele Filter → sauberes natives UI |
| Gebote (TM/VM/Jugend) | gebot/vmgebot/juscout | **D** | nach Login Feldnamen prüfen |
| Eigenes Verkaufen/Blitz/VM/Leihe | transfer/blitz/vmsetzen/leihspieler + Status | **D** | geschützt; nach Login |
| PM | osneu/pm | **B/C** | Posteingang nativ; WYSIWYG-Editor (sceditor) → WebView im Schreib-Editor |
| Benachrichtigungen | osneu/benachrichtigungen | **B/D** | geschützt, nach Login |
| Spielersuche | osneu/spielersuche (JSON) | **B** | sehr gut nativ umsetzbar |
| Spielervergleich | osneu/spielervergleich (JSON/HTML + Balken) | **B** | |
| Topteams/Topspieler/Topscorer/Fairplay | statteam/statspieler/topscorer/fpt | **B** | |
| Spiel-/Tabellenstatistiken | osneu/statistics, userstatistics | **B** | |
| Einstellungen | einstellungen.php, osneu/passwort | **A/B** | |
| Livegame/Konferenz | livegame/ + data.php JSON | **C** | animierte Playback-UI – mit WebView am zuverlässigsten; nativ nur als schlanker Viewer mit JSON möglich |
| Berichte/Kommentare | rep/…html, bericht.php | **B** | statische HTML-Berichte parsebar; Kommentarabgabe später |
| Forum | /forum (WoltLab) | **C** | eigener Login, JS-lastig; Apps → WebView |
| Wiki | /wiki (MediaWiki) | **B/C** | MediaWiki-API vorhanden → nativ möglich, aber selten nötig |
| Chat | extern | **C** | WebView |
| Shop | PrestaShop | – | derzeit offline |
| EM-Tippspiel | emtipp.php | **D** | geschützt |

**Erwartung:** Großer Teil (≈70–80 % der Anzeige-Funktionen) ist sauber nativ umsetzbar (**B**), weil die HTML-Tabellen sehr regulär sind (festes Tabellen-/Klassen-Schema, sortable-Tabellen). JS-heavy Module (Livegame, Taktik-Editor, internationale Tableaus, WYSIWYG-PM, Forum) → **WebView** (C). Einige Kernformulare (Zugabgabe u. a.) erst nach Login im Betrieb zu klären (**D**).

---

## H – Risiken / technische Probleme

1. **Keine offizielle API, kein JSON für den Großteil.** Alles abhängig von HTML-Scraping. HTML ist regulär, kann sich aber bei jedem Server-Update ändern → Parser-Kopplung.
2. **Textbasierte Zugriffssperren statt HTTP-Status.** „ohne Team nicht verfügbar“ / „Ohne Team gesperrt!“ / „Du musst angemeldet sein…“ / Fake-„404“ im /osneu müssen zuverlässig erkannt werden (Session-Expiry-Erkennung).
3. **User-Agent-Filter (403).** App muss stets vertrauenswürdigen Browser-UA senden; sonst Blocker.
4. **Session-Lebensdauer unbekannt.** Früher wählbar (30 Min–1 Jahr), jetzt kein UI. Automatisches Re-Login nötig; parallele Nutzung am PC kann Session überschreiben (single-session?) → Verhalten im Betrieb testen.
5. **Login „Remember me“ fehlt.** Komfort-Login muss die App selbst lösen (Keystore + Auto-Login), da `os`-Cookie ohne Expires ist.
6. **Kein expliziter Logout-Endpunkt** (nur „Relogin“). Sicheres Abmelden = Session-Reset serverseitig unklar; App sollte Session local verwerfen.
7. **Geschützte Seiten ohne Login nicht einsehbar** → Feldnamen der Schreib-Formulare (Zugabgabe, Training, Transfer, Stadion, PM-Delete usw.) erst nach ersten eigenen Anmeldungen dokumentierbar (Klassifikation D).
8. **Livegame-Datenpfad** (`data.php`) ist ohne Login nicht verifizierbar und könnte session-spezifisch oder an anderem Pfad liegen → testen.
9. **Internationale Tableaus** werden per JS befüllt (`#international`), JSON-Format nicht öffentlich → ggf. WebView nötig.
10. **Forum/Wiki/Shop** haben eigene Konten; kein SSO. Forum-Login (WoltLab) hat CSRF-Token `t`, das erst geholt werden muss.
11. **Livegame/Tableau-Sound** (wav/mp3) nur interaktiv verfügbar.
12. **Statische Berichte** liegen als Dateien unter `rep/saison/…` – Abruf ok, aber Pfade ändern sich mit Saison/ZAT.
13. **Sprach-/Formatfixierung (deutsch):** Werteformate „6.216.506 Euro“, Dezimalkomma, Abkürzungen (LI/LP/IP/FS, 3P/1I). Parser robust auf Zahlen + Text auslegen.
14. **URL-Codierung/Html-Entitäten** (Umlaute, &auml; etc.) je nach Seite unterschiedlich → Normierung nötig.
15. **Server-Stabilität:** 403-Filter, gelegentliche 500 (Bugtracker), 503 (Shop im Wartungsmodus) – auf Fehler- und Wartungsfälle vorbereiten.
16. **Zuverlässigkeitsgrenzen:** Spielaktionen ohne Bestätigung/Token; Fehler können nur über Meldungsbox/-text erkannt werden. Kein Rollback, keine IDs.
17. **Recht/Nutzungsbedingungen:** Der Betrieb des eigenen Clients ist mit dem privaten Account erlaubter Anwendungsfall, aber Scraping-/Aktivitätsregeln („nicht automatisiert?“) des Betreibers unbekannt – vor Live-Betrieb (v. a. mit vielen Anfragen) die AGB/Spielregeln prüfen. Keine automatisierten Massenaktionen.

---

## I – Empfehlung: Android-Architektur

### Grundsatz
Hybrider Ansatz, **„nativ-first, WebView wo nötig“**, mit einem zentralen **HTML-Quelle-Repository-Layer**. Der Server bleibt einzige Datenquelle; **es wird nichts nachprogrammiert, keine eigene Datenbank fürs Spiel** (nur lokale Cache-Struktur).

### Technologie-Stack (Vorschlag)
- **Sprache/UI:** Kotlin, Jetpack Compose, Material 3, Single-Activity, Navigation-Compose, Bottom-Navigation (Home · Team · Spiele · Transfers · Mehr), dark/light Theme nach Material (OS-Look: Dunkelblau/Gelb im Dark Mode).
- **Netzwerk:** OkHttp (mit persistenter CookieJar `os`-Cookie), HttpURLConnection nur als Fallback. **Browser-User-Agent** global setzen. Timeouts + Retry.
- **Parsing:** Jsoup (HTML) + kotlinx.serialization / Moshi (JSON-Endpunkte) + Coil (Bilder: Wappen, Flaggen, Gesichter, Charts).
- **Trennung:** 
  - `dto/` Modelle (Team, Spieler, Spiel, Transfer, Stadion, Wettbewerbs-Stand, PM…)
  - `parser/` pro Seite/Modul (eindeutig gekapselt, austauschbar)
  - `repo/` + `ApiClient` (Session, Login, Requests)
  - `ui/` Composable-Screens
- **Login & Sicherheit:**
  - Zugangsdaten NIE im Code/Git/Logs. Speicherung nur über **Android Keystore** (z. B. AndroidX `EncryptedSharedPreferences` mit AES/GCM; ggf. Biometric-Prompt).
  - Session-`os`-Cookie im CookieJar persistieren (private Dateien); Auto-Login bei „Session weg“-Erkennung.
  - Servertext-Wächter: Guard-Texte/Fake-404 erkennen → Umleitung zum Login-Screen (ohne Datenverlust).
- **Screens (nativ, priorisiert):**
  1. Login
  2. Dashboard (Büro) + Kalender + Beobachtung + Notizblock
  3. Mannschaft: Kader-Liste (Filter nach Position), Spielerprofil (Detail + Skills + Historie), Vertragsdaten, Einzelwerte, Statistiken
  4. Spiele: Spielplan/Fixtures, Tabellen (mit Farbcodierung), Pokal/Int.-Wettbewerbe, ZAT-Spiele, Livegame (WebView)
  5. Transfers: Transferliste/TM/VM mit Filtern, Eigene Gebote, News (letzte Transfers), Gebote (Formular)
  6. Jugend, Training/Taktik (nächste Phase, erst nach Login-Analyse; Taktik-Editor ggf. WebView), Stadion, Finanzen, Einstellungen, PM (WebView-Editor), Suche (Playercards), Ranking/Statistiken
- **WebView-Strategie:** Eine gemeinsame WebView-Anbindung, die CookieStore + User-Agent des OkHttp-Clients teilt (gleiche `os`-Session), verwendet für Livegame, Taktik-Editor, Forum, WYSIWYG-PM-Editor, evtl. internationale Tableaus. Mit Fortschritts-Overlay und „im App-Frame“-Erscheinungsbild.
- **Architektur:** MVVM + Repository; StateFlow/UiState; Offline-Cache (Room) optional für kleine Tabellen (Mannschaft, Tabelle) – Server bleibt Quelle der Wahrheit; kein kompletter Server-Spiegel.
- **Betriebsmodus Phase 2:** Nach Abschluss der Analyse und mit eigenen Zugangsdaten die geschützten Formulare (Feldnamen) dokumentieren, D-Bereiche in A/B/C auflösen.
- **Erste Implementierungsreihenfolge (nach Freigabe):** Session/Auth-Client → Dashboard → Mannschaft/Kader → Spielerprofil → Tabellen/Spielplan → Transfers (Anzeige) → Suche → Stufenweise Schreib-Aktionen → Livegame/WebView.

---

## Anhang – kurze Liste konkret verifizierter technischer Fakten
- Login-POST: `validate.php`, Felder `action=os_login|loginemail|passwort`; Fehler → 302 nach `os_menu_haupt.html`; Session-Cookie `os` (PHP-Session, `Path=/`, kein Expires).
- Ohne Browser-UA → HTTP 403.
- Marker für eingeloggten Zustand ohne eigene Daten: DemoTeam-Seiten vs. Guard-Texte vs. Fake-404.
- JSON verfügbar: `/osneu/ajax/findSpieler?term=` → `[{id,name,zusatz}]` (öffentlich).
- Autocomplete `rpc.php?action=evus` → Textliste `Name (id)Team,…`.
- Livegame-RPC: `data.php?action=gamedata&teamid&zat` (JSON, session-abhängig); Konferenz-URL `livegame/?spiele=…`.
- Statische Berichte: `rep/saison/{saison}/{zat}/{heim}-{gast}.html`.
- Positions-Codes: 1=TOR … 6=STU; Spieltyp-Codes: 2=Liga, 3=Pokal, 7=International.
- Aktuelle Saison 24; ZAT-Zyklus 1–72.
- Forum = WoltLab 6.2.7, Wiki = MediaWiki 1.39, Bug = MantisBT (500), Shop = PrestaShop (503).