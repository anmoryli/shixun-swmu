#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="${CI_OUTPUT_DIR:-${ROOT_DIR}/ci-output}"
STAGE_DIR="${OUTPUT_DIR}/stage"
PACKAGE_PATH="${OUTPUT_DIR}/medicine-cicd.tar.gz"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: required command not found: $1" >&2
    exit 2
  }
}

require_command tar

if [[ "${CI_PACKAGE_ONLY:-0}" != "1" ]]; then
  require_command java
  require_command mvn
  require_command node
  require_command npm

  echo "== Toolchain =="
  java -version
  mvn -version
  node --version
  npm --version

  echo "== Backend: unit tests and package =="
  mvn -B -ntp -f "${ROOT_DIR}/medical-backend/pom.xml" clean verify

  echo "== Frontend: reproducible install and production build =="
  npm --prefix "${ROOT_DIR}/medical-managerment-system" ci --no-audit --no-fund
  npm --prefix "${ROOT_DIR}/medical-managerment-system" run build
else
  echo "== Package existing CodeArts build outputs =="
fi

rm -rf "${OUTPUT_DIR}"
mkdir -p "${STAGE_DIR}/backend" "${STAGE_DIR}/web/dist" \
  "${STAGE_DIR}/api-tests" "${STAGE_DIR}/codearts"

JAR_PATH="$(find "${ROOT_DIR}/medical-backend/target" -maxdepth 1 -type f \
  -name 'medical-backend-*.jar' ! -name '*.original' ! -name '*-sources.jar' \
  ! -name '*-javadoc.jar' | head -n 1)"
test -n "${JAR_PATH}" || {
  echo "ERROR: backend JAR was not produced" >&2
  exit 3
}

test -f "${ROOT_DIR}/medical-managerment-system/dist/index.html" || {
  echo "ERROR: frontend dist/index.html was not produced" >&2
  exit 3
}

cp "${JAR_PATH}" "${STAGE_DIR}/backend/app.jar"
cp "${ROOT_DIR}/medical-backend/docker-entrypoint.sh" "${STAGE_DIR}/backend/docker-entrypoint.sh"
cp "${ROOT_DIR}/ci/runtime/backend.Dockerfile" "${STAGE_DIR}/backend/Dockerfile"
cp -R "${ROOT_DIR}/medical-managerment-system/dist/." "${STAGE_DIR}/web/dist/"
cp "${ROOT_DIR}/ci/runtime/web.Dockerfile" "${STAGE_DIR}/web/Dockerfile"
cp "${ROOT_DIR}/ci/runtime/nginx.conf" "${STAGE_DIR}/web/nginx.conf"
cp "${ROOT_DIR}/ci/runtime/compose.ci.yaml" "${STAGE_DIR}/compose.yaml"
cp "${ROOT_DIR}/api-tests/run_api_tests.py" "${STAGE_DIR}/api-tests/run_api_tests.py"
cp "${ROOT_DIR}/ci/codearts/deploy.sh" "${STAGE_DIR}/codearts/deploy.sh"
cp "${ROOT_DIR}/ci/codearts/api-test.sh" "${STAGE_DIR}/codearts/api-test.sh"
chmod 755 "${STAGE_DIR}/backend/docker-entrypoint.sh" "${STAGE_DIR}/codearts/"*.sh

COMMIT_ID="$(git -C "${ROOT_DIR}" rev-parse --short=12 HEAD 2>/dev/null || printf 'unknown')"
BUILD_ID="${CODEARTS_BUILD_NUMBER:-${BUILD_NUMBER:-local}}"
cat >"${STAGE_DIR}/BUILD-INFO" <<EOF
commit=${COMMIT_ID}
build=${BUILD_ID}
created_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
EOF

tar -czf "${PACKAGE_PATH}" -C "${STAGE_DIR}" .
tar -tzf "${PACKAGE_PATH}" >/dev/null
echo "Build package: ${PACKAGE_PATH}"
