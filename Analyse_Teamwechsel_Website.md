# Analyse Online-Soccer 2.0 – Menüpunkt „Teamwechsel"

- **Ziel:** Prüfen, wo der Menüpunkt „Teamwechsel" auf der Website liegt, welche Seite er aufruft und wie er sich verhält – als Grundlage für die native Abbildung in der App.
- **Methode:** Anmeldung mit dem privaten Konto, ausschließlich **Lesezugriffe** (GET) und statische Menü-/Quelltext-Analyse. **Keine** Schreibaktionen ausgelöst (kein Zweitteam übernommen, keine Transfers, kein Speichern).
- **Datenschutz:** Keine Zugangsdaten, keine Cookie-/Session-Werte, keine Kontostands- oder Spielerdaten in diesem Bericht.
- **Stand:** 30.09.2026, Saison 24. Der Account hat **genau ein Team**.

---

## 1. Ergebnis in einem Satz

Der Menüpunkt **„Teamwechsel" existiert serverseitig nur bedingt** und ist für einen Account mit nur einem Team **weder im Menü noch über eine eigene Seite erreichbar**. Er konnte daher noch nicht verifiziert werden.

---

## 2. Login-Technik (wichtig für die App)

| Aspekt | Befund |
|---|---|
| Endpunkt | `POST https://os.ongapo.com/validate.php` |
| Felder | `action=os_login`, `loginemail`, `passwort` |
| **Pflicht** | `imageField.x` + `imageField.y` mitsenden |
| User-Agent | Browser-UA **Pflicht** (ohne → HTTP 403) |
| Erfolg | `302` → `os_menu_haupt.html`, Cookies `os` (SID) + `lc` |

**Neuer Befund gegenüber `Analyse_OnlineSoccer_Phase2.md` §1:**

Sendet man das Formular **ohne** den Bild-Button `imageField.x/y` ab, antwortet der Server zwar mit `302` und setzt ein `os`-Session-Cookie – man landet aber **nicht** im eigenen Managerbüro, sondern in der **Gast-/Demo-Ansicht**:

- `wappen.php` zeigt `<span style="color: red; font-weight: bold">DemoTeam</span>`
- `haupt.php` enthält **beide** Marker `Kontostand` **und** `Demo-Managerbüro`

→ Ein **HTTP-302 nach Login ist kein Erfolgsindikator**. Die Erfolgsprüfung muss inhaltlich erfolgen.
Für die App ist das bereits berücksichtigt: `SessionGuard.isPersonalView()` (app/src/main/java/com/onlinesoccer/app/core/network/SessionGuard.kt:18) wertet genau diese Marker aus. Der Befund bestätigt diese Heuristik und zeigt, warum der `imageField`-Submit Pflicht ist.

---

## 3. Wo „Teamwechsel" NICHT auftaucht

Alle folgenden Seiten wurden mit gültiger Session geladen und case-insensitiv auf `teamwechsel` durchsucht – **0 Treffer**:

| Seite | Ergebnis |
|---|---|
| `os_menu_haupt.html` (Linkes Menü, Frameset) | 0 Treffer |
| `menue.php` (Menü-Frame) | 0 Treffer |
| `haupt.php` (Managerbüro-Startseite) | 0 Treffer |
| `wappen.php` (Kopf-/Wappenframe) | 0 Treffer |
| `showteam.php` (Mannschaft) | 0 Treffer |
| `os_kader.html` (Kader) | 0 Treffer |
| `einstellungen.php` (Erweiterte Einstellungen) | 0 Treffer |
| `wiki/Spielregeln_I._Manager` | 0 Treffer |
| `wiki/Spielregeln_III._Mannschaft` | 0 Treffer |

Eigene Seite existiert ebenfalls nicht:

| URL | HTTP |
|---|---|
| `teamwechsel.php` | **404** |
| `zweit.php` | 404 |
| `teamwahl.php` | 404 |
| `teams.php` | 404 |
| `tewechsel.php` | 404 |

### Wichtige Beobachtung zum Menü

`os_menu_haupt.html` ist ein **statisches JavaScript-Menü** (`m(1,"…")` / `m(2,"…")` in `js/osmenu1.js`), das clientseitig in den Frame `menue.php` geschrieben wird.

Die Datei ist **byte-identisch** für nicht angemeldete und angemeldete Sessions (je 6446 Bytes) – sie enthält **keine** Login-/Team-abhängigen Einträge. Das heißt:

> Jeder konditionale Menüpunkt (wie „Teamwechsel") kann **nicht** im statischen `os_menu_haupt.html` stehen. Er muss entweder über eine andere Menüquelle oder in `haupt.php` / `wappen.php` gerendert werden.

Das ist der entscheidende offene Punkt für die Umsetzung.

---

## 4. Vorhandene Zweitteam-Funktionen (verifiziert)

Im Menü `Anmeldung/Bewerbung` existieren zwei einschlägige Einträge:

| Menüpunkt | URL | In der App |
|---|---|---|
| Freie Zweitteams | `osneu/fzt` | `ServerBereicheScreen.kt` (FREIE_ZWEITTEAMS) |
| **Zweitteam übernehmen** | `zweitteam.php` | `ServerBereicheScreen.kt:97` (Browser-Aktion) |

`zweitteam.php` liefert die offizielle Regelbeschreibung und einen Submit-Button:
> „Ich möchte ein (neues) Zweitteam unwiderruflich übernehmen. Das Zweitteam wird zufällig zugeteilt. Diese Aktion kann nicht mehr rückgängig gemacht werden."

### Regeln zum Zweitteam (aus `zweitteam.php` + Wiki)

- Ein Zweitteam wird **zufällig** aus einem Pool aller Zweit- und Drittligisten zugeteilt.
- **Kein Limit**, wie lange es gemanagt werden darf.
- Übernahme nur **alle 72 ZAT** möglich; bei Verlust muss die Sperre abgewartet werden.
  - Ausnahme: Ein anderer Mitspieler bewirbt sich auf das Team und führt ≥ 7 ZAT – dann darf vor Ablauf übernommen werden.
- **Transfers und Leihen zwischen den beiden Teams sind ausnahmslos verboten** → Spielausschluss.
- **Dreieckstransfers** unter Einbeziehung fremder Teams sind verboten.
- Ein FSS pro Saison zwischen Haupt- und Zweitteam ist erlaubt, **50/50-Einnahmenverteilung**.
- Ein Manager muss das Zweitteam abgeben, wenn beide Teams international spielen würden.
- Zweck: dauerhaft spielen **oder** als Bewerbungsteam für einen Neumanager aufbauen.

---

## 5. Offene Punkte für den Zugang mit Zweitteam

Sobald ein Account **mit** Zweitteam zur Verfügung steht, ist gezielt zu prüfen:

1. **Wo erscheint der Eintrag?** Linkes Menü, Kopf-/Wappenframe (`wappen.php`) oder `haupt.php`?
   - Falls linkes Menü: welche **dynamische Menüquelle** liefert die konditionalen Einträge (nicht `os_menu_haupt.html`)?
2. **Ziel-URL / Endpoint** von „Teamwechsel" – und ist es ein `GET` mit Parameter (z. B. Team-ID) oder ein `POST`?
3. **Wie wird die Team-ID übergeben** (Query-Parameter, Session, Cookie)?
4. **Wie erkennt die App ein Zweitteam programmatisch?**
   - Kandidaten im bereits abgerufenen Material: mehrere `wappen.php`-Teamnamen, Team-IDs in `haupt.php`, ein Team-Select-Element.
   - Aktuell liefert `wappen.php` nur **einen** Teamnamen + Liga → Single-Team-Erkennung ist trivial, Multi-Team-Fallback fehlt noch.
5. **UI:** Dropdown, Button-Liste, Link im Wappenframe? Wie wird das aktive Team markiert?
6. **Zustand nach Wechsel:** Ändern sich `haupt.php`, `wappen.php`, Zugabgabe- und Finanzwerte komplett? (Session-Swap oder Team-Parameter?)
7. **Berechtigungen:** Welche Team-Bereiche sind für das Zweitteam deaktiviert (z. B. International, Transfermarkt)?

---

## 6. Umsetzungsskizze (vorläufig)

- **Noch keine Code-Änderung.** Der Menüpunkt kann ohne Zweitteam-Access nicht sinnvoll implementiert werden (Ziel-URL und UI unbekannt).
- Sobald geklärt: In der Team-Menüleiste (vgl. `Ändere die Navigation.txt`) einen Eintrag **„Teamwechsel"** vorsehen, der **standardmäßig ausgeblendet** ist und nur bei erkanntem Zweitteam erscheint.
- Erkennung möglichst über ein bestehendes Feld des Managerbüro-Requests statt über einen zusätzlichen Endpoint – Abhängigkeit von `wappen.php`-Struktur vermeiden.