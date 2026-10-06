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

# Le rendu doit être identique sur toutes les machines et dans la CI : on
# interdit Graphviz (le résultat dépend de sa version) et on utilise le moteur
# embarqué. Les diagrammes qui ne sont pas de séquence doivent donc contenir
# "!pragma layout smetana".
export GRAPHVIZ_DOT=/nonexistent/dot

if [ ! -f "$JAR" ]; then
    mkdir -p "$(dirname "$JAR")"
    curl -fsSL -o "$JAR" \
        "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml/$PLANTUML_VERSION/plantuml-$PLANTUML_VERSION.jar"
fi

rm -rf "$OUT"
mkdir -p "$OUT"
java -Djava.awt.headless=true -jar "$JAR" -tsvg -failfast2 \
    -o "$OUT" "$SRC"/*.puml

# PlantUML génère des identifiants de filtre aléatoires : on les remplace par des
# identifiants stables pour que deux rendus identiques donnent des fichiers
# identiques (sinon chaque rendu produit un diff dans git).
for svg in "$OUT"/*.svg; do
    perl -0pi -e '
        my %map; my $n = 0;
        s/(<filter\b[^>]*\bid=")([^"]+)"/$map{$2} = "filter" . ++$n; "$1$map{$2}\""/ge;
        s/url\(#([^)]+)\)/exists $map{$1} ? "url(#$map{$1})" : "url(#$1)"/ge;
    ' "$svg"
done

echo "Diagrammes générés dans $OUT :"
ls "$OUT"
