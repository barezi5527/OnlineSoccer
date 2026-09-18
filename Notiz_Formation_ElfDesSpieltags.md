# Notiz: Formation „Elf des Spieltags" – warum immer 3-4-3?

**Stand:** 2026-09-18

## Beobachtung
Stichproben in unterschiedlichen Ländern/Ligen zeigten bei der „Elf des Spieltags" immer die Formation **3-4-3**.

## Antwort: kein Zufall, nicht hartcodiert
Die Formation wird **dynamisch** aus den Daten gewählt. `ElfAuswahl.erstelleElf`
wählt aus 10 klassischen Formationen diejenige mit der höchsten Gesamtsumme der
bestbewerteten Spieler je Position.

- Logik: `app/src/main/java/com/onlinesoccer/app/data/repository/ElfBewertung.kt`
  - `FORMATIONEN` (Reihenfolge = Bevorzugung bei Gleichstand), L546-557
  - Auswahl-Schleife (maximiert die Summe), L622-639
  - Bei Gleichstand gewinnt die frühere Formation (strikes `>`).

## Warum systematisch 3-4-3?
- 3-4-3 schlägt 4-3-3, sobald der **4.-beste Mittelfeldspieler** besser bewertet
  ist als der **4.-beste Verteidiger** – im typischen Bericht sind Mittelfeld/
  Sturm meist höher bewertet als die Abwehr.
- 3-4-3 schlägt 3-5-2, weil der 5. Mittelfeldspieler schwächer wäre als der
  3. Stürmer.
- 4-Abwehr-Formationen verlieren, da der zusätzliche Verteidiger den schwächsten
  gewählten Mittelfeldspieler/Stürmer ersetzt.

## Fazit
3-4-3 ist das Ergebnis der Summen-Maximierung mit den üblichen Bewertungsmustern,
nicht fest eingestellt. Fallback ohne vollständige Formation: bestbewertete 11
Spieler (`vollstaendig = false`).

## Optionen (falls gewünscht)
- Andere Präferenz/Gewichtung der Formationen
- Kader-typische Formation eines Teams verwenden