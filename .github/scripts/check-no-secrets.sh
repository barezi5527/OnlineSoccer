#!/usr/bin/env bash
# Prüft, dass keine Zugangsdaten im Versionsstand liegen.
#
# Zwei Prüfungen:
#   1. Verbotene Dateinamen      – Secret-Dateien dürfen nicht versioniert sein
#   2. Secret-Muster im Inhalt  – bekannte Token-Formate und Passwort-Zuweisungen
#
# Nur versionierte Dateien werden geprüft (git ls-files). Die eigenen Dateien
# des Repositories sind ausgenommen, damit die Prüfung sich nicht selbst trifft.
#
# Aufruf: .github/scripts/check-no-secrets.sh
# Beendet mit 0, wenn alles sauber ist, sonst mit 1.

set -euo pipefail

status=0

# --- 1. Verbotene Dateinamen -------------------------------------------------

# Muster nach Git-Eignung, absichtlich als ERE. .gitignore allein genügt nicht:
# es greift nur bei adds aus dem Arbeitsbaum, nicht bei `git add -f` oder wenn
# eine Datei einmal schon im Index stand.
forbidden='^(keystore\.properties|release\.keystore|local\.properties|\.os_credentials)$|\.(jks|keystore|p12|pfx|jceks|pem|key|asc|gpg)$|(^|/)\.env(\.|$)|(^|/)credentials|(^|/)secrets?\.(properties|local\.properties)$|google-services\.json$|GoogleService-Info\.plist$'

mapfile -t forbidden_hits < <(
  git ls-files | grep -Ei "$forbidden" || true
)

if [ ${#forbidden_hits[@]} -gt 0 ]; then
  echo "::error::Verbotene Secret-Dateien im Versionsstand:"
  printf '  %s\n' "${forbidden_hits[@]}"
  status=1
else
  echo "ok  keine verbotenen Secret-Dateien versioniert"
fi

# --- 2. Secret-Muster im Inhalt ---------------------------------------------

# Treffer in diesen Pfaden sind erwartet und werden nicht als Fehler gewertet:
#   .github/scripts/check-no-secrets.sh  – die Prüfung selbst
#   app/src/test/                        – Test-Fixtures, unpersonenbezogen
#   *.md                                 – Doku beschreibt Muster, enthält keine
skip='^\.github/scripts/check-no-secrets\.sh$|^app/src/test/|\.md$'

mapfile -t files < <(git ls-files)

scan_failures=0
for file in "${files[@]}"; do
  if printf '%s' "$file" | grep -qE "$skip"; then
    continue
  fi
  # Binärdateien überspringen
  if ! grep -Iq . "$file" 2>/dev/null; then
    continue
  fi

  if matches=$(grep -nEi \
      -- '(-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36,}|xox[abprs]-[A-Za-z0-9-]{10,}|AIza[0-9A-Za-z_-]{35}|sk-[A-Za-z0-9]{32,})' \
      "$file"); then
    echo "::error::Secret-Muster in $file"
    printf '%s\n' "$matches" | sed 's/^/  /'
    scan_failures=1
  fi

  # Passwort-/Token-Zuweisung an einen Literalwert in Anfuehrungszeichen.
  # Ohne Anfuehrungszeichen bewusst nicht gematcht: `password = MutableStateFlow(...)`
  # ist Kotlin-Code, kein Geheimnis.
  if matches=$(grep -nEi \
      -- '(password|passwd|pwd|pw|pass|secret|api[_-]?key|auth|auth[_-]?token|access[_-]?token|bearer|client[_-]?secret|private[_-]?key)\s*[=:]\s*["'"'"'][^"'"'"']{6,}["'"'"']' \
      "$file"); then
    echo "::error::Passwort- oder Token-Zuweisung mit Literalwert in $file"
    printf '%s\n' "$matches" | sed 's/^/  /'
    scan_failures=1
  fi
done

if [ "$scan_failures" -eq 0 ]; then
  echo "ok  keine Secret-Muster im Inhalt"
else
  status=1
fi

# --- Ergebnis ----------------------------------------------------------------

if [ "$status" -eq 0 ]; then
  echo "Prüfung bestanden: keine Zugangsdaten im Versionsstand."
else
  echo "::error::Es wurden Zugangsdaten gefunden. Nicht committen – Werte rotieren."
fi

exit "$status"