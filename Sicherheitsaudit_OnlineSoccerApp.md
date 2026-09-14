# Sicherheitsaudit – OnlineSoccer Android-App

Audit-Datum: 13.09.2026
Prüfobjekt: Android-Projekt `OnlineSoccer` (Paket `com.onlinesoccer.app`)
Methode: Statische Analyse (Quellcode, Build-Konfiguration, Ressourcen, Git-Historie, Signatur-Konfiguration) inkl. empirischer APK-Prüfung

> Orientierung: OWASP MASVS / MASTG. Untersucht wurde ausschließlich die Android-App.
> Die Sicherheit der Online-Soccer-Website bzw. deren Server (TLS-Konfiguration, CSRF,
> Server-Logout, Session-Validierung) wurde **nicht** geprüft und wird hier nicht bewertet.

---

## Gesamtbewertung

**B = RELEASE MÖGLICH, ABER EMPFOHLENE VERBESSERUNGEN**

Die App ist sauber aufgebaut: keine eingebauten Zugangsdaten, keine WebView/XSS-Fläche,
verschlüsselte Token-Ablage, HTTPS-Zwang, minimale Berechtigungen, kein Logging im
Produktionscode. Vor einer breiten Verteilung sollten die folgenden Punkte adressiert werden
(v. a. Zertifikat-Pinning, Release-Signing, Minification, Export-Flag der Debug-Aktivität).

---

## Ergebnistabelle

RISIKO | SCHWEREGRAD | FUNDSTELLE | PROBLEM | AUSWIRKUNG | LÖSUNG | RELEASE-BLOCKER?
---|---|---|---|---|---|---
Kein Zertifikat-Pinning | HOCH | `core/network/NetworkModule.kt:26-40` | OkHttp nutzt nur den System-Trust-Store. `lc`-Token (2 Jahre Laufzeit) und Login-Passwort könnten bei MITM (bösartiges CA, Corporate Proxy, Captive Portal) abgefangen werden. | Token-Diebstahl bis hin zur Account-Übernahme durch Dritte | `CertificatePinner` für `os.ongapo.com` oder `network_security_config.xml` mit Pins | Bedingt
Release ohne Minification/Obfuskation | MITTEL | `app/build.gradle.kts:24` | `isMinifyEnabled = false` – R8/ProGuard läuft nie; `proguard-rules.pro` ist wirkungslos. APK trivial dekompilierbar. | Leichtes Reverse Engineering der Session-Architektur | `isMinifyEnabled=true`, `shrinkResources=true`, ProGuard-Regeln ergänzen (Hilt/Kotlinx-Serialization) | Bedingt
Kein Release-Signing konfiguriert | MITTEL | `app/build.gradle.kts` (gesamtes `signingConfigs` fehlt) | Release-APK wäre unsigned und nicht installierbar/veröffentlichbar. | Verteilung ist so nicht möglich | Keystore erzeugen, `signingConfigs.release` aus gitignoriertem `keystore.properties` lesen | Bedingt
`androidx.security:security-crypto` als Alpha | MITTEL | `gradle/libs.versions.toml:12` | `1.1.0-alpha06`, deprecated. Bekannte AEADBadTagException-/Keystore-Probleme auf einigen Geräten. Schützt die verschlüsselte Token-Ablage. | Token-Ablage kann auf bestimmten Geräten fehlerhaft sein | Auf stabile Alternative migrieren (aktuelles `androidx.security` oder Tink); `allowBackup=false` zwingend beibehalten (gesetzt) | Bedingt
Server-kontrollierte URLs schwach validiert | MITTEL | `data/repository/TeamRepository.kt:177-212`, `data/repository/InternationaleRepository.kt:179-183`, `data/repository/DashboardRepository.kt:62-70` | `startsWith(OsApi.BASE_URL)` statt geparstem Host-Check. `fuehreAktionAus()`, `ladeAktionFormular()`, `seiteUrl()`, `absoluteUrl()` übernehmen `http*`-Pfade direkt aus Server-HTML. Cookie-Jar ist domain-beschränkt (kein direkter Token-Leak); Formulardaten könnten aber an fremde Hosts gesendet werden. | Datensendung an fremde Hosts nur bei kompromittiertem Server/MITM | Host-Allowlist via `HttpUrl.toHttpUrl().topPrivateDomain()`, nur `os.ongapo.com` + Subdomains | Nein (Defense-in-Depth)
OkHttp-Cache speichert authentifizierte Antworten im Klartext | MITTEL | `core/network/NetworkModule.kt:31` | 10 MB Cache in `cacheDir/http_cache` speichert HTML-Antworten (Aufstellungen, Nachrichten, Kontodaten) unverschlüsselt auf Disk. | Lesbar bei Root/Custom-Recovery; `allowBackup=false` mildert | Interceptor für `Cache-Control: no-store` auf auth-geschützte Seiten; oder Cache deaktivieren | Nein
`DebugDumpActivity` exportiert + loggt interne Daten | MITTEL | `src/debug/AndroidManifest.xml:4-6`, `src/debug/.../DebugDumpActivity.kt:51-127` | `exported="true"`, ungeschützt. Dumpet ~30 authentifizierte Seiten (Kader, Verträge, Gehälter, Liga) in Logcat (`Log.d`) und `cacheDir`. Nur in Debug-Builds vorhanden. | Jede App auf einem Debug-Gerät kann via Intent einen vollständigen persönlichen Daten-Dump auslösen | `exported="false"` setzen und nie nach `src/main` verschieben | Bedingt (Debug-only)
Test-Fixtures unter `src/test/resources/dumps/` | INFORMATION | `app/src/test/resources/dumps/*.html` (67 Dateien) | Enthalten **ausschließlich virtuelle Online-Soccer-Spieldaten** (Spieler-, Manager-, Vereinsnamen, Gehälter, „Geburtstag = ZAT-Nr."). **Keine** E-Mail-Adressen, Logins, Passwörter, Session-/Cookie-Tokens oder andere personenbezogene Daten realer Personen. Werden **nicht** in das APK gepackt (empirisch am Debug-APK verifiziert). | Kein Datenschutz- oder Security-Risiko | Optional: bei Veröffentlichung als Repo Kommentar „virtuelle Spieldaten" ergänzen | Nein
Logout invalidiert Server-Session nicht vollständig | NIEDRIG | `core/auth/SessionManager.kt:107-115`, `core/network/OsCookieStore.kt:75` | GET auf `index.php`, dann lokales Löschen. Der 2-Jahres-`lc`-Token bleibt serverseitig gültig. | Geteiltes Gerät: Session könnte serverseitig weiter aktiv sein | Server-Logout-Endpunkt nutzen, falls vorhanden (Server-Grenze) | Nein
`URL.openStream()` ohne Timeout für Team-Logos | NIEDRIG | `feature/dashboard/DashboardScreen.kt:205-211` | Bild-Load über `java.net.URL` statt OkHttp: kein Timeout, kein Host-Check. Cleartext ist global blockiert. | Hänger bei langsamem Bildserver; https zu beliebigen Hosts möglich | OkHttp-Client (mit Timeouts) oder `HttpUrl`-Validierung + https-Zwang | Nein
Passwort verbleibt bis Activity-Ende im RAM | INFORMATION | `ui/AppViewModel.kt:27-28` | Passwort in `MutableStateFlow`; wird nie persistiert, nie geloggt. | Minimal (Heap-Dump theoretisch möglich) | Optional: nach Login aus dem StateFlow entfernen | Nein
Kein CSRF-Token-Handling | INFORMATION | Repositories (alle Form-POSTs) | App repliziert Browser-Formulare (wie die Website selbst); serverseitige CSRF-Eigenschaft. | Kein App-eigenes Risiko | Server-seitig klären (Server-Grenze) | Nein
`BuildConfig` aktiviert, `USER_AGENT` hartkodiert | INFORMATION | `core/network/OsApi.kt:14` | Versionsstring im User-Agent weicht von `versionName` ab und wird nicht gepflegt. | Kein Sicherheitsproblem, Driftgefahr | Aus `BuildConfig.VERSION_NAME` ableiten | Nein

---

## Prüfumfang im Detail

### 1. Zugangsdaten und geheime Daten

Sicher. Systematische Suche über alle Quell-, Config-, Resourcen- und Build-Dateien nach
API-Keys, Passwörtern, Tokens, Secrets, privaten URLs, Base64-Secrets, IP-Adressen und
privaten Schlüsseln: **kein Treffer mit echtem Credential-Wert**.

- Kein `*.pem`, kein Keystore, kein `.env` im Projekt.
- `.gitignore` schließt `.os_credentials`, `credentials*`, `*.pem`, `.cookies/` korrekt aus.
- `local.properties` enthält nur `sdk.dir` (Maschinenpfad) und ist untracked.
- Kommentare und Doku beschreiben nur das Schema (`lc`/`os`-Cookie, Login-Felder), keine Werte.

### 2. Login und Session-Management

- Login: HTTPS-POST auf `validate.php` (`action=os_login`, `loginemail`, `passwort`). Passwort nie persistiert, nur im RAM-StateFlow.
- Speicherung: `EncryptedSharedPreferences` mit Keystore-`MasterKey` (AES256-GCM), Schlüsselnamen AES256-SIV, Werte AES256-GCM (`core/storage/TokenStorage.kt:22-33`).
- Cookies: eigener `CookieJar` (`core/network/OsCookieStore.kt`), domain-beschränkt; nur `os` und `lc` werden verschlüsselt persistiert.
- Stiller Relogin via `lc` beim App-Start mit inhaltsbasierter Verifikation (`SessionGuard`), kein falsches „eingeloggt".
- Logout löscht Cookies + verschlüsselte Persistenz (`cookieStore.clear()`). ⚠️ Serverseitige Gültigkeit bleibt (siehe Tabelle).
- `allowBackup="false"` verhindert Backup-Extraktion.

### 3. Netzwerk

- Ausschließlich HTTPS: `BASE_URL = "https://os.ongapo.com"`, `usesCleartextTraffic="false"`, kein HTTP in `src/main`.
- Kein Custom-TrustManager, kein Hostname-Verifier-Override, kein Trust-All.
- ⚠️ Kein Zertifikat-Pinning (höchste einzelne Empfehlung).
- Redirect-Following aktiv; durch globales Cleartext-Verbot risikobegrenzend.
- ⚠️ Server-kontrollierte URLs erreichen den OkHttp-Stack mit schwacher Host-Prüfung (siehe Tabelle).

### 4. Android-Berechtigungen

| Permission | Benötigt? | Kommentar |
|---|---|---|
| `android.permission.INTERNET` | Ja | Kernfunktion (Login, Datenabruf) – nicht entfernbar, kein Risiko |
| `android.permission.ACCESS_NETWORK_STATE` | Ja | Netzwerk-Status – nicht entfernbar, kein Risiko |

Keine weiteren Permissions (kein Speicher-, Kamera-, Kontakt-, Standort-, Notifications-Zugriff).
Beispielhaft minimale Berechtigungsfläche.

### 5. WebView / Website-Integration

**Ausdrücklich geprüft: keine WebView vorhanden.**

- Kein `WebView`, kein `WebViewClient`, kein `loadUrl`, kein `addJavascriptInterface`, kein
  `setJavaScriptEnabled`, kein `evaluateJavascript` – über alle `.kt`/`.xml`-Dateien.
- Server-HTML wird ausschließlich mit Jsoup geparst und in Compose-Komponenten dargestellt:
  keine Remote-JS-Ausführung, keine JS-Brücke, keine Stored-/Reflected-XSS-Injection-Fläche.
- Externe Links via `ACTION_VIEW`; eine `forumUrl` aus Server-HTML ohne Host-Validierung
  (siehe Tabelle, Defense-in-Depth).
- Keine Deep-Links (`MainActivity` ohne `data:`-Filter).

### 6. Eingabevalidierung

- Form-Bodies generisch via OkHttp `FormBody` (keine SQL-/HTML-Injection).
- Server-Antworten werden als Daten verarbeitet, nie ausgeführt.
- ⚠️ Schwachstelle wie oben: `http*`-Pfade aus Server-HTML ohne Host-Allowlist an
  `ladeSeite`, `fuehreAktionAus`, `ladeAktionFormular`, `absoluteUrl`, `teamLogoUrl`, `forumUrl`.

### 7. Logging und Debugging

- **Produktionscode: 0 `Log.*`/`println`/`printStackTrace`** in `src/main`.
- Nur `DebugDumpActivity` (Debug-Source-Set) loggt Voll-HTML und Kader-/Vertragsdaten – nicht im Release-APK, aber `exported="true"` (siehe Tabelle).
- Kein Crash-Reporter, kein HTTP-Logging-Interceptor.
- Release-Build: kein `debuggable`, default (release = nicht debuggbar).

### 8. APK / Release-Sicherheit

- `compileSdk 35`, `targetSdk 35` (aktuell 2026), `minSdk 26` (Android 8.0) – sinnvoll.
- ⚠️ Kein `signingConfig` für Release → unsigned APK.
- ⚠️ `isMinifyEnabled=false` → keine Obfuskation.
- Debug-Dateien sauber ausgeschlossen (`debugImplementation`, `src/debug`).
- Testdaten/Credentials gelangen nicht ins APK (empirisch am Debug-APK verifiziert).

### 9. Datenschutz

- Verarbeitet: E-Mail + Passwort (Login), Team-/Kader-/Transfer-/Finanzdaten – ausschließlich Richtung `os.ongapo.com`.
- **Keine** Drittanbieter-SDKs, kein Analytics, kein Tracking, keine Werbung.
- Lokal gespeichert: verschlüsselte Tokens, Theme-Präferenz (Bool, unkritisch), OkHttp-Cache (Klartext, ⚠️ siehe Tabelle).
- Externe Bibliotheken: Compose, Activity/Navigation/Lifecycle, kotlinx-coroutines, OkHttp 4.12.0, Jsoup 1.18.1, Security-Crypto (⚠️ Alpha), Hilt, JUnit (Test), Compose-UI-Tooling (Debug).

### 10. Code und Abhängigkeiten

- AGP 9.1.0, Kotlin 2.2.10, Gradle 9.5.0 – aktuell.
- Compose-BOM 2024.12.01 ≈ 18 Monate älter als Rest (Hygienedrift, kein bekannter CVE).
- OkHttp 4.12.0 ohne offene kritische CVEs; Jsoup 1.18.1 aktuell.
- Kein Gradle-Dependency-Verification/Lockfile (Option zur Härtung).
- Repository-Politik auf google() + mavenCentral begrenzt, `FAIL_ON_PROJECT_REPOS` gesetzt (gut).

### 11. Git und Projektdaten

- Git-Historie (3 Commits) geprüft: keine Credentials, keine Keystores, keine sensiblen Dateien versioniert.
- Test-Fixtures: ausschließlich virtuelle Spieldaten, kein APK-Bestandteil (siehe Tabelle).

### 12. Funktionale Sicherheit (Zugabgabe/ZAT/Transfers)

- Kritische Aktionen werden nur aus expliziter Nutzerabsicht ausgelöst (Buttons mit `speichernd`-Flags, keine Auto-Submit im Init).
- Keine Selbst-POSTs aus Parser-Daten allein; lokale Zustände werden serverseitig validiert.
- Kein `debounce`/`distinctUntilChanged` auf Speichern-Triggern – Doppel-Tap kann zwei POSTs auslösen (Idempotenz ist eine Server-Eigenschaft, Server-Grenze).

### 13. Servergrenze

- Die Online-Soccer-Website (TLS, CSRF, Session-Validierung, Server-Logout) wurde **nicht** geprüft.
- Die App erzeugt keine neuen App-eigenen Risiken über die in der Tabelle genannten Punkte hinaus.

---

## Priorisierte Empfehlungen vor Release

| Prio | Maßnahme | Aufwand |
|---|---|---|
| 1 | Zertifikat-Pinning für `os.ongapo.com` (wegen 2-Jahres-`lc`-Token) | gering |
| 2 | Release-Signing konfigurieren (Keystore + `signingConfigs.release`), Keystore nie committen | gering |
| 3 | `isMinifyEnabled=true` + ProGuard-Regeln vervollständigen | gering |
| 4 | `DebugDumpActivity` auf `exported="false"` setzen | trivial |
| 5 | Security-Crypto-Migration planen / monitoren | mittel |
| 6 | Host-Allowlist (`os.ongapo.com`) bei allen `http*`-URL-Übernahmen | mittel |
| 7 | OkHttp-Cache für auth-geschützte Seiten deaktivieren (`no-store`) | gering |
| 8 | User-Agent aus `BuildConfig.VERSION_NAME` ableiten | trivial |

---

## Nachprüfung „real person data" in den Test-Dumps (Korrektur)

Der ursprüngliche Audit-Punkt „committed real PII" in `app/src/test/resources/dumps/`
wurde nachträglich exakt geprüft und **als False Positive korrigiert**:

- Alle 67 Dumps enthalten **ausschließlich virtuelle Online-Soccer-Spieldaten**:
  generierte Spieler-/Manager-/Vereinsnamen, Gehälter, Marktwerte, „Geburtstag = ZAT-Nummer".
- **Keine** E-Mail-Adressen, Logins, Passwörter, Session-/Cookie-Tokens oder andere
  personenbezogene Daten realer Personen (systematisch gesucht: 0 Treffer).
- Beispielbeleg: In `st902.html` erscheint „Oliver Schitthelm" sowohl als Manager (`writePM(4160)`)
  als auch als virtueller Spieler (`spielerinfo(152022)`) – das Spiel vergibt Namen generisch,
  Namen sind kein Realidentitätsmerkmal.
- Die Dumps liegen unter `src/test/resources` (nur für JVM-Unit-Tests) und gelangen
  **nicht in das APK** (empirisch mit `unzip -l` am Debug-APK verifiziert).
- Bewertung: **kein Datenschutz-/Security-Risiko**, kein Release-Blocker.

---

## Schlussfolgerung

Keine kritischen, verteilungsverhindernden Sicherheitslücken. Nach Umsetzung der Punkte 1–4
aus der Empfehlungsliste wird aus „B = Release möglich" ein sauberes „A = Release bereit".