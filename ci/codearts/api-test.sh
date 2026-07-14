#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_URL="${BASE_URL:-}"
WAIT_SECONDS="${API_WAIT_SECONDS:-180}"

test -n "${BASE_URL}" || {
  echo "ERROR: BASE_URL is required, for example http://ECS_PUBLIC_IP:9092" >&2
  exit 2
}
test -n "${ADMIN_USERNAME:-}" || {
  echo "ERROR: ADMIN_USERNAME must be provided as a secret parameter" >&2
  exit 2
}
test -n "${ADMIN_PASSWORD:-}" || {
  echo "ERROR: ADMIN_PASSWORD must be provided as a secret parameter" >&2
  exit 2
}

# Wait for the deployed service to become healthy before running API assertions.
deadline=$((SECONDS + WAIT_SECONDS))
until python3 - "${BASE_URL%/}/actuator/health" <<'PY'
import sys
import urllib.request

try:
    with urllib.request.urlopen(sys.argv[1], timeout=5) as response:
        raise SystemExit(0 if response.status == 200 else 1)
except Exception:
    raise SystemExit(1)
PY
do
  if (( SECONDS >= deadline )); then
    echo "ERROR: service was not healthy within ${WAIT_SECONDS}s" >&2
    exit 3
  fi
  sleep 5
done

export BASE_URL
export API_EVIDENCE_DIR="${API_EVIDENCE_DIR:-${ROOT_DIR}/api-test-results}"
python3 "${ROOT_DIR}/api-tests/run_api_tests.py"
