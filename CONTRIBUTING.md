# Beitragen (CONTRIBUTING)

Schön, dass du mitmachst! Dieser Beitrag ist freiwillig und ohne
rechtliche Bindung im Sinne der DCO – schlicht die Etikette, die hier
zusammenpasst.

## Kurzfassung

1. Such zuerst ein offenes Issue oder eröffne eines, bevor du anfängst
2. Forke das Repository und lege einen Feature-Branch an
3. Prüfe mit `./gradlew :app:testDebugUnitTest`
4. Eröffne einen Pull Request und beschreibe, was du geändert hast und warum

## Erste Schritte

```bash
git clone https://github.com/dein-name/OnlineSoccer.git
cd OnlineSoccer
git checkout -b mein-feature
```

Arbeite **nie** direkt auf `main`. `main` bleibt immer baubar.

## Commits

- Ein logischer Schritt pro Commit
- Nachricht im Imperativ: `Teamwechsel-Zähler korrigieren` statt
  `Fixes` oder `fix`
- Wenn möglich auf das Issue verweisen: `Behebt #42`

## Vor dem Pull Request

Diese drei Befehle müssen sauber durchlaufen:

```bash
./gradlew :app:testDebugUnitTest     # 34 Testklassen
./gradlew :app:assembleDebug         # baut die App
./gradlew :app:lintDebug             # statische Analyse
```

**Das prüft die CI automatisch** – bei jedem Push und jedem Pull Request laufen
diese Schritte auf GitHub. Ein roter Status blockiert den Merge. Der
Lint-Bericht wird als Artefakt hochgeladen, sodass du ihn im PR herunterladen
kannst.

Die CI baut zusätzlich einen Release-Build **ohne Keystore**. Sie hat
absichtlich keinen Zugriff auf Signiermaterial: `permissions` steht auf
`contents: read`, es gibt keinen `secrets`-Block. Eine echte Signierung
erfolgt nur lokal beim Maintainer.

Wenn etwas nicht grün ist, komm trotzdem – aber sag im PR dazu, was noch
nicht funktioniert.

### Zugangsdaten gehören niemals ins Repo

Vor jedem Commit prüft die CI automatisch, ob Secret-Dateien oder
Secret-Muster im Versionsstand liegen. Der Lauf ist lokal reproduzierbar:

```bash
./.github/scripts/check-no-secrets.sh
```

Die `.gitignore` deckt Keystore, Zertifikate, `.env` und
`google-services.json` ab. Eine eigene `keystore.properties` legst du nur lokal
an – sie wird nie committet.

## Code-Stil

- **Kotlin-Idiome**: `kotlin.code.style=official` ist gesetzt. Nutze
  suspending Functions statt `Thread`/`AsyncTask`.
- **Compose**: Aufteilung in kleine, wiederverwendbare Komponenten unter
  `ui/components`. Kein Riesen-`when` in einer `@Composable`.
- **Keine Geheimnisse**: Zugangsdaten, Tokens oder Keystore-Dateien gehören
  **niemals** ins Repository. `keystore.properties` ist bereits in
  `.gitignore`.
- **Keine neuen Tracking-Bibliotheken.** Das Projekt ist bewusst
  analytics- und trackingfrei – siehe `PRIVACY.md` der
  [Download-Seite](https://barezi5527.github.io/OnlineSoccer-Download/).
- Kommentare auf Deutsch, Code-Bezeichner auf Englisch (so ist das Projekt
  bisher aufgebaut).

## Parser ändern

Das Herzstück der App ist das Parsen der HTML-Seiten von os.ongapo.com.
Wenn du dort etwas änderst:

- Test-Fixtures liegen unter `app/src/test/resources/dumps/`
- Bestehende Tests sind `*RepositoryParseTest.kt` in
  `app/src/test/java/com/onlinesoccer/app/data/`
- Bitte **keine echten Zugangsdaten** oder personenbezogenen Daten in den
  Fixtures. Sie stammen aus Testläufen und sollen anonym bleiben.
- Wenn sich die Weboberfläche geändert hat, aktualisiere auch
  [`Analyse_OnlineSoccer.md`](Analyse_OnlineSoccer.md), dort ist die
  Seitenstruktur dokumentiert.

## Worum es sich beim Mitwirken lohnt

Einige Bereiche, in denen Feedback besonders willkommen ist:

| Bereich | Warum |
|---|---|
| **Barrierefreiheit** | TalkBack-Tests, Bedienbarkeit mit großer Schrift |
| **Übersetzungen** | Strings liegen derzeit fest in Deutsch vor |
| **Parser-Robustheit** | os.ongapo.com ändert sein Layout gelegentlich |
| **Tests** | Von 147 Kotlin-Dateien sind derzeit 34 Testklassen vorhanden |
| **Dokumentation** | Die Analyse-Dokumente im Repo-Root sind teils veraltet |

## Lizenz deiner Beiträge

Wenn du einen Pull Request eröffnest, stimmst du zu, dass dein Beitrag unter
der [GPL-3.0](LICENSE) lizenziert wird – dieselbe Lizenz wie das übrige
Projekt. Das ist die übliche „inbound = outbound"-Regel und bedeutet: Was du
beiträgst, bleibt offen.

## Fragen

Fragen zur Codebasis sind als Diskussion im zugehörigen Issue willkommen –
auch von Leuten, die noch nichts beitragen wollen.

Mitmachen heißt außerdem: Wir sind gegenseitig keine Dienstleister. Wenn
etwas nicht passt, sag es ruhig. Für den Umgang gilt der
[Code of Conduct](CODE_OF_CONDUCT.md).