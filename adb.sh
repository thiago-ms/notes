#!/usr/bin/env bash
#
# adb.sh — roda o adb SEMPRE via Docker (imagem de build do projeto), com
# passthrough de USB. NUNCA instala nada no host.
#
# A imagem `notes-android-build:latest` já traz o Android SDK completo
# (adb em /opt/android-sdk/platform-tools/adb). Este script sobe um container
# de longa duração que mantém o servidor adb vivo e o reaproveita.
#
# Uso:
#   ./adb.sh devices                 lista os dispositivos
#   ./adb.sh authorize               sobe o servidor e espera você autorizar no celular
#   ./adb.sh install [caminho.apk]   instala o APK (padrão: mais recente em dist/)
#   ./adb.sh build-install           gera o APK debug (gradle distApk) e instala
#   ./adb.sh build-install-release   gera o APK release (distReleaseApk) e instala
#   ./adb.sh logcat                  segue os logs do app
#   ./adb.sh reset                   derruba o container e apaga a autorização (.adb/)
#   ./adb.sh <args...>               repassa qualquer comando ao adb (ex.: ./adb.sh shell date)
set -euo pipefail

IMAGE="notes-android-build:latest"
CONTAINER="notes-adb"
PKG="br.com.notes"

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ADBHOME="$PROJECT_DIR/.adb"   # chave adb persistente (fora do git)

blue()  { printf '\033[34m%s\033[0m\n' "$*"; }
green() { printf '\033[32m%s\033[0m\n' "$*"; }
red()   { printf '\033[31m%s\033[0m\n' "$*" >&2; }

# Sobe (ou reaproveita) o container com o servidor adb vivo e USB passado.
ensure_server() {
  mkdir -p "$ADBHOME"
  if docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
    return
  fi
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  blue ">> subindo container adb ($CONTAINER)..."
  docker run -d --name "$CONTAINER" --privileged \
    -v /dev/bus/usb:/dev/bus/usb \
    -v "$ADBHOME":/adbhome -e HOME=/adbhome \
    -v "$PROJECT_DIR":/workspace:Z -w /workspace \
    --entrypoint sh "$IMAGE" -c "adb start-server && sleep infinity" >/dev/null
  sleep 2
}

adb()   { docker exec "$CONTAINER" adb "$@"; }
state() { adb get-state 2>/dev/null | tr -d '\r'; }

require_authorized() {
  if [ "$(state)" != "device" ]; then
    red "Aparelho não autorizado (estado: '$(adb devices | awk 'NR==2{print $2}')')."
    red "Rode:  ./adb.sh authorize"
    exit 1
  fi
}

# Instala um APK; se a assinatura divergir da instalada, desinstala e reinstala.
install_apk() {
  local apk="$1" cpath out
  cpath="$(container_path "$apk")"
  blue ">> instalando $(basename "$apk")..."
  out="$(adb install -r "$cpath" 2>&1)"; echo "$out"
  if echo "$out" | grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE"; then
    red ">> assinatura divergente; desinstalando e reinstalando (apaga dados do app)..."
    adb uninstall "$PKG" >/dev/null 2>&1 || true
    adb install "$cpath"
  fi
  green ">> instalado."
}

# Converte um caminho de APK (absoluto sob o projeto, ou relativo) para o caminho
# de dentro do container (/workspace/...).
container_path() {
  local p="$1"
  case "$p" in
    "$PROJECT_DIR"/*) printf '/workspace/%s' "${p#"$PROJECT_DIR"/}" ;;
    /*)               red "APK precisa estar dentro do projeto ($PROJECT_DIR)"; exit 1 ;;
    *)                printf '/workspace/%s' "$p" ;;
  esac
}

cmd="${1:-devices}"; shift || true

case "$cmd" in
  authorize)
    ensure_server
    if [ "$(state)" = "device" ]; then green ">> já autorizado."; adb devices -l; exit 0; fi
    blue ">> No CELULAR (tela desbloqueada):"
    echo "   1. Opções do desenvolvedor → 'Revogar autorizações de depuração USB' → OK"
    echo "   2. Desligue e ligue a 'Depuração USB'"
    echo "   3. Modo USB = 'Transferência de arquivos (MTP)'"
    echo "   4. Desconecte e reconecte o cabo USB"
    blue ">> aguardando autorização (~2min)..."
    for _ in $(seq 1 40); do
      adb reconnect >/dev/null 2>&1 || true
      if [ "$(state)" = "device" ]; then green ">> AUTORIZADO!"; adb devices -l; exit 0; fi
      sleep 3
    done
    red ">> timeout: ainda não autorizado. Confira o pop-up no celular e rode de novo."
    exit 1
    ;;

  install)
    ensure_server; require_authorized
    apk="${1:-}"
    [ -z "$apk" ] && apk="$(ls -t "$PROJECT_DIR"/dist/*.apk 2>/dev/null | head -1 || true)"
    [ -z "$apk" ] && { red "Nenhum APK encontrado em dist/. Rode ./adb.sh build-install"; exit 1; }
    install_apk "$apk"
    ;;

  build-install)
    blue ">> gerando APK debug (gradle distApk via Docker)..."
    docker compose run --rm --user "$(id -u):$(id -g)" android \
      ./gradlew --no-daemon --console=plain distApk
    ensure_server; require_authorized
    install_apk "$(ls -t "$PROJECT_DIR"/dist/*-debug.apk | head -1)"
    ;;

  build-install-release)
    blue ">> gerando APK release (gradle distReleaseApk via Docker)..."
    docker compose run --rm --user "$(id -u):$(id -g)" android \
      ./gradlew --no-daemon --console=plain distReleaseApk
    ensure_server; require_authorized
    install_apk "$(ls -t "$PROJECT_DIR"/dist/*-release.apk | head -1)"
    ;;

  logcat)
    ensure_server; require_authorized
    pid="$(adb shell pidof -s "$PKG" 2>/dev/null | tr -d '\r' || true)"
    if [ -n "$pid" ]; then
      blue ">> logcat do app (pid $pid) — Ctrl+C para sair"
      adb logcat --pid="$pid"
    else
      blue ">> app não está rodando; mostrando logcat filtrado por '$PKG' — abra o app. Ctrl+C para sair"
      adb logcat | grep --line-buffered -i "$PKG"
    fi
    ;;

  reset)
    docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
    rm -rf "$ADBHOME"
    green ">> container derrubado e autorização (.adb/) apagada."
    ;;

  *)
    ensure_server
    adb "$cmd" "$@"
    ;;
esac
