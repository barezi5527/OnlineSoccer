# Maßnahmeplan Update-Funktion (Update-Hinweis + Download, Installation durch Nutzer)

**Feature:** Die App prüft **1× pro Woche** die GitHub-Releases, zeigt einen Hinweis
„Update verfügbar" und bietet einen **Download-Button** an. Die **Installation macht der
Nutzer selbst** über die System-Download-Benachrichtigung.

**Ausdrücklicher Nicht-Bestandteil:** Die App startet den Android-Installer **nicht selbst**,
kein `REQUEST_INSTALL_PACKAGES`, kein `FileProvider`, keine Kosten.

**Kernaussage der Recherche:** Machbar **ohne jede neue Berechtigung** — über den
`DownloadManager`. Details siehe „Der Schlüssel" unten.

**Arbeitsweise:** Jeder Schritt dauert max. 30 Minuten, einzeln abarbeiten. Nach jedem Schritt:
Haken setzen, `./gradlew :app:testDebugUnitTest` und `:app:assembleDebug` laufen lassen,
ggf. committen.

**Aktueller Stand:** ☐ offen / ☑ erledigt — **noch nichts umgesetzt.**
Offen zur Entscheidung: Banner vs. Dialog (siehe Schritt U7) und Developer-Verification
(siehe „Offene Produktentscheidung").

---

## Verifizierte Ausgangslage (nicht nochmal prüfen)

Alles live geprüft am 02.10.2026:

| Sachverhalt | Wert |
|---|---|
| Release-Repo | `barezi5527/OnlineSoccer-Download`, **öffentlich**, API antwortet mit `200` |
| Aktuelles Release | Tag `v1.0.0`, `prerelease: false` |
| Version im Code | `versionCode = 1`, `versionName = "1.0.0"` (`app/build.gradle.kts:26-27`) |
| Git-Tags | **keine** vorhanden |
| `buildConfig` | **ist bereits aktiviert** (`app/build.gradle.kts:65`) — `BuildConfig.VERSION_NAME` geht sofort, kein Build-Umbau nötig |
| `BuildConfig`-Nutzung im Code | **null** Vorkommen in `.kt`/`.xml` |
| Manifest-Berechtigungen | nur `INTERNET` + `ACCESS_NETWORK_STATE` — mehr braucht das Feature **nicht** |
| `FileProvider` | **nicht vorhanden**; `app/src/main/res/xml/` ist ein leeres Verzeichnis |
| `canRequestPackageInstalls()` | kommt im Projekt nirgends vor |
| `usesCleartextTraffic` | `false` — passt, GitHub ist HTTPS |
| Update-/Version-/About-Code | **existiert nicht** (Suche nach `update`/`version`/`about`/`changelog`/`PackageManager`: kein Treffer) |
| Strings | `res/values/strings.xml` enthält **nur** `app_name`; App-Texte sind hart im Kotlin (deutsch) |
| Persistenz | SharedPreferences + `EncryptedSharedPreferences`; **kein Room** |
| `USER_AGENT`-Drift | `OsApi.kt:14` = `"OnlineSoccerApp/0.1"`, passt nicht zu `versionName 1.0.0` — steht als offener Punkt im `Sicherheitsaudit_OnlineSoccerApp.md` |

### Die Versionsquelle liefert alles mit — live bestätigt

`GET https://api.github.com/repos/barezi5527/OnlineSoccer-Download/releases/latest`

```json
"tag_name": "v1.0.0",
"assets": [
  { "name": "checksums.txt",                "size": 97,
    "digest": "sha256:c0d0ee0f0b97a5abdeedafa624ca6f81792a665ea6eba90a55ed757dbcc8be88" },
  { "name": "OnlineSoccer-1.0.0-release.apk", "size": 15878401,
    "digest": "sha256:f243b676c94b282e6a500326e119f428b5b4a33acc1e962f4623036a5cceea68",
    "browser_download_url": "https://github.com/.../releases/download/v1.0.0/OnlineSoccer-1.0.0-release.apk" }
]
```

- **Kein eigenes Manifest-File nötig** — Version, URL, Größe und SHA-256 kommen alle aus der API.
- **Dateiname im Release ist versionsabhängig** (`OnlineSoccer-1.0.0-release.apk`). Deshalb
  **nicht** die `.../releases/latest/download/<name>`-URL hartkodieren, sondern immer
  `browser_download_url` aus der API nehmen. Nur so findet der Check spätere Releases automatisch.
- `digest` (SHA-256 pro Asset) ist neu in der GitHub-API; `checksums.txt` existiert zusätzlich
  bereits als Release-Asset und ist der Fallback, falls `digest` mal fehlt.
- **Keine Authentifizierung nötig** — 60 Requests/h pro IP reichen bei 1 Check/Woche um Größenordnungen.
- **Kein CDN-Problem:** `browser_download_url` liefert `302` auf
  `release-assets.githubusercontent.com`, dort `200` mit `content-length: 15878401`
  (live mit `curl -L` geprüft). Der `DownloadManager` folgt Redirects.

---

## Der Schlüssel: `DownloadManager` statt eigener Installer-Aufruf

### Warum die App den Installer nicht selbst aufrufen darf (AOSP, geprüft)

`frameworks/base` → `packages/PackageInstaller/src/.../InstallStart.java`:

```java
final boolean isIntentInstall =
    Intent.ACTION_VIEW.equals(intentAction)
 || Intent.ACTION_INSTALL_PACKAGE.equals(intentAction);
isTrustedSource = (!isIntentInstall && isInstallPkgPermissionGranted) || isPrivilegedAndKnown;
```

Und `PackageInstallerActivity.handleUnknownSources()`:

```java
final int appOpCode = mAppOpManager.permissionToOpCode(
        Manifest.permission.REQUEST_INSTALL_PACKAGES);
switch (mAppOpMode) {
  case AppOpsManager.MODE_DEFAULT:
      mAppOpManager.setMode(appOpCode, mOriginatingUid, AppOpsManager.MODE_ERRORED);
  case AppOpsManager.MODE_ERRORED:
      showDialogInner(DLG_EXTERNAL_SOURCE_BLOCKED);   // "Diese Quelle darf keine Apps installieren"
```

**Folgerung:** Sobald die App selbst den Installer startet — auch nur per `ACTION_VIEW`
mit APK-Mimetype — ist die App der Aufrufer, nicht privilegiert, und
`OP_REQUEST_INSTALL_PACKAGES` wird erzwungen. Ein `<uses-permission>` allein genügt nicht,
der Nutzer muss die Option zusätzlich pro App freischalten (`canRequestPackageInstalls()`).

### Warum `DownloadManager` der Ausweg ist (AOSP, geprüft)

`packages/providers/DownloadProvider` → `OpenHelper.buildViewIntent()` — das ist der Code,
der beim Antippen der Download-Benachrichtigung läuft:

```java
final String documentUri = DocumentsContract.buildDocumentUri(
        Constants.STORAGE_AUTHORITY, String.valueOf(id));
final Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setDataAndType(documentUri, mimeType);
intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION);
if ("application/vnd.android.package-archive".equals(mimeType)) {
    // genau dafür diese Extras: der Absender bleibt nachvollziehbar
    intent.putExtra(Intent.EXTRA_ORIGINATING_URI, remoteUri);
    intent.putExtra(Intent.EXTRA_REFERRER, getRefererUri(context, id));
    intent.putExtra(Intent.EXTRA_ORIGINATING_UID, mOriginatingUid);
}
```

Aufrufer ist der **System-Downloads-Provider**, der privilegiert ist ⇒ Prüfung entfällt.
Die App ist danach nur noch der Anstoßgeber.

### Ergebnis: keine neuen Berechtigungen

| Permission / Baustein | nötig? | Begründung |
|---|---|---|
| `REQUEST_INSTALL_PACKAGES` | ❌ | Installer wird vom System-Downloads-Provider gestartet |
| `FileProvider` + `res/xml/file_paths.xml` | ❌ | kein `content://` aus der App heraus |
| `POST_NOTIFICATIONS` | ❌ | die Benachrichtigung postet `com.android.providers.downloads` |
| `WRITE_EXTERNAL_STORAGE` | ❌ | Standard-Download-Ziel, keine App-spezifische Datei nötig |
| `canRequestPackageInstalls()`-Navigation | ❌ | komplett gegenstandslos |

Das Manifest bleibt bei `INTERNET` + `ACCESS_NETWORK_STATE`.

**Nebenwirkung, die wir brauchen:** Der `DownloadManager` läuft im System-Prozess und macht
mit dem Download weiter, wenn die App geschlossen wird. Damit entfällt das größte technische
Risiko des ursprünglichen Wunsches (ein `viewModelScope`-Download stirbt beim Wechsel in den
Hintergrund). Fortschrittsanzeige und Resume kommen ebenfalls gratis mit.

---

## Ablauf (Entwurf)

```
App-Start
  └─ Gate: letzter Check < 7 Tage?  (SharedPreferences "update_check_ts")
        ├─ ja  → nichts tun, ggf. nur lokales „bekannte Version" anzeigen
        └─ nein → GET api.github.com/.../releases/latest
                    ├─ semver-Vergleich tag_name vs. BuildConfig.VERSION_NAME
                    ├─ kein Update / Fehler → Hinweis ausblenden, ts setzen (kein Retry-Flapping)
                    └─ Update  → Update-Info (Version, Datum, Asset-Größe, digest)
                                  └─ Banner „🔄 Update verfügbar – Online Soccer 1.0.4"
                                       „Jetzt herunterladen"  → DownloadManager.enqueue()
                                                                    (Asset-Name, keine Custom-Destination)
                                       „Später" / weg  → gemerkte Version vormerken
                                 → System-Benachrichtigung "Online Soccer 1.0.4"
                                      → Nutzer tippt → System-Installer → "Installieren"
App-Neustart nach Installation: versionCode ist jetzt höher, Latest ist nicht mehr neuer
                                 → Banner „Installiert – bitte neu starten" (Neustart-Button)
```

**Kein `BroadcastReceiver` auf `ACTION_DOWNLOAD_COMPLETE` mit eigenem `startActivity()`.**
Das würde die App wieder zum Aufrufer machen und die ganze Konstruktion kippen (→ Gründe
in „Verworfene Alternativen"). Die App überwacht den Download-Status höchstens **lesend**, um
den Button zu deaktivieren bzw. „wird heruntergeladen …" zu zeigen.

---

## Risiken

| # | Risiko | Gewicht | Gegenmaßnahme |
|---|---|---|---|
| R1 | **Android Developer Verification** blockiert die Installation (siehe unten) | **hoch** | außerhalb der Code-Umsetzung, Produktentscheidung |
| R2 | `versionCode` ist auf `1` **fest verdrahtet** (`app/build.gradle.kts:26`) — bei 1.0.0 installiert und 1.0.4 angeboten mit weiterhin `versionCode 1` ⇒ `INSTALL_FAILED_VERSION_DOWNGRADE`, ein **stiller** Fehler | **hoch** | U1: `versionCode` je Release erhöhen; als `versionCode` **plus** Versionsprüfung vor dem Release-Checklisteneintrag |
| R3 | Signatur muss identisch sein. Ein Release mit Debug-Key ist für installierte Nutzer **tot**; Downgrade geht ohne ADB nicht | **hoch** | Release immer mit `release.keystore` signieren. Prüfschritt: `apksigner verify --print-certs` im Release-Check |
| R4 | Kein Rollback: Downgrade ohne ADB unmöglich ⇒ Nutzer stecken bei defekter Version | mittel | Release Notes ehrlich; ältere APK im Release-Text verlinken lassen (Notausgang). Kein Code-Gegenmittel |
| R5 | `browser_download_url` wird aus der API geliefert ⇒ theoretisch könnte eine manipulierte Antwort auf einen Fremdhost zeigen. HTTPS + `usesCleartextTraffic="false"` schützt, aber die **Host-Whitelist schützt erst** | mittel | U2: gegen `api.github.com` / `github.com` / `release-assets.githubusercontent.com` prüfen, nur `https` |
| R6 | Update-Hinweis verpufft, wenn die App 3–4 Wochen nicht geöffnet wird | mittel | bewusst akzeptiert: Hinweis erscheint beim nächsten Start. Kein WorkManager (keine neue Abhängigkeit, kein Akku-Verbrauch). Optional später (siehe „Spätere Ausbaustufen") |
| R7 | GitHub-API-Rate-Limit 60/h pro IP unauthentifiziert | gering | 1 Check/Woche ist um Größenordnungen darunter; `X-RateLimit-Remaining` prüfen, bei 403 nicht retryen, ts setzen |
| R8 | Manche ROMs (Samsung, einige asiatische) blockieren den Download oder zeigen zusätzliche „AutoUpdate"-Dialoge | gering | ist ein reines Gerätephänomen; beim Testgerät und 2–3 Fremdgeräten prüfen |
| R9 | Session liegt in `EncryptedSharedPreferences` ohne Migrationsmechanismus — benennt eine Version den Key um, ist der Nutzer ausgeloggt | gering | bestehende Keys (`cookie_lc`, `cookie_os`, `last_email`) in keiner Version umbenennen |
| R10 | Play-Protect-Warnung beim Sideloading („nicht aus dem Play Store") schreckt Nutzer ab | gering | SHA-256 (aus dem `digest`) im Hinweis anzeigen; Release-Text enthält ihn bereits |

**Wichtig zu R3/R10:** Weil die App den Installer **nicht** selbst startet, wird die
Signaturprüfung von Android selbst durchgesetzt. Ein manipuliertes APK mit falschem Key
scheitert an `INSTALL_FAILED_UPDATE_INCOMPATIBLE` und wird gar nicht erst installiert. Die
Signatur ist das harte Tor; die SHA-256-Prüfung ist nur Defense-in-Depth (und über
`DownloadManager` nicht mehr bequem selbst prüfbar, weil die Datei nicht im App-Verzeichnis
liegt). Das ist kein Nachteil, sondern eine Vereinfachung.

---

## Offene Produktentscheidung: Android Developer Verification

**Unabhängig vom Update-Feature und unabhängig vom Preis — betrifft auch heute schon den
GitHub-Download.** Stand 02.10.2026 läuft die Einführung:

| Zeitpunkt | Was |
|---|---|
| **30.09.2026** (2 Tage her) | Brasilien, Indonesien, Singapur, Thailand: Apps von registrierten Entwicklern nötig, auf zertifizierten Geräten mit Android 7+. **Gilt für Updates genauso wie für Neuinstallationen.** |
| **2027** | weltweite Ausweitung auf alle zertifizierten Geräte |

Wörtlich aus Googles FAQ: *"Unregistered apps can only be installed or updated when the
advanced flow is enabled or by using ADB — if the advanced flow is disabled, updates to
unregistered apps will fail."*

Ein „Jetzt aktualisieren"-Button, der für betroffene Nutzer in einer Fehlerschleife landet,
ist schlechter als kein Button. Deshalb **vor** dem Bau von U3–U6 klären:

| Weg | Kosten | Folge |
|---|---|---|
| **Android Developer Console, Voll-Distribution** | **$25 einmalig** + Identitätsnachweis | Updates laufen überall ohne Reibung. Paketname `com.onlinesoccer.app` + SHA-256-Fingerprint des `release.keystore` registrieren |
| **Limited-Distribution-Account** (Hobbyist) | **gratis**, kein Ausweisdokument | **max. 20 Geräte**, jeder Nutzer muss die Einladung manuell annehmen ⇒ skaliert nicht |
| **Nichts tun** | 0 | Nutzer in den vier Ländern brauchen ADB oder den „Advanced Flow" (Developer Options + Reboot + 24 h Wartezeit) |

Quellen: `developer.android.com/developer-verification`, `.../guides/faq`,
`android-developers.googleblog.com` (30.03.2026).

**Für diese App ist das eine Produktfrage, keine Codefrage** — deshalb hier nur dokumentiert.

---

## Umsetzungsschritte

- [ ] **U0** Entscheidung Developer Verification treffen (siehe oben). Ab U3 sinnvoll.
- [ ] **U1** `versionCode`-Vergabe festlegen: bei jedem Release erhöhen und die Erhöhung in
      die Release-Checkliste aufnehmen. Optional `versionCode` aus `versionName` ableiten,
      damit ein Vergessen unmöglich ist.
- [ ] **U2** `core/update/UpdateRepository.kt` (neu)
      - eigener `@Singleton` OkHttp-Client **ohne** `CookieJar` (Session-Cookies dürfen
        nie an GitHub gehen) — als zweite Binding nach dem `@ToggleClient`-Vorbild in
        `core/network/NetworkModule.kt` (z.B. Qualifier `@UpdateClient`)
      - `GET https://api.github.com/repos/barezi5527/OnlineSoccer-Download/releases/latest`
      - parsen: `tag_name`, `prerelease` (ignorieren), `assets[].browser_download_url`,
        `.name`, `.size`, `.digest`
      - **Host-Whitelist**: nur `https` und `api.github.com` / `github.com` /
        `release-assets.githubusercontent.com`
      - Asset wählen: `.apk`, bevorzugt `OnlineSoccer-*-release.apk`
      - Fehler → `IOException`, wie in allen anderen Repositories (`PmRepository` etc.)
- [ ] **U3** `core/update/Version Vergleich` — semver-Vergleich (`1.0.10` > `1.0.9`, nicht
      String-compare!) + Unit-Test. Repo-Hinweis: `org.json` ist in `testImplementation`
      vorhanden, das passt für die JSON-Fixtures.
- [ ] **U4** `core/update/UpdateStore.kt` (neu) — SharedPreferences nach dem Muster von
      `core/ui/theme/ThemeState.kt`: `last_check_ts`, `bekannte_version` (zum Ausblenden des
      Banners nach Wegtippen), `heruntergeladen_version`. Kein Room.
- [ ] **U5** `feature/update/UpdateViewModel.kt` (neu) — `@HiltViewModel`,
      `MutableStateFlow<UpdateUiState>` (`Aus` / `Prueft` / `Verfuegbar(version, groesse, digest, url)`
      / `Laedt` / `Fehler(text)`), `viewModelScope`, deutsche Texte inline wie im Rest der App.
- [ ] **U6** `UpdateBannerCard` (neu) — einsetzen in `DashboardScreen.kt` `DashboardContent`,
      passend zwischen `TeamHeaderCard` (ab Zeile 205) und `ServerBereicheCard` (ab Zeile 280).
      Checkbox-Variante: **Banner** (Empfehlung) oder **Dialog** — s. U7.
- [ ] **U7** Entscheidung **Banner vs. Dialog** einbauen. Empfehlung: **dismissible Card**
      direkt nach `TeamHeaderCard`. Begründung: nicht modal (kein Login-Blocker), sichtbar beim
      eigentlichen Spielen statt beim Anmelden, wegklickbar ohne Folgen.
- [ ] **U8** „Jetzt herunterladen" → `DownloadManager.enqueue()`. **Kein**
      `BroadcastReceiver` mit eigenem `startActivity()` (siehe verworfene Alternativen).
      `setTitle("Online Soccer <Version>")`, `setDescription("Update wird heruntergeladen …")`,
      `setNotificationVisibility(VISIBILITY_VISIBLE_COMPLETE)`, **keine** Custom-Destination.
      Danach Hinweis „Download läuft — tippe auf die Download-Benachrichtigung, um zu
      installieren".
- [ ] **U9** Nachtrag im Verein-Hub: `ServerBereicheScreen.kt` Abschnitt „Einstellungen & Hilfe"
      (ab Zeile 103) um „App-Version / Update prüfen" ergänzen, plus
      `Routes.UPDATE` in `ui/AppRoot.kt` (`Routes`-Objekt ab Zeile 105) und ein `composable`
      im NavHost — damit der Hinweis auch ohne Dashboard erreichbar bleibt und manuell
      prüfbar ist (für Nutzer, die den 7-Tage-Gate nicht abwarten wollen).
- [ ] **U10** Versionsanzeige generell: `BuildConfig.VERSION_NAME` irgendwo sichtbar machen
      (About-Dialog oder Verein-Hub).
- [ ] **U11** `OsApi.USER_AGENT` (Zeile 14) aus `BuildConfig.VERSION_NAME` ableiten — schließt
      den offenen Punkt aus `Sicherheitsaudit_OnlineSoccerApp.md`.
- [ ] **U12** `Sicherheitsaudit_OnlineSoccerApp.md`: prüfen, ob die Ergänzungen (neuer
      Netzwerkpfad zu GitHub, `DownloadManager`) eingetragen werden müssen.

**Reihenfolge-Abhängigkeit:** U0 (Verifikation) ist eine Produktentscheidung ohne Codeanteil,
U1 sollte vor dem nächsten Release stehen, U2–U9 sind der eigentliche Bau.

---

## Abnahme

- [ ] `./gradlew :app:testDebugUnitTest` und `./gradlew :app:assembleDebug` sind grün.
- [ ] `AndroidManifest.xml` enthält **unverändert genau 2** `uses-permission` (Diff prüfen!).
- [ ] Kein `<provider>` und kein `res/xml/file_paths.xml` im Diff.
- [ ] Kein `startActivity` mit APK-Intent und kein `canRequestPackageInstalls()` im Code.
- [ ] Ein Check ohne verfügbares Update erzeugt **keinen** Banner und keinen Fehler.
- [ ] Netzwerkfehler beim Check → kein Banner, `last_check_ts` wird gesetzt (kein Flapping),
      Debug-Log, im UI stumm.
- [ ] Einmal-Check-Gate greift: zweiter App-Start innerhalb 7 Tage löst **keinen** Request aus.
- [ ] Bei GitHub-Ausfall (404/500/Timeout) erscheint **kein** Banner und die App bleibt bedienbar.
- [ ] Versionsvergleich: `1.0.10 > 1.0.9` (Unit-Test, kein String-compare).
- [ ] `1.0.4` gegen `versionCode 1` installiert ⇒ Update-Knopf, kein Downgrade-Fehler.
- [ ] **Gerätetest:** Update verfügbar → Banner → Download → System-Benachrichtigung →
      Antippen → System-Installer → Installation → App-Start zeigt neue Version, **kein** Banner.
- [ ] **Gerätetest:** Nach Wegtippen bleibt der Hinweis weg, bis eine neuere Version erscheint.
- [ ] Kein Download, wenn das Gerät offline ist (kein Retry-Flapping).

---

## Verworfene Alternativen (nicht wieder aufgreifen)

| Alternative | Warum verworfen |
|---|---|
| **App startet den Installer selbst** (`ACTION_INSTALL_PACKAGE` / `ACTION_VIEW` + `FileProvider` + `REQUEST_INSTALL_PACKAGES`) | AOSP `InstallStart`/`PackageInstallerActivity`: App als Aufrufer ⇒ nicht privilegiert ⇒ `OP_REQUEST_INSTALL_PACKAGES` erzwungen, Nutzer muss pro App freischalten. Braucht zusätzlich `FileProvider` + `res/xml/file_paths.xml` und hat auf API 30 den bekannten `onActivityResult`-Bug nach den Einstellungen. Bei einer unbeaufsichtigten Hobby-App zu viel Fummelfaktor. |
| **`BroadcastReceiver` auf `ACTION_DOWNLOAD_COMPLETE` → eigenes `startActivity(ACTION_VIEW)`** | Macht die App wieder zum Aufrufer des Installers und kippt damit die ganze Konstruktion (siehe oben). Genau die Falle, die die `DownloadManager`-Variante gerade vermeidet. |
| **Update-Hinweis per eigener Push-Notification** | Braucht `POST_NOTIFICATIONS` (Laufzeitabfrage ab API 33) und die Nutzer müssen zustimmen — zusätzliche Berechtigung und zusätzliche Abbruchstelle. Der System-Downloader zeigt seine eigene Benachrichtigung **ohne** jede Berechtigung. |
| **Eigener APK-Download via OkHttp in `filesDir`/`cacheDir`** | Datei im app-internen Verzeichnis lässt sich nicht an den Installer übergeben (StackOverflow-Konsens: interne Pfade scheitern, `content:` wird abgelehnt). Und der Download bricht mit dem Prozess ab. Beides umgeht `DownloadManager` kostenlos. |
| **`WorkManager` mit 7-Tage-Intervall** für die Prüfung | Funktioniert, aber kostet eine neue Abhängigkeit (`work-runtime-ktx`) + Hilt-Worker-Setup + Foreground-Notification (mit `POST_NOTIFICATIONS`) — alles nur, damit die Prüfung auch ohne App-Start läuft. Der Nutzer erfährt es ohnehin erst, wenn er die App öffnet, weil er dann auch handeln kann. Bei 1 Check/Woche ist der Start-Gate kostenlos und ausreichend. → siehe „Spätere Ausbaustufen". |
| **Eigenes `version.json` im Release-Repo** | Die GitHub-API liefert Version, URL, Größe und `digest` bereits vollständig. Eine zweite Quelle wäre ein zusätzlicher Pfad, der auseinanderlaufen kann. |
| **Hartkodierte `.../releases/latest/download/OnlineSoccer-<feste-version>.apk`** | Der Dateiname enthält die Versionsnummer. Die URL bricht beim nächsten Release, weil der Pfad nicht mehr existiert. Immer `browser_download_url` aus der API verwenden. |
| **Play-In-App-Updates / Firebase** | App ist bewusst nicht im Play Store (so dokumentiert in `Forumstext_OnlineSoccer_App.txt`). |
| **Update sofort beim Start ohne Gate** | Rate-Limit (60/h pro IP) unnötig belastet und 15,9 MB sinnlos mehrfach angeboten. |

---

## Spätere Ausbaustufen (nur falls später gewünscht)

- `WorkManager` mit 7-Tage-Intervall, damit der Hinweis auch **ohne** App-Start kommt
  (Kosten: neue Abhängigkeit + Foreground-Notification-Berechtigung).
- „Später"-Button mit „In 3 Tagen wieder erinnern".
- Versions-Historie/Changelog im Hub.
- SHA-256 des heruntergeladenen APKs im UI anzeigen (schafft Vertrauen gegenüber
  Play-Protect).
- Hinweis, wenn `versionCode` des installierten Pakets **höher** ist als das gefundene
  Release (Downgrade-Situation nach Handinstallation).
