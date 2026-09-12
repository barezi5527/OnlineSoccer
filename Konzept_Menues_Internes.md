# Konzept: „Anmeldung/Bewerbung" und „Optionen/Internes" in der OS-App

Stand: 09.09.2026 · Entwurf zur Freigabe – noch **keine** Umsetzung.
Basis: Live-Check der Website (09.09.2026) + Architektur-Review + UX/Security-Review durch
mehrere Agents; Korrekturen eingearbeitet.

---

## 1. Ausgangslage & Ziel

Die Website (os.ongapo.com) hat in ihrem Hauptmenü (`os_menu_haupt.html`, live verifiziert)
zwei Bereiche, die in der App noch fehlen:

1. **Anmeldung/Bewerbung** – 8 Einträge.
2. **Optionen/Internes** – 6 aktive Einträge (changelog.php + banner.php sind im Quelltext
   auskommentiert, gehören NICHT dazu).

Ziel: Diese Menüs samt Unterpunkten **lesend** in die App überführen – auffindbar, ohne die
Navigation zu überladen. Schreib-/Formularaktionen werden **nicht** im App-Code umgesetzt.

---

## 2. Die Menüs der Website (verifiziert)

Legende: **P**=öffentlich, **S**=benötigt Session/Team (App-Session vorhanden), **F**=Formular,
**L**=Listentabelle. Hinweis: „Gast-Ansicht" = offene/beobachtende Seite im Browser ohne Login.

### 2.1 Anmeldung/Bewerbung

| Unterpunkt | URL | Typ | App-Vorschlag |
|---|---|---|---|
| Anmeldung | os_anmeldung.html | P/F | Info-Zeile (Registrierung gehört zum LoginScreen, kein Server-Parsing) |
| Abmeldung | abmelden.php | P | Info-Zeile „Abmeldung nur per E-Mail an das OS-Team" (keine Funktion) |
| Passwort verloren? | osneu/lostpw | P/F | Reader; später Direktlink am LoginScreen. **Achtung:** riesiges öffentliches Team-`<select>` – nicht roh rendern |
| Bewerbung | bewerbung.php | S/F | Hinweis-Zeile „Als Gast nicht bewerben!" (live verifiziert) |
| Freie Teams | osneu/freieteams | P/L | **Native Liste mit Land-Filter** (Zeilen erst nach Auswahl) |
| Freie Zweitteams | osneu/fzt | P/L | Reader (kleine Liste) |
| Gesperrte Teams | osneu/gesperrteteams | P/L | Reader (Liste aktuell leer) |
| Zweitteam übernehmen | zweitteam.php | S/F | Hinweis-Zeile „Ohne Team nicht verfügbar!" (live verifiziert) |

### 2.2 Optionen/Internes

| Unterpunkt | URL | Typ | App-Vorschlag |
|---|---|---|---|
| Benachrichtigungen | osneu/benachrichtigungen | S¹ | „Im Browser öffnen" (Gast sieht nur eine 404-Stubseite) |
| Passwort ändern | osneu/passwort | S¹ | „Im Browser öffnen" |
| Support | support.php | **S** (nicht P!) | „Im Browser öffnen" (Gast: „Du hat keine Rechte hier zu zugreifen!") |
| Team/Managerliste | osneu/managerliste | P/L | **Native Liste mit Land/Liga-Filter** |
| Team/Managersuche | osneu/managersuche | P/F | Reader/Info; später native Suche |
| Zusatzfeatures | zfeatures.php | S/F | „Im Browser öffnen" (Gast: „Ohne Team nicht verfügbar!") |

¹ `benachrichtigungen`/`passwort` liefern Gästen technisch HTTP 200, aber nur eine generische
„404 – not found"-Stubseite (kein Formular). Im Reader also **nicht** sinnvoll anzeigen.

---

## 3. Einordnung & Ort in der App

### 3.1 Wo: Dashboard-Karte, nicht neuer Tab

- **Kein neuer Bottom-Tab**: Die Bottom-Leiste hat bereits 5 Tabs
  (`Dashboard | Team | ZAT | Nationale Bewerbe | Intern. Bewerbe`, `AppRoot.kt:315-321`);
  diese Menüs sind Neben-Bereiche statt Kern-Arbeitsfluss.
- **Primär:** Neue Karte auf dem Dashboard „Weitere Online-Soccer-Bereiche"
  (nach der SummaryCard), öffnet einen eigenen Screen. Das Dashboard ist bereits das
  „Managerbüro" und hostet schon eine externe Karte (Forum, `DashboardScreen.kt:214-236`).
- **Optional sekundär:** Ein Leicht-⋮-Eintrag („Weitere Bereiche") in Team-/Bewerbe-Hubs
  (Muster existiert bereits: `TeamScreen.kt:111-124`).
- Warum Reader statt nativ für fast alles: kein neuer Parser nötig, konsistent, klein.

### 3.2 Der Screen: Sektionen nach Aufgabe (nicht 1:1 Website-Spiegel)

`ServerBereicheScreen` (Route `server`) mit **drei** Sektionen – Nutzeraufgaben statt
Website-Überbleibsel (Leser der Website-Menüs finden sich trotzdem zurecht):

1. **Entdecken & Finden** (öffentlich, höchster Wert):
   Freie Teams ▸, Freie Zweitteams ▸, Gesperrte Teams ▸, Team/Managerliste ▸, Team/Managersuche ▸
2. **Konto & Bewerbung**:
   Passwort verloren? ▸, Bewerbung, Zweitteam übernehmen, Anmeldung (Info), Abmeldung (Info)
3. **Einstellungen & Hilfe**:
   Benachrichtigungen, Passwort ändern, Zusatzfeatures, Support

Je Zeile: Label + Icon + Typ-Kennzeichen (z. B. „im Browser" / „Team nötig"). Klickverhalten:

- **Native Zeile** (▸ = neue Listen-Screens, s. §4.2) bzw. **Reader** (`seite/{path}`,
  bestehender `SeiteScreen`) für reine Info-/Tabellenseiten.
- **Info-Zeile (nicht klickbar)** für Anmeldung/Abmeldung (Hinweistext).
- **„Im Browser öffnen"** für ALLE übrigen (auch Session-Seiten!) – siehe Sicherheit §5.

### 3.3 Login-/Demo-Umgang

- App-Zustände: real eingeloggt (`SignedIn`) bzw. Demo-Gast (`SignedInDemo`).
- **Demo-Nutzer + S-Eintrag:** lokale Karte „Erfordert ein angemeldetes Team" statt
  Netzwerk-/Reader-Zeile (kein toter Parse der 404-Stubseiten).
- **Reader-Härtung:** Marker-Erkennung (wie `SessionGuard`): „404 – not found",
  „ohne Team nicht verfügbar", „keine Rechte", Login-Formular → freundliche Fehlermeldung,
  nicht als Inhalt rendern. Schützt auch bei abgelaufener Session.

---

## 4. Technische Umsetzung

### 4.1 Phase A – Struktur & Einstieg (rein lesend)

1. `Routes.SERVER = "server"` (`app/src/main/java/com/onlinesoccer/app/ui/AppRoot.kt`);
   eigene `titelFuer`-Variante (nicht das „Team"-Label des Readers verwenden).
2. Neuer Screen `feature/server/ServerBereicheScreen.kt` (Konvention `feature/<bereich>/`):
   - Statische Struktur `ServerBereich(title, items)` / `ServerItem(label, path?, typ,
     erfordertLogin)`, URLs fix (live verifiziert), kein Server-Parsing.
   - Sektionen als Cards, Zeilen nach §3.2; nur-Lesen.
3. Dashboard-Karte „Weitere Online-Soccer-Bereiche" nach `SummaryCard`
   (`DashboardScreen.kt:107-161`); Callback `onServerBereicheClick` analog zu
   `onZugabgabeClick` (`AppRoot.kt:212-225`).
4. Reader-Navigation mit `Uri.encode(path)` auf `Routes.SEITE`
   (exakt das bestehende Muster, `AppRoot.kt:235-238`; multi-segment wie `osneu/…` funktioniert).
5. „Im Browser öffnen" per `ACTION_VIEW`-Intent (Forum-Karten-Muster).
6. Reader-Umbau in `SeiteViewModel.kt:39` / `SeiteScreen.kt:44-64`:
   aktuell ⇒ `seite == null && fehler == null` zeigt **leeren** Screen. Neu: Marker-Erkennung
   + Zustand „gesperrt/Login nötig" mit freundlichem Text (Demo) bzw. „Inhalt fehlt".

### 4.2 Phase B – native Listen (höchster Wert, empfohlen in diesem Vorhaben)

- `FreieTeamsScreen` (Land-Filter) und `ManagerlisteScreen` (Land/Liga-Filter):
  eigene State/ViewModel-Datenmodelle + `TeamRepository`-Methoden (GET auf `osneu/freieteams`
  bzw. `osneu/managerliste` mit Formularparametern, Parsing der Zeilentabelle).
  Begründung: Diese Seiten liefern erst nach Auswahl Zeilen – ein GET-Reader kann das nicht.
- Sicherung `lostpw`/`managersuche` bleiben Reader/Info (Browser in späterer Stufe).

### 4.3 Sicherung (Härtung)

- **Host-Whitelist** in `TeamRepository.ladeSeite` (`:159-162`): nur URLs auf
  `OsApi.BASE_URL` zulassen (Route ist künftig aus einem Menü erreichbar; defense-in-depth).
- **Kein Schreib-/Formular-Post** im App-Code (Regel aus PLAN_PHASE3).

---

## 5. Sicherheit (Review-Verdikt)

- Der Reader macht nur GET auf feste Pfade und rendert Links/Formulare **nicht**
  interaktiv (`HtmlTools.seitenAnsicht`), also kein CSRF-/GET-Nebenwirkungs-Risiko;
  versteckte Formularwerte werden nicht geleakt (Attribute, kein Text).
- **Wichtig (Review-Korrektur zur anfänglichen „nur öffentliche Browser"-Regel):** Der
  externe Browser hat **niemals** die App-Session (Cookies liegen nur im App-`OsCookieStore`).
  „Im Browser öffnen" ist daher für **alle** Seiten sicher und die sicherste verfügbare
  Variante für Schreibaktionen (Bewerbung, Zweitteam, Passwort ändern, …): keine
  App-Formular-Logik, Passwort läuft nie durch die App, nativer Site-Schutz/CSRF-Check.
  Phase-A-Versprechen „keine Writes im App-Code" bleibt unangetastet.
- Registrierung/Login bleibt auf dem LoginScreen; `lostpw`-Auswahl bewusst nicht riesig
  gerendert (siehe 4.2).

---

## 6. Bewertung

- Konsistenz: nur vorhandene Muster (Route, Reader, Karten, Browser-Intent). Wenig
  neuer Code, kein neuer Parser.
- Demo-/Gast-Nutzer: öffentliche Listen funktionieren sofort; S-Einträge zeigen eine
  saubere „erfordert Login"-Karte statt toter 404-Seiten (Korrektur eingearbeitet).
- `support.php` wurde live als **nicht-öffentlich** eingestuft → korrekt klassifiziert.

---

## 7. Offene Fragen (zur Freigabe)

1. **Scope:** Phase A nur (Struktur + Reader + Browser-Buttons) oder Phase A **+ B**
   (inkl. nativer Listen „Freie Teams" + „Team/Managerliste")? Empfehlung: beides, weil
   B der eigentliche Wertzuwachs ist (Reader kann die Listen ohne Filter nicht sinnvoll
   darstellen).
2. Einstieg nur als Dashboard-Karte (Empfehlung) oder zusätzlich ⋮-Eintrag in den Hubs?
3. Sektionen nach Aufgabe gruppieren (vorgeschlagen) oder 1:1 wie Website (Anmeldung/
   Bewerbung bzw. Optionen/Internes)?