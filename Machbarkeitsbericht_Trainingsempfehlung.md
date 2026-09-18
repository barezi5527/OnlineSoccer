# Machbarkeitsbericht: Bereich „Trainingsempfehlung“ (Team)

Stand: 17.09.2026 · Basis: aktueller Code-Stand im Repo `OnlineSoccer` (Kotlin/Compose/Hilt).

## 1. Kurzfazit

**Der Bereich ist machbar**, ohne die bestehende Trainings- oder Trainerfunktion anzufassen.

- Die benötigten Spielerdaten (Alter, Position, Stärke, Opti, Fit, Mor, Einzelwerte, bisherige Trainingserfolge) sind im Projekt bereits **vollständig verfügbar** und geparst.
- Die Übertragung von Empfehlungen in den bestehenden Trainingsbereich kann **über die vorhandene POST-Logik** (`fuehreAktionAus`) und das vorhandene Trainings-Formularpattern (`tr1/tr2` + `trainingspeichern`) erfolgen – ohne die Trainingsseite neu zu implementieren.
- **Wesentliche Einschränkung:** Die echte OS-Trainings„Chance“ wird **serverseitig berechnet** und liegt im Projekt **nicht als Formel** vor. Sie ist nur auf der Trainingsseite sichtbar, **wenn** Trainer + Skill bereits gesetzt sind (sonst „0.00 %“). Die im UI geforderte „Erfolgschance in %“ kann daher **nur eine transparent gekennzeichnete App-Prognose sein**, keine OS-exakte Garantie.
- Die Pflicht „keine automatische Trainerwahl/keine finanzielle Aktion“ ist technisch sauber umsetzbar: Es werden **nur die „Trainierter Skill“-Werte** (`tr2`) pro Spieler vorbefüllt, der Trainer-Slot (`tr1`) bleibt auf „0/kein Trainer“.

---

## 2. Datenlage (Ist-Zustand im Code)

| Daten | Quelle (OS) | Struktur im Projekt | Verfügbar |
|---|---|---|---|
| Kader (Nr., Name, Alter, Position, Skillschnitt, Opt.Skill, Fit, Mor, Sonderfähigkeiten, Sperre) | `showteam.php?s=0` | `KaderSpieler` – `TeamRepository.parseKader` (`TeamRepository.kt:85–135`), Modell `KaderSpieler.kt:4` | ✅ |
| Einzelwerte (volle Skill-Map je Spieler) | `showteam.php?s=2` | `StaerkeZeile.werte` (header-basierte `Map<String,String>`) – `TeamRepository.parseStaerken` (`TeamRepository.kt:998–1018`), Modell `TeamDetails.kt:99` | ✅ |
| Trainingsseite: Trainer-Slot (`tr1<pid>`), trainierter Skill (`tr2<pid>`), OS-„Chance“, Trainer-Betreuung, Trainingsspeicher/Presets | `training.php` | `TeamRepository.parseTraining` (`TeamRepository.kt:415–548`); Dump `app/src/test/resources/dumps/training.html` | ✅ |
| Trainerstab: Skill-Vorgaben (62.5–99.5), Gehalt, Vertragslaufzeit, Legende „trainiert bis Skill X“ | `trainer.php` | `TeamRepository.parseTrainer` (`TeamRepository.kt:551–600`); Dump `dumps/trainer.html` | ✅ |
| Bisherige Entwicklung / Trainingserfolge je ZAT („Erfahrung gestiegen“, „Führungsqualität gestiegen“, …) | `zar.php` | `ZatReportRepository` → `ZatReportTraining` (`ZatRepository.kt:104–151`, `ZatReport.kt:20`) | ✅ (nur gewählter ZAT) |
| Zusammengeführte Spielerkarte | `s=0…4` + `sp.php` | `TeamRepository.spielerKarte(pid)` (`TeamRepository.kt:913–937`) | ✅ |

**Kein Datenmodell für einen eigenen „Trainingszustand“-Wert** (‚trainingszustand‘ in der Anforderung). Als „vorhandene Trainingsdaten“ stehen die aktuelle Trainingsseite (Trainer + Skill + OS-Chance) und die ZAT-Trainingserfolge aus `zar.php` zur Verfügung.

---

## 3. Bekannte OS-Trainingsregeln (im Projekt belegt)

Aus `dumps/training.html`, `dumps/trainer.html` und `Analyse_OnlineSoccer_Phase2.md §7/§8`:

- **Trainierbare Skills sind positionsabhängig** (`training.html:186–203` / `436–451`):
  - **Feldspieler:** `SCH` (Schießen), `BAK` (Bälleroberung), `KOB` (Körperkontakt), `ZWK` (Zweikampf), `DEC` (Defensive), `GES` (Geschwindigkeit) + allen gemeinsam `AGG`, `PAS`, `AUS`, `UEB`, `ZUV` → **11 Optionen**.
  - **Torwart:** `ABS` (Abstoß), `STS` (Stellungsspiel), `FAN` (Fangsicherheit), `STB` (Strafraumbeh.), `SPL` (Spiel auf der Linie), `REF` (Reflexe) + `AGG`, `PAS`, `AUS`, `UEB`, `ZUV` → **11 Optionen**.
  - **Hinweis:** In OS gibt es **keine** Kategorien wie „Kondition“, „Technik“ oder „Schuss“ als Trainingsform. Die Empfehlung sollte daher mit den **echten OS-Skill-Kürzeln** (zzgl. deutschem Anzeigenamen, z. B. „Zweikampf“ für `ZWK`) arbeiten – „Kondition“ ist dann z. B. `AUS`/`GES`.
- **Trainer-Slot**: `tr1<pid>` wählt einen Trainingsplatz („T 1 60“ … „T 6 60“ = 6 Einheiten à 60 min) – `training.html:168–184`, Phase2 §7.
- **Trainer-Skill-Legende** (`trainer.html:750–788`): Trainer „N“ trainiert bis zu einem bestimmten Maximal-Skill (Trainer 02 → bis Skill 62, … Trainer 17 → bis Skill 99). Diese Tabelle erlaubt eine **Trainer-Anforderungs-Empfehlung** („benötigter Trainer liefert mind. Skill X“).
- **Betreuungsregel** (`training.html:57–63`): „Ein oder mehr Trainer betreuen mehr als 5 Spieler… nur die ersten 5 werden trainiert.“ → max. 5 Spieler pro Trainingsplatz (für eine sinnvolle Verteilung relevant).
- **OS-„Chance“**: wird vom Server berechnet und nur bei gesetztem Trainer+Skill angezeigt; ohne Einstellung „0.00 %“ (`training.html:206`). **Die Berechnungsformel ist weder im Projekt noch öffentlich dokumentiert.**

---

## 4. Machbarkeit je Anforderung

### 4.1 Menüpunkt zwischen „Trainer“ und „Taktik-Editor“
**Machbar (gering).** Der Team-Hub rendert eine `HubTabs`-Leiste in `TeamScreen.kt:90–111` (aktuell: Mannschaft | Training | Trainer | **Taktik-Editor** | …). Ein neuer Eintrag „Trainingsempfehlung“ zwischen „Trainer“ und „Taktik-Editor“ ist eine Enum-Erweiterung (`TeamBereich`) + ein Tab-Eintrag. Die Seite ist **nativ/komputiert** (keine neue Website-Seite), ähnlich dem lokalen `TeamBereich.TAKTIK`-Zweig. Die Website-Navigation und die übrigen Tabs bleiben unberührt.

### 4.2 Analyse pro Spieler
**Machbar (geringe/mittlere Komplexität).** Bis auf „Trainingszustand“ sind alle geforderten Kriterien mit verifizierten Daten abgedeckt:
- Alter, Position, Stärke, Opti, Fit, Mor → `KaderSpieler` (s=0).
- Einzelwerte/Fähigkeiten (inkl. aller trainierbaren Skills) → `StaerkeZeile.werte` (s=2).
- Vorhandene Trainingsdaten → aktuelle `training.php`-Seite (Trainer, trainierter Skill, OS-Chance) + ZAT-Trainingserfolge (`zar.php`).
- Beide Quellen werden pro `pid` über die Kader-ID (aus `sp.php`-Links geparst) verknüpft; exakt dasselbe Muster nutzt bereits `spielerKarte()` (`TeamRepository.kt:913`).

### 4.3 Erfolgschance / Alternative / Begründung
**Machbar, aber mit Auflage (transparente Kennzeichnung).**
- Eine echte OS-Wahrscheinlichkeit ist **nicht berechenbar** (Formel unbekannt). Die Prozentzahl ist eine **App-Prognose auf Basis bekannter OS-Faktoren**:
  - Alter (Degression mit zunehmendem Alter),
  - Abstand Skill↔Opti (`opti`), Abstand des Einzelwerts zum Trainer-Limit,
  - Fitness (`fit`) und Moral (`mor`),
  - bisherige Trainingserfolge aus `zar.php` (Entwicklung),
  - Reihenfolge/„Lücken“ in den 18 Einzelwerten (Schwächwerte trainieren üblicherweise am besten).
- Die Gewichtung ist **keine OS-Regel**, sondern eine App-interne Heuristik und muss im Code **klar kenntlich** gemacht werden (Konstanten, Kommentare, Doku) – entsprechend der Vorgabe „keine Annahmen über OS-Regeln treffen / nicht erfinden, sondern kenntlich machen“.
- Die geforderte Optik („Die Darstellung soll nicht suggerieren, dass die Berechnung eine Garantie ist“) wird im UI durch Zusatztext „OS-basierte Prognose – keine Garantie“ erfüllt.
- Ergänzung: Für Spieler **mit bereits gesetzter** Trainings-Einstellung kann zusätzlich die **echte OS-Chance** aus der Trainingsseite angezeigt werden (bereits geparst, `TeamRepository.kt:494`).
- „Alternative“ = zweitbester Skill nach derselben Prognose.

### 4.4 Trainer-Empfehlung
**Machbar (gering).** Aus der Trainer-Legende (`trainer.php`) lässt sich je Spieler die **Mindest-Trainer-Skillstufe** ableiten, die nötig ist, um den empfohlenen Skill sinnvoll zu trainieren (z. B. „Trainer mit Skill ≥ 85 / Trainer 15“). Da **kein Trainer gewählt/verpflichtet** wird, ist dies eine reine Anzeige. Um die echte Kader-Situation widerzuspiegeln, kann die **Anzahl freier/belegter Trainingsplätze** aus der Trainingsseite (Spalte „Trainer“) mit ausgegeben werden.

### 4.5 Gesamtübersicht (Kader-Zusammenfassung)
**Machbar (gering).** Aggregation der Einzel-Empfehlungen („ZWK → 5 Spieler, …“) ist eine reine Zählung über die Empfehlungsliste.

### 4.6 „Empfehlungen ins Training übernehmen“
**Machbar (mittlere Komplexität), mit zu verifizierender Randbedingung.**
- Mechanismus vorhanden: `TeamRepository.fuehreAktionAus(ziel, felder)` (`TeamRepository.kt:207`) sendet POST an `training.php`; das Formularpattern (pro Spieler `tr1<pid>`/`tr2<pid>` + Button `trainingspeichern`) ist bereits geparst und getestet (`TeamSeitenParseTest.kt:288–298`, `ZeilenAktionenTest.kt`).
- Keine automatische Trainerwahl: Es werden **nur `tr2<pid>` = empfohlener Skill** gesetzt, `tr1<pid>` bleibt **„0“ (kein Trainer)**. Damit werden exakt die Vorgaben erfüllt (keine Trainerentscheidung, keine Verpflichtung, keine Finanzaktion, kein Vertrag).
- Vor dem POST erscheint ein **Bestätigungsdialog** (Muster existiert bereits: `AktionDialog.kt` / `sendeAktion` mit `dialogFormular` in `SeiteViewModel.kt:61–75`): „Die Trainingsempfehlungen für N Spieler werden in den Trainingsbereich übertragen. Der Trainer muss anschließend von dir selbst ausgewählt werden.“
- **Risiko (im Betrieb zu verifizieren):** Ob OS ein `trainingspeichern` mit `tr1=0` (kein Trainer) für alle Spieler vollständig akzeptiert und ob eine Skill-Vorbelegung ohne Trainer später korrekt weiterverwendet wird, ist serverseitig unklar (keine öffentliche Doku). Funktional treffen die gesetzten `tr2`-Werte aber genau die bestehende Datenstruktur. Der Code muss defensiv prüfen (Seite neu laden → gesetzte Werte verifizieren; bei Fehler Meldung statt stillschweigendem „Erfolg“).

### 4.7 Keine Beeinträchtigung bestehender Funktionen
**Machbar (gering).** Alles Neue liegt in einem eigenen Paket/Komponenten; `parseTraining`, `parseTrainer`, `SeiteScreen`, Aufstellung, Login, Cache und Navigation werden **nicht** verändert. Es wird nur ein zusätzlicher Tab in der bestehenden `HubTabs`-Leiste ergänzt.

---

## 5. Risiken & offene Punkte (für die Umsetzung zu klären)

1. **Prognose vs. Serverwahrheit:** „Erfolgschance in %“ ist eine transparente App-Schätzung. Erwartungsmanagement durch expliziten Hinweistext.
2. **Übernahme ohne Trainer:** Wirksamkeit/Validierung von `tr2` allein (ohne `tr1`) serverseitig ungesichert → Verifikationstest im echten Betrieb („Speichern geht, danach Seite neu laden“).
3. **Spalten-/Header-Kopplung der Einzelwerte:** `StaerkeZeile.werte` sind header-basiert (deutsche Spaltennamen). Das Mapping „Einzelwert-Spalte → trainierbarer Skill-Code“ (z. B. „Schießen“ → `SCH`/`tr2`-Wert) muss explizit und defensiv gemappt werden (fehlende/unbekannte Werte → Empfehlung überspringen statt raten).
4. **ZAT-Historie nur punktuell:** `zar.php` liefert Trainingserfolge je gewähltem ZAT, keine lückenlose Verlaufshistorie → „bisherige Entwicklung“ nur begrenzt nutzbar (als Kontextfaktor, nicht als Basis).
5. **ID-Lebensdauer:** Spieler-IDs sind saisonal volatil; nie hartkodieren (bereits Standard im Projekt, siehe Risiko-Liste Phase2 §20).
6. **Position:** Torwart-Skillliste ≠ Feldspieler-Skillliste → Empfehlungs-Engine muss pro `SpielerPosition` auswählen.
7. **Betreuungsobergrenze (5 Spieler/Trainer):** Für die Verteilung der Empfehlung auf die 6 Trainingsplätze beachten, sobald doch Trainer zugeordnet werden; in der reinen „Skill-Empfehlung“ spielt es nur bei der Trainer-Hinweiszeile eine Rolle.

---

## 6. Vorgeschlagene Architektur (Überblick, ohne bestehende Logik anzufassen)

- **Paket:** `feature/team/trainingsempfehlung` (oder `feature/trainingsempfehlung`).
- **Modell:** `TrainingsEmpfehlung` (pid, Name, Position, Alter, Skill, Empfehlung=[Skill-Code, Prognose-%, Begründung, Alternativ-Skill, Trainer-Anforderung]).
- **Engine (pure Funktion, gut testbar):** nimmt `KaderSpieler` + `StaerkeZeile` + aktuelle Training-Zeilen + ZAT-Trainingserfolge und erzeugt pro Spieler eine sortierte Skill-Bewertung. Heuristik-Konstanten **zentral und kommentiert** („App-intern, keine OS-Formel“).
- **Repository:** lädt parallel (async wie in `spielerKarte`) Kader, Stärken, Trainingsseite, ggf. Trainerstab; stellt `übernehme(empfehlungen)` bereit → baut Felder `tr2<pid>` + `trainingspeichern`, ruft `fuehreAktionAus("…/training.php", felder)`.
- **ViewModel/Screen:** analog `TeamViewModel`/`SeiteScreen`; Bestätigungsdialog vor Übertragung.
- **Tests:** Unit-Tests für die Engine (Determinismus, Positions-Skilllisten, Grenzwerte) + Parser-Weiterverwendung (`TeamSeitenParseTest` bleibt Bestand).

---

## 7. Aufwandsschätzung (grob)

| Baustein | Aufwand |
|---|---|
| Menüpunkt + Screen-Gerüst | ~ 0,5 Tag |
| Empfehlungs-Engine inkl. Kennzeichnung/Kommentare | ~ 1,5–2 Tage |
| Verknüpfung Kader/Stärken/Training/ZAT | ~ 0,5–1 Tag |
| Trainer-Anforderungslogik (Legende) | ~ 0,5 Tag |
| Übernahme (POST, Bestätigungsdialog, Rückmeldung) | ~ 1 Tag |
| Tests + Verifikation im Betrieb | ~ 1 Tag |
| **Summe (netto, ohne Review)** | **≈ 5–6 Tage** |

---

## 8. Empfehlung / nächste Schritte

- **Freigabe zur Umsetzung:** Jede Anforderung ist umsetzbar; die Prognose-Natur der „Erfolgschance“ wird im Code und im UI explizit gekennzeichnet.
- Vor/nach der Umsetzung **einmal real verifizieren**:
  1. Reagiert `training.php` korrekt auf `tr2`-set + `tr1=0` + `trainingspeichern`?
  2. Passt die Einzelwerte-Spalte auf der echten `s=2`-Seite zum Kader (Header-Mapping)?
- Optional, aber **nicht Teil der Anforderung**: reine Info-Anzeige der echten OS-„Chance“ für bereits eingestellte Spieler (Daten liegen schon vor).