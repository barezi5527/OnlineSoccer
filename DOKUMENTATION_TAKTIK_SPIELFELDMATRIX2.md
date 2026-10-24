# Dokumentation: Taktik-Spielfeldmatrix

Die Matrix gehört zur **inoffiziellen Android-App Online Soccer**. Dieses Dokument hält das sichtbare Layout, die Koordinaten und die geplante Anbindung an den ZAT-Bereich fest.

## Zweck und aktueller Stand

Im Taktik zeigt die Karte „Vorgeschlagener Kader“ die aus einem tatsächlichen Spielbericht übernommene Formation als 15×11-Matrix. Die Markierungen zeigen die Spieler des Kader-Vorschlags auf den gemeldeten Feldkoordinaten. Die Matrix ist dort eine Vorschau.

Der ZAT-Bereich besitzt bereits ein interaktives Feld (`FeldView` in `ZugabgabeScreen.kt`). Das Taktik kann außerdem einen geprüften Vorschlag für den ZAT vorbereiten und nach Bestätigung übertragen. Bei einer späteren UI-Integration soll der Matrix-Vorschlag im ZAT-Bereich mit dessen bestehender Aufstellung und Bearbeitung verbunden werden, statt ein zweites Koordinatensystem einzuführen.

## Sichtbares Raster

| Eigenschaft | Festlegung |
| --- | --- |
| Feld | 15 Zeilen × 11 Spalten |
| UI-Spalten | `1` bis `11`, von links nach rechts |
| Interne Spalten | `0` bis `10`, von links nach rechts |
| UI-Zeilen | `O` bis `A`, vom gegnerischen Tor oben zum eigenen Tor unten |
| Interne Zeilen | `0` bis `14`, von oben nach unten |
| Torwart | Separate Zeile `T` unterhalb des Feldes, mittig (Spalte 6 / Index 5) |
| Feldspieler-Buchstaben | Die im Raster aufgedruckten Server-Buchstaben, üblicherweise aus `A`–`L` |
| Ersatzbank | `U` = Ersatztorwart, `V`–`Z` = fünf Feldspieler |

Umrechnung der Feldkoordinaten:

- UI-Zeile als Buchstabe: `'O' - zeile` (Index `0` wird `O`, Index `14` wird `A`).
- UI-Spalte: `spalte + 1`.
- Rückrichtung: `zeile = 'O' - zeilenBuchstabe`, `spalte = uiSpalte - 1`.

Die Taktik-Vorschau hinterlegt den oberen Bereich (Zeilen 0–4) als Sturmzone, die mittleren Zeilen (5–9) als Mittelfeldzone und den unteren Bereich (10–14) als Abwehrzone. Die Spielerplättchen selbst sind nach Spielerposition eingefärbt. Die Farben sind Darstellung; maßgeblich für die Übertragung sind die Koordinaten und die aktuelle Server-Zuordnung der Buchstaben.

## Beispiel aus der sichtbaren Vorschau

Das auf dem verbundenen Gerät gezeigte Raster war eine 3-4-3-Aufteilung. Die Positionen sind ein konkreter Screenshot-Zustand und keine global feste Formation:

| Buchstabe | UI-Zeile | UI-Spalte | Interne Koordinate `(zeile, spalte)` |
| --- | ---: | ---: | --- |
| A | N | 8 | `(1, 7)` |
| B | M | 4 | `(2, 3)` |
| C | K | 8 | `(4, 7)` |
| D | J | 3 | `(5, 2)` |
| E | H | 6 | `(7, 5)` |
| F | F | 2 | `(9, 1)` |
| G | F | 10 | `(9, 9)` |
| H | D | 6 | `(11, 5)` |
| K | C | 3 | `(12, 2)` |
| L | C | 9 | `(12, 8)` |
| T | separate Torwartzeile | 6 | `AufstellungSlot.Torwart` |

Die Buchstaben sind keine verlässliche Spieler-ID und können je nach aktuellem ZAT-Raster anders auf Koordinaten abgebildet sein. Spieler müssen über ihre IDs (`pid`) zugeordnet werden.

## Datenfluss

1. `TaktikAuswertung.formationPositionen()` liest für jeden Feldspieler Buchstabe, Linie und Koordinate aus der echten Spielberichts-Aufstellung. Es wird nur eine vollständige Feldaufstellung mit zehn gültigen Positionen übernommen.
2. `kaderVorschlag()` teilt den Kader nach Torwart, Abwehr, Mittelfeld und Sturm auf. Die Spieler einer Linie werden nach den bestehenden Auswahlregeln den berichteten Koordinaten dieser Linie zugewiesen.
3. `VorschlagFeldMatrix()` zeichnet diese Positionen im Taktik. Das UI zeigt die 15 Zeilen, 11 Spalten sowie die Buchstaben; Torwart und Ersatzbank werden separat dargestellt.
4. `ZugabgabeRepository.ladeAufstellung()` liefert mit `Aufstellung.rasterPositionen` die **aktuelle** Zuordnung zwischen Server-Buchstaben und Feldkoordinaten.
5. `erstelleZugabgabeAufstellung()` übersetzt die vorgeschlagenen Koordinaten zurück in die aktuellen Server-Buchstaben (`raSlot`) und erzeugt die ZAT-Slots. Die Zuordnung darf nicht anhand fest verdrahteter Buchstaben angenommen werden.

## Voraussetzungen für die spätere ZAT-Integration

- Für die zehn Feldspieler müssen genau zehn eindeutige und gültige Koordinaten vorliegen (Zeilen `0..14`, Spalten `0..10`).
- Torwart und Ersatztorwart bleiben getrennte Slots; der Ersatztorwart belegt `U`.
- Der ZAT-Vorschlag muss insgesamt 17 verschiedene Spieler enthalten: Torwart, zehn Feldspieler, Ersatztorwart und fünf Feldspieler auf `V`–`Z`.
- Die Spieler-IDs müssen in der aktuellen ZAT-Spielerliste vorkommen und dort für den Wettbewerb spielberechtigt sein.
- Ein Vorschlag mit unvollständigen Bericht-Koordinaten wird nicht übertragen. Bei veraltetem oder nicht passendem Server-Raster muss die Übertragung abbrechen, nicht raten.
- Für die Oberfläche den vorhandenen `FeldView` und `Aufstellung.rasterPositionen` im ZAT-Bereich weiterverwenden. Die Taktik-Matrix ist aktuell eine Vorschau, kein eigener Editor.
- Die bestehende Vorschau-/Bestätigungs- und Teamwechsel-Prüfung des Taktik beibehalten; Server-Schreibaktionen dürfen erst nach ausdrücklicher Bestätigung erfolgen.

## Zugehörige Implementierung

- `app/src/main/java/com/onlinesoccer/app/feature/taktiklabor/TaktikLaborScreen.kt` – Vorschau-Matrix `VorschlagFeldMatrix()`.
- `app/src/main/java/com/onlinesoccer/app/data/repository/TaktikLaborAuswertung.kt` – Formation, Koordinatenzuordnung und Übertragung in `Aufstellung`.
- `app/src/main/java/com/onlinesoccer/app/data/repository/TaktikLaborRepository.kt` – ZAT-Vorschau und bestätigte Übertragung.
- `app/src/main/java/com/onlinesoccer/app/feature/zugabgabe/ZugabgabeScreen.kt` – bestehendes interaktives ZAT-Feld `FeldView()`.
- `app/src/main/java/com/onlinesoccer/app/data/model/Aufstellung.kt` – `AufstellungSlot`, `RasterPosition` und aktuelle Aufstellungsdaten.
