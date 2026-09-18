# Anforderungen: Bereich „Trainingsempfehlung“ (Team)

Stand: 17.09.2026 · Status: **zurückgestellt** (Machbarkeitsbericht: `Machbarkeitsbericht_Trainingsempfehlung.md`)

Aufbewahrung der Anforderungen für die spätere Umsetzung. Der Inhalt ist wortgetreu übernommen aus dem Auftrag.

## A – Allgemein

- Neuer Bereich **„Trainingsempfehlung“** unter **Team**.
- Menüpunkt **zwischen „Trainer“ und „Taktik-Editor“**.
- Analyse für **jeden Spieler des aktuellen Kaders individuell**: welches Training nach den bekannten Kriterien/Vorgaben von OnlineSoccer (OS) voraussichtlich den höchsten Trainingserfolg erzielt.
- Zusätzlich pro Spieler anzeigen, **welcher Trainertyp bzw. welche Trainereigenschaften** für das empfohlene Training geeignet wären.
- **Wichtig:** Die App soll **keinen Trainer automatisch auswählen oder verpflichten**. Die Trainerentscheidung bleibt vollständig beim Manager, damit durch eine automatische Auswahl kein finanzieller Schaden entstehen kann.

## B – Analyse pro Spieler

- Auswertung der vorhandenen Spielerkarte.
- Ausschließlich Daten und Regeln, die aus OS bekannt bzw. in der App bereits verfügbar sind.
- Zu berücksichtigende Kriterien (insbesondere):
  - Alter
  - aktuelle Stärke
  - Einzelwerte/Fähigkeiten
  - Position
  - Trainingszustand bzw. vorhandene Trainingsdaten
  - Fitness
  - Moral
  - bisherige Entwicklung
  - ggf. weitere für das OS-Training relevante Werte
- Ergebnis pro Spieler: Training mit der **höchsten prognostizierten Wahrscheinlichkeit für einen erfolgreichen Trainingseffekt**.

## C – Darstellung (pro Spieler eine Karte)

Aufbau der Spielerkarte:

**Spielername**
Position | Alter | Stärke

**Trainingsempfehlung:**
🏋️ Zweikampf

**Erfolgschance:**
z. B. **82 %**

**Alternative:**
Kondition – 71 %

**Begründung:**
Kurze verständliche Erklärung, warum dieses Training für diesen Spieler empfohlen wird.

**Trainer:**
Empfohlene Trainereigenschaft bzw. benötigter Schwerpunkt für dieses Training.

- Die Darstellung darf **nicht suggerieren, dass die Berechnung eine Garantie ist**. Es handelt sich um eine **OS-basierte Empfehlung/Prognose**.

## D – Gesamtübersicht

Ober- oder unterhalb der Spielerlisten eine Zusammenfassung:

**Empfohlenes Training für den Kader**

z. B.:
- Zweikampf → 5 Spieler
- Kondition → 3 Spieler
- Technik → 2 Spieler
- Schuss → 1 Spieler

Ziel: Manager erkennt sofort, welches Training für den gesamten Kader aktuell besonders sinnvoll erscheint.

## E – Training übernehmen

- Am Ende der Seite ein deutlich sichtbarer Button: **„Empfehlungen ins Training übernehmen“**.
- Beim Auslösen werden die ermittelten Trainingsempfehlungen **pro Spieler in den bestehenden Trainingsbereich übertragen**.
- Dabei gilt:
  - Keine automatische Trainerwahl
  - Keine Trainerverpflichtung
  - Keine finanziellen Aktionen
  - Keine Änderung von Verträgen
  - Keine endgültige Zugabgabe
  - Vor der Übernahme eine Bestätigung anzeigen
- Beispieltext der Bestätigung: **„Die Trainingsempfehlungen für 18 Spieler werden in den Trainingsbereich übertragen. Der Trainer muss anschließend von dir selbst ausgewählt werden.“**
- Erst nach Bestätigung werden die Empfehlungen übernommen.

## F – Rahmenbedingungen

- Bestehende Trainingsfunktion **nicht neu implementieren oder unnötig verändern**; vorhandene Trainingslogik und UI soweit möglich nutzen.
- Neue Funktion liefert lediglich: analysieren, Empfehlungen erzeugen, in den bestehenden Trainingsbereich übertragen.
- **Keine Annahmen über OS-Regeln treffen.** Zuerst prüfen, welche OS-Trainingsregeln und Berechnungsgrundlagen bereits im Projekt vorhanden sind. Falls Werte oder Regeln nicht zuverlässig verfügbar sind: **nicht erfinden, sondern im Code entsprechend kenntlich machen.**
- Architektur, bestehende Funktionen, Parser, Login, Cache und andere Bereiche der App dürfen durch diese Erweiterung **nicht beschädigt oder unnötig verändert** werden.

## G – Verknüpfte Referenz

- Machbarkeitsbericht (inkl. Datenlage, bekanntes OS-Wissen, Risiken, Architekturvorschlag, Aufwand): `Machbarkeitsbericht_Trainingsempfehlung.md`