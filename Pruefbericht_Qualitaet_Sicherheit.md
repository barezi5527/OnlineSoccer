# Kurzbericht: Qualität und Sicherheit

**Prüfdatum:** 08.11.2026  
**Geprüft:** Quellcode der Android-App Online Soccer  
**Methode:** Statische Prüfung des Quellcodes und lokale Build-/Test-Prüfungen

## Kurzfazit

Die App wirkt insgesamt ordentlich aufgebaut und enthält keine offensichtlichen
kritischen Sicherheitslücken. Sie verwendet nur die für die Verbindung nötigen
Berechtigungen und speichert die Sitzungs-Cookies verschlüsselt.

Für eine veröffentlichte Version sollten aber einige Punkte verbessert werden.
Die wichtigste Empfehlung ist, URLs aus den Server-Antworten strenger zu prüfen
und keine Sitzungs- oder Netzwerkdetails ins Logcat zu schreiben.

## Was gut ist

- Die Prüfung auf Zugangsdaten und geheime Dateien ist bestanden. Keystore-Dateien
  und lokale Konfigurationen sind nicht im Git-Versionsstand enthalten.
- Die App verwendet `HTTPS` und verbietet unverschlüsselte HTTP-Verbindungen.
- Es gibt nur die Berechtigungen `INTERNET` und `ACCESS_NETWORK_STATE`.
- Sitzungs-Cookies werden mit Android Keystore und
  `EncryptedSharedPreferences` verschlüsselt gespeichert
  (`app/src/main/java/com/onlinesoccer/app/core/storage/TokenStorage.kt`).
- Es gibt keine WebView und keine JavaScript-Schnittstelle. Server-HTML wird als
  Daten verarbeitet, nicht als ausführbarer Code.
- Die App enthält keine erkennbare Analyse-, Werbe- oder Tracking-Bibliothek.

## Gefundene Punkte

### 1. Mittel: URLs werden teilweise nicht ausreichend geprüft

Mehrere Stellen übernehmen URLs oder Formularziele aus dem HTML der Website,
zum Beispiel in `TeamRepository.kt:214-218` und `:512-513`, in
`DashboardRepository.kt:102-110` sowie in `InternationaleRepository.kt:517-521`.
Dabei wird teilweise nur geprüft, ob ein Text mit `https` oder der Basis-URL
beginnt. Das ist keine vollständige Prüfung des tatsächlichen Hostnamens.

**Warum verständlich gesagt:** Wenn eine manipulierte Server-Antwort eine fremde
URL liefert, könnte die App diese URL aufrufen oder einen Link dorthin öffnen.
Die Sitzungs-Cookies sind zwar für `os.ongapo.com` begrenzt, Formulardaten sollten
aber trotzdem niemals an fremde Server gesendet werden.

**Empfehlung:** Jede URL mit `HttpUrl` parsen und nur `os.ongapo.com` sowie
ausdrücklich erlaubte Subdomains akzeptieren. Für externe Links zusätzlich eine
klare Bestätigung oder eine feste Allowlist verwenden.

### 2. Niedrig bis mittel: Informationen im Produktions-Log

`TeamwechselRepository.kt:79-92` schreibt HTTP-Status und Fehlermeldungen mit
`Log.w` nach Logcat. Die Meldungen enthalten derzeit keine Cookies oder
Passwörter, sollten bei einer veröffentlichten App aber möglichst nicht dauerhaft
vorhanden sein.

**Empfehlung:** Logging auf Debug-Builds begrenzen oder nur allgemeine,
nicht sensible Fehlercodes protokollieren. Niemals URLs mit Zugangsdaten,
Antwort-HTML, Cookies oder Formulardaten loggen.

### 3. Mittel: Release-Schutz ist noch nicht optimal

In `app/build.gradle.kts:41-54` ist für Release `isMinifyEnabled = false` gesetzt.
Ohne R8 ist der Code leichter zu untersuchen. Ohne lokale
`keystore.properties` fällt der Release-Build außerdem absichtlich auf die
Debug-Signatur zurück.

**Empfehlung:** Für die tatsächlich verteilte APK `isMinifyEnabled = true` und
`shrinkResources = true` prüfen. Die echte Release-APK ausschließlich mit einem
nicht versionierten Keystore signieren. Die Debug-Signatur darf nicht für eine
öffentliche Release verwendet werden.

### 4. Niedrig: Veraltete bzw. experimentelle Abhängigkeit

`gradle/libs.versions.toml:12` verwendet
`androidx.security:security-crypto:1.1.0-alpha06`. Eine Alpha-Version kann sich
ändern oder auf einzelnen Geräten Probleme machen.

**Empfehlung:** Eine aktuelle stabile Lösung prüfen und die Migration mit Tests
für vorhandene Sitzungen absichern.

## Qualitätsprüfung

- Secret-Prüfung: **bestanden** (`check-no-secrets.sh`).
- Unit-Test-Aufgabe: **ohne gemeldeten Testfehler abgeschlossen bzw. bereits
  aktuell** (`UP-TO-DATE`).
- Debug-Build: **ohne gemeldeten Buildfehler erzeugt**.
- Lint und Release-Build: In der Prüfumgebung blieb Gradle beim Lint-/Release-
  Abschluss hängen. Deshalb wird hier kein erfolgreiches Gesamtergebnis
  behauptet; diese beiden Befehle sollten in CI erneut geprüft werden.

## Grenzen der Prüfung

Geprüft wurde die App, nicht die Website selbst. Aussagen über TLS-Konfiguration,
Server-Logout, CSRF-Schutz und Session-Gültigkeit des Servers sind daher nicht
Bestandteil dieses Berichts.

## Priorität für die nächsten Schritte

1. URL-Allowlist mit geparstem Hostnamen einführen.
2. Produktions-Logging reduzieren oder auf Debug beschränken.
3. Release-APK mit R8 und echter Release-Signatur in CI bzw. lokal verifizieren.
4. Abhängigkeiten aktualisieren und danach Tests, Lint sowie beide Builds erneut
   ausführen.
