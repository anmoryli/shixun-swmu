#!/usr/bin/env bash
set -Eeuo pipefail

ARTIFACT_PATH="${1:-./medicine-cicd.tar.gz}"
DEPLOY_ROOT="${2:-/opt/medicine}"
ENV_FILE="${MEDICINE_ENV_FILE:-/etc/medicine/medicine-ci.env}"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

command -v docker >/dev/null 2>&1 || fail "docker is required on the target host"
docker compose version >/dev/null 2>&1 || fail "docker compose v2 is required"
test -r "${ARTIFACT_PATH}" || fail "artifact not found: ${ARTIFACT_PATH}"
test -r "${ENV_FILE}" || fail "environment file not found: ${ENV_FILE}"

RAW_RELEASE_ID="${CODEARTS_BUILD_NUMBER:-${BUILD_NUMBER:-$(date -u +%Y%m%d%H%M%S)}}"
RELEASE_ID="$(printf '%s' "${RAW_RELEASE_ID}" | tr -cs 'A-Za-z0-9_.-' '-')"
RELEASE_DIR="${DEPLOY_ROOT}/releases/${RELEASE_ID}"
CURRENT_LINK="${DEPLOY_ROOT}/current"
PREVIOUS_DIR=""

if test -L "${CURRENT_LINK}"; then
  PREVIOUS_DIR="$(readlink -f "${CURRENT_LINK}" || true)"
fi

install -d -m 755 "${DEPLOY_ROOT}/releases"
test ! -e "${RELEASE_DIR}" || fail "release already exists: ${RELEASE_DIR}"
install -d -m 755 "${RELEASE_DIR}"
tar -xzf "${ARTIFACT_PATH}" -C "${RELEASE_DIR}"
test -r "${RELEASE_DIR}/compose.yaml" || fail "invalid artifact: compose.yaml is missing"

printf 'MEDICINE_IMAGE_TAG=%s\n' "${RELEASE_ID}" >"${RELEASE_DIR}/.release.env"

compose() {
  MEDICINE_IMAGE_TAG="${RELEASE_ID}" docker compose \
    --project-directory "${RELEASE_DIR}" \
    --env-file "${ENV_FILE}" \
    -f "${RELEASE_DIR}/compose.yaml" "$@"
}

rollback() {
  test -n "${PREVIOUS_DIR}" || return 0
  test -r "${PREVIOUS_DIR}/.release.env" || return 0
  PREVIOUS_TAG="$(sed -n 's/^MEDICINE_IMAGE_TAG=//p' "${PREVIOUS_DIR}/.release.env" | head -n 1)"
  test -n "${PREVIOUS_TAG}" || return 0
  echo "Rolling back to ${PREVIOUS_TAG}"
  MEDICINE_IMAGE_TAG="${PREVIOUS_TAG}" docker compose \
    --project-directory "${PREVIOUS_DIR}" \
    --env-file "${ENV_FILE}" \
    -f "${PREVIOUS_DIR}/compose.yaml" up -d --no-build --remove-orphans || true
}

compose config --quiet
if ! compose up -d --build --remove-orphans --wait --wait-timeout 180; then
  compose logs --tail 200 || true
  rollback
  fail "deployment did not become healthy"
fi

WEB_PORT="$(sed -n 's/^MEDICINE_WEB_PORT=//p' "${ENV_FILE}" | tail -n 1)"
WEB_PORT="${WEB_PORT:-9092}"
HEALTH_URL="http://127.0.0.1:${WEB_PORT}/actuator/health"
if command -v curl >/dev/null 2>&1; then
  curl --fail --silent --show-error --retry 6 --retry-delay 5 "${HEALTH_URL}" >/dev/null || {
    rollback
    fail "health check failed: ${HEALTH_URL}"
  }
else
  wget -q -O /dev/null "${HEALTH_URL}" || {
    rollback
    fail "health check failed: ${HEALTH_URL}"
  }
fi

ln -sfn "${RELEASE_DIR}" "${CURRENT_LINK}"
echo "Deployment healthy: ${RELEASE_ID} (${HEALTH_URL})"
