#!/usr/bin/env bash
# =========================================================================
# 慧医数字医疗应用系统 -- CI 复用的构建脚本
# 用途：在 CodeArts 流水线或本地 CI 上复用同一套构建命令，避免流水线配置
#       和 shell 脚本两套维护。
# 关键修复：原流水线 Maven 步骤在仓库根目录跑，找不到 pom.xml 而失败。
#       这里先把工作目录切到 medical-backend，再调用 mvn。
# 用法：bash deploy/scripts/ci-build.sh backend|frontend|all [SKIP_TESTS]
# =========================================================================
set -euo pipefail

PROJECT_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${PROJECT_ROOT}"

SKIP_TESTS="${2:-true}"
MVN_OPTS=(-B -e)
if [[ "${SKIP_TESTS}" == "true" ]]; then
  MVN_OPTS+=(-Dmaven.test.skip=true)
fi

phase() { printf '\n=== %s ===\n' "$*"; }

build_backend() {
  phase "后端构建 (medical-backend)"
  pushd medical-backend >/dev/null
  # pwd 一定打印，方便流水线日志里看清目录
  printf 'current dir: %s\n' "$(pwd)"
  [[ -f pom.xml ]] || { echo "ERROR: pom.xml 不在 $(pwd)" >&2; exit 1; }
  mvn "${MVN_OPTS[@]}" clean package
  local jar
  jar="$(ls target/medical-backend-*.jar 2>/dev/null | head -1 || true)"
  [[ -n "${jar}" ]] || { echo "ERROR: 后端 jar 未生成" >&2; exit 1; }
  printf 'OK backend jar: %s (%s bytes)\n' "${jar}" "$(stat -c %s "${jar}" 2>/dev/null || echo '?')"
  popd >/dev/null
}

build_frontend() {
  phase "前端构建 (medical-managerment-system)"
  pushd medical-managerment-system >/dev/null
  printf 'current dir: %s\n' "$(pwd)"
  command -v node >/dev/null || { echo "ERROR: node 未安装" >&2; exit 1; }
  printf 'node: %s, npm: %s\n' "$(node -v)" "$(npm -v)"
  # 国内网络下走 npmmirror
  npm config set registry https://registry.npmmirror.com >/dev/null 2>&1 || true
  if [[ -f package-lock.json ]]; then
    npm ci --no-audit --no-fund || npm install --no-audit --no-fund
  else
    npm install --no-audit --no-fund
  fi
  npm run build
  [[ -d dist ]] || { echo "ERROR: dist/ 未生成" >&2; exit 1; }
  printf 'OK frontend dist entries:\n'
  ls -lh dist/ | head
  popd >/dev/null
}

case "${1:-all}" in
  backend)  build_backend ;;
  frontend) build_frontend ;;
  all)
    build_backend
    build_frontend
    ;;
  *)
    cat <<EOF
用法: bash deploy/scripts/ci-build.sh <target> [skip-tests]
  target: backend | frontend | all
  skip-tests: true | false  (默认 true)
EOF
    exit 64
    ;;
esac

phase "构建完成"
