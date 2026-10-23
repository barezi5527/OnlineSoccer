# Bericht für 24.10.2026 – Fix OSC-/OSE-Qualifikation (Rückspiel-Ergebnis)

## Problem
In der OSC-/OSE-Qualifikation listet die Website beide Spiele einer Paarung in **Paarungsreihenfolge** (Team1 : Team2), nicht in Heim : Gast. 
Der Berichtslink des **Rückspiels** dreht die Reihenfolge um: `os_bericht(gastId, heimId, zat, saison)`. 
Der Parser zeigt deshalb für das Rückspiel aktuell fälschlich das Ergebnis in der verkehrten Perspektive (z. B. `3:0` statt `0:3`), weil er das Ergebnis nicht entsprechend der Paarungsreihenfolge dreht.

## Betroffene Stellen
- `app/src/main/java/com/onlinesoccer/app/data/repository/InternationaleRepository.kt`
  - `parseSpiele(...)` ordnet Spiele anhand `berichtTeams` zu Paarungen zu
  - `ergebnisInPaarungsreihenfolge(...)` / `tauscheErgebnis(...)` sollen das Ergebnis drehen, wenn das Spiel in umgekehrter Reihenfolge zur Paarung (Team A : Team B) zugeordnet wurde
- Test: `app/src/test/java/com/onlinesoccer/app/data/repository/InternationaleRepositoryParseTest.kt`
  - `rueckspielErgebnisWirdAufHeimUndGastGedreht()` erwartet, dass das Rückspiel `heim/gast` auf Paarungslogik gedreht wird und das Ergebnis entsprechend getauscht ist
- Fixture: `app/src/test/resources/dumps/oscq_ergebnisse_paarungsreihenfolge.html` (wiederhergestellt)

## Ergebnis (24.10.2026)
- `ergebnisInPaarungsreihenfolge(...)` wird nur noch für Qualifikationen angewandt
  und dreht das Ergebnis, wenn der Berichtslink Heim/Gast gegenüber der Paarung
  vertauscht. Der Vergleich läuft über die Team-IDs, ersatzweise über die Namen.
- Fixture und Test auf die tatsächliche Website-Semantik abgestimmt:
  Ergebnisse stehen in Paarungsreihenfolge, der Sieger ist per `<b>` markiert.
   Für die Paarung A:B (`2:3` und roh `3:0`) wird im Rückspiel B gegen A `0:3`
   angezeigt; für C:D (`1:1` und roh `0:2`) im Rückspiel D gegen C `2:0`.
- `./gradlew :app:testDebugUnitTest` und `:app:lintDebug` laufen grün.
- Geräteprüfung auf MI 8: Die echte Paarung `SCR Bitola : Eendracht Niklaas`
  zeigt Hinspiel `2:3`, Rückspiel `0:3` und „Weiter: SCR Bitola“ – der Sieger
  passt zur gedrehten Ergebnisanzeige.

## Notizen
- Issue #12 („OSC-/OSE-Qualifikationsspielen“) wurde bereits als behoben markiert, aber der zugehörige Test/Fixture ging beim Aufräumen verloren und bricht jetzt den Build. 
- Taktik-Labor untracked entfernt (war testweise, nicht zu committen).
