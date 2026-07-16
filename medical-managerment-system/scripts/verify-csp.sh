#!/usr/bin/env bash
set -euo pipefail
BASE="${1:-http://localhost:9092}"
echo "==> 检查 $BASE 的 CSP 响应头"
CSP=$(curl -sI "$BASE/" | tr -d '\r' | grep -i '^content-security-policy:' || true)
if [ -z "$CSP" ]; then
  echo "  [FAIL] 未返回 Content-Security-Policy 头"
  exit 1
fi
echo "$CSP"
rc=0
for token in "unsafe-eval" "unsafe-inline" "*.alicdn.com" "*.amap.com" "*.autonavi.com"; do
  if echo "$CSP" | grep -q "$token"; then
    echo "  [OK]   $token"
  else
    echo "  [MISS] $token"; rc=1
  fi
done
exit $rc
