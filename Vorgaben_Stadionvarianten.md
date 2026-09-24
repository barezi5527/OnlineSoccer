# Vorgaben: Stadionvarianten (Stadionausbau / Teaminformation)

Status: **freigegeben** · Umfang: Datenlogik + Geometrie + Zeichnung · keine Server-Änderungen

## Ziel

Der Stadionplan soll in **Stadionausbau** und **Teaminformation identisch** dargestellt
werden (Referenz = Stadionausbau). Statt einer einheitlichen Bauform entstehen
**20 unterscheidbare Bau-Typen (Archetypen)**, die einen realitätsnahen Ausbau
zeigen: Ein 90.000-Stadion sieht nicht aus wie ein 50.000-Stadion, und die Bauform
ändert sich spürbar, wenn der Verein aus- oder umbaut.

## Grundsätze

- **Determinismus:** Die Variante ist eine reine Funktion der bekannten
  Stadion-Gesamtdaten (Kapazität, Sitz-/Stehplätze, überdachte Anteile). Gleiche
  Daten → identischer Plan in beiden Screens.
- **Parität:** Beide Screens speisen denselben `StadionPlanView`; es gibt keinen
  zweiten Zeichenpfad.
- **Kein Extra-Parsing nötig:** Der Server liefert nur Gesamtwerte (kein
  Tribünen-Bruch). Die Bauform wird daraus abgeleitet – zusätzliche Felder
  (Anzeigetafel, Rasenheizung) bleiben ungenutzt, damit die Screens deckungsgleich bleiben.

## Datenbasis (Stichprobe, live vom Server)

- 90 deutsche Teams (Liga 1–5) abgerufen: **22.000 – 106.350 Plätze**, Median 50.650.
- Globales Minimum in kleinen Ligen: **15.000**.
- **Kein deutsches Stadion unter 20.000** → Bänder für „Regionalliga-Größe" (<20k) entfallen.
- Stehplätze: nur **31 %** der Teams (28/90); >40 % Stehanteil nur bei 2 Teams.
- Überdachung: Sitzbereiche zu **91 % voll überdacht**, 9 % teilüberdacht.
- `Stadiongrösse` = Sitz + Steh exakt (100 % verifiziert).

## Größenbänder (an OS-Realität angepasst)

| Band | Kapazität | deutscher Name | n (DE-Stichprobe) | Charakter |
|------|-----------|----------------|-------------------|-----------|
| A | < 30.000 | Kleines Stadion | 3 | einfach, offene Ecken, einrangig |
| B | 30–45.000 | Kompakt-Stadion | 27 | Ecken geschlossen, einrangig, VIP ab 35k |
| C | 45–60.000 | Mittel-Stadion | 27 | 2 Ränge Langseiten, Kurven einrangig, Gästeblock |
| D | 60–75.000 | Groß-Stadion | 15 | 2 Ränge überall, VIP im Mittelrang |
| E | > 75.000 | Mega-Stadion | 18 | 3 Ränge Langseiten, VIP über den Ring verteilt / Mittelrang |

## 20 Archetypen (5 Bänder × 4 Grundformen)

Die Grundform wird deterministisch aus den Stadionwerten gewählt (stabiler Seed aus
Kapazität + überdachten Anteilen + Stehplätzen). Alle 4 Formen sind in jedem Band möglich:

| Grundform | eckFaktor | Eigenschaft |
|-----------|-----------|-------------|
| Kasten | 1,0 | geschlossener, abgerundeter Bowl |
| Achteck | 0,55 | kantiger, fast achteckiger Bowl |
| Oval | 1,7 | deutlich stärker gerundetes (Oval-)Stadion |
| Offene Ecken | 1,0 (offen) | Eckabschlüsse entfallen → Laufbahn-Optik mit offenen Ecken |

Sondervarianten (ergänzen die Form, zählen zu den 20):
- **Riesenkurve** (Südkurve übergroß): nur mit Stehplätzen möglich, Band C–E.
- **Teildach**: wenn überdachte Anteile < Gesamtkapazität.

## VIP-Platzierung je Band

| Band | Modus |
|------|-------|
| A | kein VIP-Bereich (unter 35.000) |
| B / C | VIP als Streifen an der äußeren Kante der Haupttribüne |
| D | VIP im Mittelrang der Haupttribüne (mittig platziert) |
| E | VIP „über den Ring verteilt" (Mittelrang West + äußere Kanten Ost/Nord/Süd) **oder** Mittelrang |

## Ränge je Band

| Band | Rangzahl |
|------|----------|
| A / B | 1 |
| C / D | 2 |
| E | 3 |

## Umsetzung (Dateien)

1. `StadionPlan.kt` (Datenmodell + Logik):
   - Neu: `StadionBand`, `Grundform`, `VipPlatzierung`, `StadionVariante`.
   - Neu: `StadionPlanLogik.variante(plan)` – deterministisch, liefert Band, Form,
     VIP-Modus, Rangzahl, Riesenkurve, Teildach, Archetyp-Name (`bautypZeile`).
   - `lagengewichte(…)` erhält `riesenKurve`-Flag (Südkurve wird schwerer).
2. `StadionPlanGeometrie.kt`:
   - `berechneGeometrie(…)` erhält die Variante; `eckRadius` wird mit `eckFaktor`
     skaliert; bei „Offene Ecken" entfallen `kurven` (Eckbereiche).
   - `ZoneStreifen` erhält eine Position (`AUSSEN` / `MITTELRANG`); VIP-Streifen werden
     je Modus auf einer oder mehreren Seiten erzeugt.
   - `PlanGeometrie` trägt `rangzahl` + `variante` für die Zeichnung.
3. `StadionPlanView.kt`:
   - Bautyp-Zeile über dem Plan („Bautyp: …").
   - Ränge: Trennlinien im Sitzband bei Rangzahl ≥ 2; row-Dichte steigt mit Rangzahl.
4. Tests: `StadionPlanLogikTest.kt` + `StadionPlanGeometrieTest.kt` erweitern.

## Verifikation

- `./gradlew :app:testDebugUnitTest` (Logik- und Geometrie-Tests, offline-fähig).
- Beide Screens rendern denselben Determinismus: gleiches `StadionPlanDaten` →
  gleiches `PlanGeometrie`.