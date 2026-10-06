#!/usr/bin/env bash
# Rend les diagrammes PlantUML de docs/diagrams/*.puml en SVG dans
# docs/diagrams/out/. Échoue si un diagramme contient une erreur de syntaxe.
# Le jar PlantUML est téléchargé depuis Maven Central (nécessite Java).
set -euo pipefail

PLANTUML_VERSION="${PLANTUML_VERSION:-8059}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${PLANTUML_JAR:-$ROOT/target/tools/plantuml-$PLANTUML_VERSION.jar}"
SRC="$ROOT/docs/diagrams"
OUT="$SRC/out"

if [ ! -f "$JAR" ]; then
    mkdir -p "$(dirname "$JAR")"
    curl -fsSL -o "$JAR" \
        "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/$PLANTUML_VERSION/plantuml-$PLANTUML_VERSION.jar"
fi

rm -rf "$OUT"
mkdir -p "$OUT"
java -Djava.awt.headless=true -jar "$JAR" -tsvg -failfast2 \
    -o "$OUT" "$SRC"/*.puml

echo "Diagrammes générés dans $OUT :"
ls "$OUT"
