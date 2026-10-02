# AGENTS.md

Arbeitsanweisungen für dieses Repository. Bei AI-gestützten Beiträgen verbindlich.

## Zweck

Quellcode der inoffiziellen Android-App **Online Soccer** (Paket
`com.onlinesoccer.app`) für das Spiel auf `os.ongapo.com`.

Der Quellcode ist seit dem 02.10.2026 **öffentlich** und steht unter der
**GPL-3.0** (siehe [LICENSE](LICENSE)). Beiträge aus der Community sind
willkommen – Einstieg über [CONTRIBUTING.md](CONTRIBUTING.md).

Das veröffentlichte Release-Artefakt (APK) und dessen Dokumentation liegen
getrennt im Repository `barezi5527/OnlineSoccer-Download`. Hier gehört nur
der Quellcode hin, keine Release-Artefakte.

## Zugangsdaten: niemals ins Repo

Verbindlich – weder im Arbeitsbaum noch in der Historie.

Nicht committen (`.gitignore` deckt das ab, die CI prüft zusätzlich):

- `keystore.properties`, `release.keystore`, `*.jks`, `*.p12`, `*.pfx`, `*.key`
- `.os_credentials`, `.env`, `.env.*`, `google-services.json`
- Tokens, Passwörter, API-Schlüssel, private Schlüssel

Lokal vor jedem Commit prüfbar:

```bash
./.github/scripts/check-no-secrets.sh
```

Die CI (`ci.yml`) hat bewusst **kein** Secret und **kein** Signiermaterial:
`permissions: contents: read`, kein `secrets`-Block. Ein Release-Build in der CI
ist deshalb immer debug-signiert. Die echte Signierung passiert ausschließlich
lokal auf dem Rechner des Maintainers.

## Inoffiziell-Kennzeichnung

Die App ist ein inoffizieller Client für `os.ongapo.com`. Es besteht keine
Verbindung zu den Betreibern des Spiels. Diese Kennzeichnung ist rechtlich und
kommunikativ wesentlich und darf nie wegfallen.

In der Dokumentation (README, Changelog, Release-Notizen) muss „inoffiziell"
enthalten sein. Die Form „Inoffiziell Online Soccer" ist falsch und darf nicht
verwendet werden. Der APK-Dateiname und das App-Label (`Online Soccer`) bleiben
bewusst ohne Zusatz.

## Qualitätsschwelle

Die CI verlangt bei jedem Push und jedem Pull Request:

```bash
./gradlew :app:testDebugUnitTest     # 34 Testklassen
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease       # debug-signiert, ohne Keystore
```

Ein roter Lauf blockiert den Merge (Branch-Schutz). Der Lint-Bericht wird als
Artefakt hochgeladen.

## Ton und Stil

- Deutsch, Du-Ansprache, sachlich, keine Werbesprache.
- Kommentare im Code auf Deutsch, Bezeichner auf Englisch.
- Sicherheitsaussagen nur mit Beleg. Keine Behauptung ohne Nachweis.
- Kotlin-Idiome statt `Thread`/`AsyncTask`; Compose-Komponenten klein halten.

## Abhängigkeiten und Datenschutz

- Keine Tracking-/Analytics-Bibliotheken. Das Projekt ist analytics- und
  trackingfrei.
- Neue Berechtigungen müssen in `AndroidManifest.xml` begründet und in der
  `SECURITY.md` des Download-Repos nachgezogen werden (aktuelle Liste: nur
  `INTERNET` und `ACCESS_NETWORK_STATE`).

## Parser

Das Herzstück parst HTML von `os.ongapo.com`. Bei Änderungen daran:

- Test-Fixtures unter `app/src/test/resources/dumps/` ergänzen oder
  aktualisieren.
- Keine echten Zugangsdaten oder personenbezogenen Daten in den Fixtures.
- Bestehende `*RepositoryParseTest`-Klassen grün halten.
- Seitenstruktur ist in `Analyse_OnlineSoccer.md` dokumentiert – bei
  Layoutänderungen dort nachziehen.

## Versionsschema

Semantic Versioning: MAJOR für inkompatible Änderungen, MINOR für neue
Funktionen, PATCH für Fehlerbehebungen. `versionName` und `versionCode` liegen
in `app/build.gradle.kts`; `versionCode` steigt bei jedem Release monoton.
