# Bericht: Spielbericht aus dem Saisonplan fremder Teams

Stand: 22.10.2026 · Analyse und Umsetzung

## 1. Auftrag und Symptom

Wird in der Ligatabelle ein fremdes Team geöffnet und dort unter
**Teaminformationen → Saisonplan** ein Spielbericht aufgerufen, zeigt die App
nicht den gewählten Bericht, sondern einen fremden Spielbericht.

Der Fehler betrifft den Weg:

```
Ligatabelle → fremdes Team (st.php?c=<teamId>)
            → Teaminformationen (st.php?c=<teamId>)
            → Saisonplan (st.php?s=6&c=<teamId>)
            → Bericht (bericht.php?s=…)
```

## 2. Ergebnis

Die Ursache liegt im Saisonplan-Parser. Er hat aus dem Link
`javascript:os_bericht(heim,gast,zat,saison)` **die komplette Argumentliste**
als `berichtUrl` gespeichert, z. B. `3449,709,1,24`. Die Navigation hat diesen
Wert anschließend als **Bericht-ID** (`sid`) an `bericht.php?s=…` übergeben.

`bericht.php` erwartet an dieser Stelle aber eine einzelne Bericht-ID, keine
`os_bericht`-Parameter. Damit wird nicht der ausgewählte Bericht geladen. Der
erste Wert der Liste ist die Heimteam-ID; wird die Zeichenkette serverseitig als
Zahl ausgewertet, landet man bei einem völlig anderen Bericht. Das erklärt die
Beobachtung „ein fremder Spielbericht wird angezeigt“.

Der Fehler ist damit nicht auf fremde Teams beschränkt. Bei eigenen Teams fällt
er nur weniger auf, weil der erste Wert dort die eigene Team-ID ist.

## 3. Belegstellen vor der Korrektur

1. **Falsche Extraktion** in `TeamRepository.parseSaisonplan`
   (`app/src/main/java/com/onlinesoccer/app/data/repository/TeamRepository.kt`):
   - Aus `javascript:os_bericht(3449,709,1,24)` wurde nur der Inhalt der Klammer
     extrahiert: `3449,709,1,24`.
   - Das Feld heißt zwar `berichtUrl`, enthielt aber keine URL.

2. **Falsche Verwendung** in `AppRoot`:
   - `onBerichtClick = { sid -> buildBerichtRoute(sid, null) }`
   - Der komma-separierte Wert wurde als `sid` in die Route
     `bericht?sid={sid}&url={url}` geschrieben.

3. **Fehlschlagende Auflösung** in `BerichtRepository.ladeBericht`
   (`BerichtRepository.kt:41-68`):
   - Bei gesetztem `sid` wird zuerst `bericht.php?s=<sid>` geladen.
   - Die bevorzugte statische Quelle
     `rep/saison/<saison>/<zat>/<heim>-<gast>.html` wird erst danach aus der
     Wrapper-Seite abgeleitet oder über `url` genutzt.
   - Mit `sid = "3449,709,1,24"` enthält die Wrapper-Seite nicht den gewählten
     Bericht.

Die übrigen Berichtsquellen machen es bereits richtig und bauen aus
`os_bericht(...)` direkt die statische URL:

- `DashboardRepository.parseBerichtUrl`
- `BewerbeRepository.osBerichtUrl`
- `InternationaleRepository.osBerichtUrl`

## 4. Umsetzung

### 4.1 Parser liefert jetzt eine echte Bericht-URL

`TeamRepository.parseSaisonplan` erwartet die vier Zahlen aus
`os_bericht(heim,gast,zat,saison)` und baut daraus:

```
https://os.ongapo.com/rep/saison/<saison>/<zat>/<heim>-<gast>.html
```

Leerzeichen innerhalb des Aufrufs werden toleriert. Nur wenn alle vier Werte
vorliegen, entsteht eine URL; unvollständige oder fremde Links bleiben `null`.

Beispiel aus dem Fixture `showteam_s6.html`:

| Link im Saisonplan | Neue `berichtUrl` |
| --- | --- |
| `os_bericht(3449,709,1,24)` | `https://os.ongapo.com/rep/saison/24/1/3449-709.html` |
| `os_bericht(283,3449,3,24)` | `https://os.ongapo.com/rep/saison/24/3/283-3449.html` |

### 4.2 Navigation übergibt die URL statt einer scheinbaren ID

In `AppRoot.kt:615-619` wird der Wert jetzt als `url` an
`buildBerichtRoute(null, url)` übergeben. `BerichtRepository.ladeBericht` nutzt
damit die statische Berichtsseite als erste Quelle. Der Umweg über
`bericht.php?s=…` entfällt.

### 4.3 Regressionstest angepasst

`TeaminformationenRepositoryParseTest` prüft nicht mehr die fehlerhafte
Argumentliste, sondern beide Fälle:

- Heimspiel: `os_bericht(3449,709,1,24)` → statische URL mit `3449-709`.
- Auswärtsspiel: `os_bericht(283,3449,3,24)` → statische URL mit `283-3449`
  (Heim-/Gast-Reihenfolge bleibt erhalten).

## 5. Verifikation

```text
./gradlew :app:testDebugUnitTest
BUILD SUCCESSFUL
```

Der zuvor fehlschlagende Test wurde durch die neue Erwartung ersetzt und läuft
grün. Die übrige Testsuite ebenfalls.

## 6. Geänderte Dateien

| Datei | Änderung |
| --- | --- |
| `app/src/main/java/com/onlinesoccer/app/data/repository/TeamRepository.kt` | `os_bericht`-Argumente werden zur statischen Bericht-URL |
| `app/src/main/java/com/onlinesoccer/app/ui/AppRoot.kt` | Bericht-URL wird als `url` statt als `sid` übergeben |
| `app/src/test/java/com/onlinesoccer/app/data/repository/TeaminformationenRepositoryParseTest.kt` | Regressionstest für Heim- und Auswärtsspiel |

## 7. Grenzen der Prüfung

Die Analyse stützt sich auf den Quellcode und die vorhandenen HTML-Fixtures.
Eine Live-Prüfung des Serververhaltens von `bericht.php?s=<kommawert>` wurde
nicht durchgeführt. Die Korrektur vermeidet diesen Pfad vollständig und nutzt
die gleiche statische Berichtsquelle, die bereits im Dashboard, in den
nationalen Bewerben und in den internationalen Bewerben funktioniert.
