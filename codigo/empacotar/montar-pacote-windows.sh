#!/bin/bash
# Monta o pacote portatil para Windows (dois cliques, sem instalar nada):
#   Java de Windows (jlink) com JavaFX dentro + os jars do projeto + codigo + dados.
# Roda em Linux/macOS com JDK 21 e Maven. Uso:  ./montar-pacote-windows.sh [pasta-de-trabalho]
# Resultado: <pasta-de-trabalho>/projeto-diarios-javafx.zip
set -euo pipefail

JDK_VERSAO="21.0.5"
JDK_URL="https://download.bell-sw.com/java/${JDK_VERSAO}+11/bellsoft-jdk${JDK_VERSAO}+11-windows-amd64.zip"
JFX_URL="https://download2.gluonhq.com/openjfx/${JDK_VERSAO}/openjfx-${JDK_VERSAO}_windows-x64_bin-jmods.zip"

AQUI="$(cd "$(dirname "$0")/.." && pwd)"        # pasta codigo/
RAIZ="$(cd "$AQUI/.." && pwd)"                  # raiz do repositorio
TRABALHO="$(mkdir -p "${1:-$RAIZ/dist}" && cd "${1:-$RAIZ/dist}" && pwd)"
PACOTE="$TRABALHO/projeto-diarios"

mkdir -p "$TRABALHO/baixados"
baixar() { [ -s "$TRABALHO/baixados/$2" ] || curl -fSL -o "$TRABALHO/baixados/$2" "$1"; }
baixar "$JDK_URL" jdk-windows.zip
baixar "$JFX_URL" javafx-jmods-windows.zip

echo "1/5 extraindo os modulos do JDK e do JavaFX de Windows"
rm -rf "$TRABALHO/jdk-win" "$TRABALHO/jfx-win"
unzip -q "$TRABALHO/baixados/jdk-windows.zip" -d "$TRABALHO/jdk-win"
unzip -q "$TRABALHO/baixados/javafx-jmods-windows.zip" -d "$TRABALHO/jfx-win"

echo "2/5 jlink: Java de Windows enxuto, com JavaFX"
rm -rf "$PACOTE"
mkdir -p "$PACOTE/_sistema/lib"
jlink \
  --module-path "$(echo "$TRABALHO"/jdk-win/*/jmods):$(echo "$TRABALHO"/jfx-win/*)" \
  --add-modules java.se,jdk.httpserver,jdk.unsupported,jdk.crypto.ec,jdk.localedata,jdk.charsets,javafx.controls \
  --strip-debug --no-man-pages --no-header-files \
  --output "$PACOTE/_sistema/java"

echo "3/5 compilando os jars do projeto"
mkdir -p "$PACOTE/_sistema/lib" "$PACOTE/_sistema/tools"
(cd "$AQUI" && mvn -q package -DskipTests \
 && mvn -q dependency:copy-dependencies -DincludeScope=runtime -DexcludeGroupIds=org.openjfx \
    -DoutputDirectory="$PACOTE/_sistema/lib" \
 && mvn -q dependency:copy -Dartifact=org.junit.platform:junit-platform-console-standalone:1.11.3 \
    -DoutputDirectory="$PACOTE/_sistema/tools" -Dmdep.stripVersion=true)
cp "$AQUI"/target/diarios-oficiais-*.jar "$PACOTE/_sistema/diarios.jar"
(cd "$AQUI/target/test-classes" && jar cf "$PACOTE/_sistema/testes.jar" .)

echo "4/5 copiando codigo, dados e atalhos"
mkdir -p "$PACOTE/codigo" "$PACOTE/dados"
(cd "$AQUI" && tar --exclude=target --exclude=empacotar -cf - .) | (cd "$PACOTE/codigo" && tar xf -)
cp -r "$RAIZ/dados/diarios" "$PACOTE/dados/"
cp "$RAIZ/LEIA-ME.txt" "$RAIZ/ABRIR_PAINEL.bat" "$AQUI"/empacotar/ABRIR_JANELA.bat "$PACOTE/"
cp "$AQUI/empacotar/RODAR_TESTES.bat" "$PACOTE/_sistema/"

echo "5/5 compactando"
(cd "$TRABALHO" && rm -f projeto-diarios-javafx.zip && zip -qr projeto-diarios-javafx.zip projeto-diarios)
ls -lh "$TRABALHO/projeto-diarios-javafx.zip"
