#!/usr/bin/env bash
# =========================================================================
# 慧医数字医疗应用系统 -- UML 图批量渲染脚本
# 将 uml-diagrams 下全部 .puml 渲染为 PNG，统一输出到 images/
#
# 依赖:
#   - java (JRE, 推荐 11+)
#   - plantuml.jar (默认放在本目录, 或通过 PLANTUML_JAR 环境变量指定)
#     下载地址: https://plantuml.com/download
#
# 用法:
#   ./render-all.sh              # 渲染全部 .puml 到 images/
#   PLANTUML_JAR=/path/p.jar ./render-all.sh
# =========================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR="${PLANTUML_JAR:-$SCRIPT_DIR/plantuml.jar}"
if [ ! -f "$JAR" ]; then
  echo "错误: 未找到 plantuml.jar ($JAR)" >&2
  echo "请从 https://plantuml.com/download 下载放置到该路径, 或设置 PLANTUML_JAR 环境变量。" >&2
  exit 1
fi

command -v java >/dev/null 2>&1 || { echo "错误: 未找到 java, 请先安装 JRE/JDK。" >&2; exit 1; }

mkdir -p images
echo "渲染 UML 图到 images/ (使用 $JAR) ..."

fail_count=0
while IFS= read -r -d '' f; do
  name="$(basename "$f" .puml)"
  if java -jar "$JAR" -charset UTF-8 -tpng -pipe < "$f" > "images/$name.png" 2>/dev/null; then
    echo "  ✓ images/$name.png"
  else
    echo "  ✗ $name 渲染失败" >&2
    fail_count=$((fail_count + 1))
  fi
done < <(find . -name "*.puml" -not -path "./images/*" -print0 | sort -z)

total=$(ls images/*.png 2>/dev/null | wc -l)
echo "完成: 生成 $total 张 PNG -> images/ ${fail_count:+(失败 $fail_count 张)}"
