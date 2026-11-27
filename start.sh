#!/usr/bin/env bash
set -euo pipefail

project_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
backend_env="$project_root/backend/.env"
backend_url=http://localhost:8080/actuator/health
frontend_url=http://localhost:4200/repositories
startup_timeout=300
backend_pid=''
frontend_pid=''

fail() {
  printf 'RepoPilot: %s\n' "$1" >&2
  exit 1
}

load_backend_env() {
  local line name value

  [[ -f $backend_env ]] || return 0

  # Read the supported values as data so a .env file cannot execute shell commands.
  while IFS= read -r line || [[ -n $line ]]; do
    case $line in
      DB_PASSWORD=*|GOOGLE_API_KEY=*|GEMINI_CHAT_MODEL=*)
        name=${line%%=*}
        value=${line#*=}
        if [[ ! -v $name ]]; then
          printf -v "$name" '%s' "${value%$'\r'}"
        fi
        ;;
    esac
  done < "$backend_env"
}

require_secret() {
  local name=$1 prompt=$2

  if [[ -z ${!name:-} ]]; then
    [[ -t 0 ]] || fail "Set $name in backend/.env or the environment before running without a terminal."
    read -rsp "$prompt" "$name" || fail "Could not read $name."
    printf '\n'
    [[ -n ${!name} ]] || fail "$name is required."
  fi

  export "$name"
}

run_frontend() (
  unset DB_PASSWORD GOOGLE_API_KEY GITHUB_TOKEN
  cd -- "$project_root/frontend"
  exec npm "$@"
)

wait_for_http() {
  local service=$1 url=$2 pid=$3 deadline=$((SECONDS + startup_timeout))

  until curl --noproxy '*' --fail --silent --max-time 2 --output /dev/null "$url"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      fail "$service exited during startup. Check the logs above."
    fi
    (( SECONDS < deadline )) || fail "$service did not respond at $url within ${startup_timeout}s."
    sleep 1
  done

  kill -0 "$pid" 2>/dev/null || fail "$service exited during startup. Check the logs above."
}

stop_servers() {
  local pid

  for pid in "$frontend_pid" "$backend_pid"; do
    if [[ -n $pid ]]; then
      kill -TERM -- "-$pid" 2>/dev/null || true
    fi
  done

  wait || true
}

if (( BASH_VERSINFO[0] < 4 || (BASH_VERSINFO[0] == 4 && BASH_VERSINFO[1] < 3) )); then
  fail 'Bash 4.3 or later is required.'
fi

for command in docker node npm curl; do
  command -v "$command" >/dev/null 2>&1 || fail "$command is required. See README.md for setup."
done
docker compose version >/dev/null 2>&1 || fail 'The Docker Compose plugin is required.'

for url in "$backend_url" "$frontend_url"; do
  if curl --noproxy '*' --silent --max-time 2 --output /dev/null "$url"; then
    fail "A server is already responding at $url. Stop it before running the launcher."
  fi
done

load_backend_env
require_secret DB_PASSWORD 'Local database password (use the existing password if already initialized): '
require_secret GOOGLE_API_KEY 'Gemini API key: '
export GEMINI_CHAT_MODEL="${GEMINI_CHAT_MODEL:-gemini-3.1-flash-lite}"

printf 'Starting PostgreSQL with Docker Compose…\n'
(
  cd -- "$project_root/backend"
  docker compose up -d --wait postgres
)

printf 'Installing Angular dependencies…\n'
run_frontend ci

# Give each server its own process group so cleanup also stops its child processes.
set -m
trap stop_servers EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

printf 'Starting Spring Boot…\n'
(
  cd -- "$project_root/backend"
  exec ./mvnw spring-boot:run
) </dev/null &
backend_pid=$!
wait_for_http 'Spring Boot' "$backend_url" "$backend_pid"

printf 'Starting Angular…\n'
run_frontend start </dev/null &
frontend_pid=$!
wait_for_http 'Angular' "$frontend_url" "$frontend_pid"

printf '\nRepoPilot is ready at %s\nPress Ctrl+C to stop Angular and Spring Boot. PostgreSQL stays running.\n' "$frontend_url"
if wait -n "$backend_pid" "$frontend_pid"; then
  printf 'A RepoPilot server stopped.\n'
else
  status=$?
  printf 'A RepoPilot server exited with status %s. Check the logs above.\n' "$status" >&2
  exit "$status"
fi
