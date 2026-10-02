# Sicherheitsrichtlinie (SECURITY)

## Bitte keine Sicherheitslücken über öffentliche Issues melden

Ein öffentliches Issue ist für jeden sichtbar – auch für Leute, die deinen
Fund ausnutzen wollen, bevor ein Fix verfügbar ist.

Nutze stattdessen **GitHub Private Vulnerability Reporting**:

1. Repository → **Security** → **Vulnerability reporting**
2. **Report a vulnerability** klicken

Über diesen Weg erreicht die Meldung nur das Maintainer-Team, ist also nicht
öffentlich sichtbar. Voraussetzung dafür ist, dass das Feature für dieses
Repository aktiviert ist.

## Was du melden kannst

Alles, was die App oder ihre Nutzerinnen und Nutzer gefährdet, zum Beispiel:

- **Zugangsdaten im Klartext** – z. B. Log-Ausgaben (`Log.d`), die
  Session-Tokens, Cookies oder Passwörter enthalten
- **Fehlerhafte Verschlüsselung** – etwa ein Auslesen des
  `EncryptedSharedPreferences`-Inhalts
- **Unsichere Netzwerkkommunikation** – `http://` statt `https://`, fehlende
  Zertifikatsprüfung
- **Rechteausweitung** über die App hinaus, etwa Zugriff auf Dateien anderer Apps
- **Offenlegung von Nutzerdaten**, die über das für die Funktion Nötige
  hinausgehen

## Was ausdrücklich *keine* Lücke ist

- Die App liest Daten von os.ongapo.com, um Spieler-, Vereins- und
  Spielstände anzuzeigen. Das ist die Kernfunktion und gewollt.
- Wo es ohne Erlaubnis der Betreiberinnen und Betreiber keine technische
  Abhilfe gibt, ist ein Bug – kein Sicherheitsproblem. Melde ihn als Issue.
- Die APK wird bewusst nicht im Play Store gehalten und nicht
  automatisch aktualisiert. Das ist eine bewusste Entscheidung, keine Lücke.

## Was du erwarten kannst

| | |
|---|---|
| **Erste Rückmeldung** | innerhalb von 7 Tagen |
| **Einschätzung** | innerhalb von 14 Tagen, ob es bestätigt ist |
| **Fix** | bei bestätigten Lücken mit hoher Priorität |

Bist du auf eine Frist angewiesen – etwa weil Exploit-Code bereits im Umlauf
ist? Schreib das ausdrücklich in die Meldung.

## Anerkennung

Wenn du möchtest, nenn ich dich in den Release-Notes der behobenen Version.
Sag dazu einfach Bescheid.

## Für Endnutzerinnen und -nutzer

Die App speichert deine Zugangsdaten lokal verschlüsselt und gibt sie nur an
os.ongapo.com weiter. Sie enthält keine Werbung, kein Analytics und keine
Drittanbieter-Bibliotheken. Sie wird in der [Download-Seite](https://barezi5527.github.io/OnlineSoccer-Download/)
mit einer SHA-256-Prüfsumme bereitgestellt, damit du die APK gegen Manipulation
prüfen kannst.