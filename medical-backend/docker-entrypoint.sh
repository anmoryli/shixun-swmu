#!/bin/sh
set -eu

load_secret() {
  variable_name="$1"
  secret_file="$2"
  current_value="$(printenv "$variable_name" 2>/dev/null || true)"

  if [ -n "$current_value" ] && [ -n "$secret_file" ]; then
    echo "Both $variable_name and ${variable_name}_FILE are set; use only one." >&2
    exit 1
  fi
  if [ -n "$secret_file" ]; then
    if [ ! -r "$secret_file" ]; then
      echo "Secret file is not readable: $secret_file" >&2
      exit 1
    fi
    secret_value="$(cat "$secret_file")"
    if [ -z "$secret_value" ]; then
      echo "Secret file is empty: $secret_file" >&2
      exit 1
    fi
    export "$variable_name=$secret_value"
  fi
}

load_secret DB_PASSWORD "${DB_PASSWORD_FILE:-}"
load_secret REDIS_PASSWORD "${REDIS_PASSWORD_FILE:-}"

if [ -z "${DB_PASSWORD:-}" ] || [ -z "${REDIS_PASSWORD:-}" ]; then
  echo "DB_PASSWORD and REDIS_PASSWORD must be configured through Docker secrets." >&2
  exit 1
fi

if [ "$#" -eq 0 ]; then
  set -- java -jar /app/app.jar
fi

exec "$@"
