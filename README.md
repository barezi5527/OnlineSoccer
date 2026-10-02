# Online Soccer

Eine Android-App für das Online-Fußballspiel auf [os.ongapo.com](https://os.ongapo.com) –
mit Spielerkarte, Transfermarkt, Aufstellungen, ZAT-Berichten, Statistiken und
Freundschafts-/Teamwechsel-Funktionen.

Die App ist **freie und quelloffene Software** unter der
[GPL-3.0-Lizenz](LICENSE). Beiträge aus der Community sind willkommen – siehe
[CONTRIBUTING.md](CONTRIBUTING.md).

> **Kein offizielles Produkt von ongapo.** Dies ist eine inoffizielle Fan-App.
> Sie wird von den Betreiberinnen und Betreibern von os.ongapo.com weder
> unterstützt noch genehmigt. Alle Marken und Rechte liegen bei den jeweiligen
> Inhabern.

## Für Nutzerinnen und Nutzer (ohne Programmieren)

Du möchtest die App einfach nur benutzen? Dann lade dir die fertige APK von der
[Download-Seite](https://barezi5527.github.io/OnlineSoccer-Download/) herunter.
Dort liegen auch die Download-Zähler und alle bisherigen Versionen.

Zum Installieren auf dem Android-Gerät musst du in den Systemeinstellungen
„Installation aus unbekannten Quellen" für deinen Browser aktivieren. Es gibt
die App bewusst **nicht** im Play Store – die APK lässt sich jederzeit selbst
prüfen und neu bauen.

## Mitwirken

<details open>
<summary><b>Code ausprobieren und selbst kompilieren</b></summary>

### Voraussetzungen

| | |
|---|---|
| **JDK** | 17 oder neuer |
| **Android SDK** | Plattform **API 35** |
| **Gradle** | nicht nötig – der Wrapper (`gradlew`) bringt Version 9.5.0 mit |

Du brauchst dafür **kein** Android Studio, obwohl es die bequemste Umgebung ist.
Ein schlichter Texteditor plus die beiden Zeilen `JAVA_HOME` und
`ANDROID_HOME` genügen.

### Loslegen

```bash
# 1. Android SDK-Pfad eintragen (anpassen)
export ANDROID_HOME=$HOME/Android/Sdk

# 2. Prüfen, ob die Toolchain erkannt wird
sdkmanager "platforms;android-35"

# 3. Debug-APK bauen
./gradlew :app:assembleDebug

# 4. Unit-Tests ausführen (34 Testklassen)
./gradlew :app:testDebugUnitTest
```

Die fertige APK liegt danach unter
`app/build/outputs/apk/debug/app-debug.apk` und lässt sich per
`adb install app/build/outputs/apk/debug/app-debug.apk` auf dem Gerät
installieren.

### Release-Build

```bash
./gradlew :app:assembleRelease
```

Für eine **signierte** Release brauchst du ein eigenes Keystore und eine
`keystore.properties` im Repo-Root:

```properties
storeFile=mein-keystore.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Diese Datei steht in `.gitignore` und wird **niemals** committet – bitte
prüfe das vor jedem Commit. Ohne Keystore fällt der Release-Build
automatisch auf die Debug-Signatur zurück (siehe `app/build.gradle.kts`),
damit frische Entwicklungsumgebungen sofort loslegen können.

</details>

### Wie das Projekt aufgebaut ist

```
app/src/main/java/com/onlinesoccer/app/
├── core/          Netzwerk (OkHttp), Auth, Token-Speicher, Theme, State
├── data/          Datenmodelle und Repositories
├── feature/       Oberflächen: auth, dashboard, team, taktik, transfers,
│                  spiele, zat, zugabgabe, bewerbe, statistik, pm, server
└── ui/            Wiederverwendbare Compose-Komponenten
```

**Technischer Stack**

- Kotlin 2.2.10, Android Gradle Plugin 9.1.0
- Jetpack Compose (BOM 2024.12.01), Material 3
- Hilt / Dagger für Dependency Injection
- OkHttp + Jsoup für HTTP und HTML-Parsing
- androidx.security-crypto für die sichere Speicherung der Zugangsdaten
- minSdk 26, targetSdk 35

Die Parser sind gegen HTML-Fixtures getestet, die unter
`app/src/test/resources/dumps/` liegen. Wenn sich die Weboberfläche von
os.ongapo.com ändert, sind das die Dateien, die zuerst wieder funktionieren
müssen – siehe [`Analyse_OnlineSoccer.md`](Analyse_OnlineSoccer.md).

## Beitragen

Beiträge sind ausdrücklich willkommen. Bitte lies zuerst
[CONTRIBUTING.md](CONTRIBUTING.md) – kurz gesagt:

1. Issue anlegen oder einen bestehenden suchen, bevor du größere Änderungen
   machst
2. Fork erstellen, Feature-Branch anlegen
3. Mit `./gradlew :app:testDebugUnitTest` prüfen, dass alles grün ist
4. Pull Request eröffnen

Beiträge unterliegen automatisch der GPL-3.0 – siehe den Hinweis am Ende
dieser Datei.

## Sicherheitslücken melden

Bitte **nicht** über öffentliche Issues melden. Der Meldeweg steht in
[SECURITY.md](SECURITY.md).

## Verhaltenskodex

Für die Teilnahme an diesem Projekt gilt der
[Code of Conduct](CODE_OF_CONDUCT.md).

## Datenschutz

Die App speichert Zugangsdaten lokal im verschlüsselten Keystore des Geräts
(`androidx.security-crypto`) und überträgt sie ausschließlich an
os.ongapo.com. Es gibt **kein** Analytics, keine Tracking-Bibliothek, keine
Werbung und keine Drittanbieter-CDNs.

## Lizenz

    Online Soccer – eine inoffizielle Fan-App für os.ongapo.com
    Copyright (C) 2026 barezi5527

    Dieses Programm ist freie Software: du darfst es unter den Bedingungen der
    GNU General Public License Version 3 wie veröffentlicht weitergeben und
    verändern.

    Dieses Programm wird OHNE JEDE GEWÄHRLEISTUNG bereitgestellt. Siehe die
    GNU General Public License für weitere Einzelheiten.

    <https://www.gnu.org/licenses/>

Den vollständigen Lizenztext findest du in [LICENSE](LICENSE). Kurzfassung
für Mitwirkende: Der Quellcode bleibt offen. Wer eine veränderte Fassung
weitergibt, muss auch diese unter GPL-3.0 veröffentlichen.