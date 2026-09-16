#!/bin/bash
set -e   # 명령이 하나라도 실패하면 즉시 중단

# 배포할 이미지 태그(= 커밋 short_sha). CD가 인자로 넘긴다.
IMAGE_TAG=$1

# 고정 설정값 (환경/배포마다 안 바뀌어 하드코딩)
AWS_REGION="ap-northeast-2"
SSM_SECRET_PATH="/leisure/prod/"           # 앱 시크릿이 저장된 SSM Parameter Store 경로(SecureString)
SERVER_NAME="api.leisure.com"              # 백엔드 API 도메인 (TLS 인증서 경로 조회용, 고정값)
APP_DIR="${APP_DIR:-/home/ubuntu/leisure}" # 배포 작업 디렉터리 (미지정 시 기본값)

# 배포 상태/설정 파일 경로 (.deploy 아래에 모아둔다)
STATE_DIR=".deploy"
ENV_DIR="$STATE_DIR/env"                     # 색깔별 .env 스냅샷 보관 (롤백 시 그 색의 env 재사용)
NGINX_DIR="$STATE_DIR/nginx"
UPSTREAM_FILE="$NGINX_DIR/app-upstream.inc"  # nginx가 include하는 "어느 색으로 보낼지" 파일
LIVE_COLOR_FILE="$STATE_DIR/live_color"      # 현재 라이브 색(blue/green)을 기억하는 파일

cd "$APP_DIR"
mkdir -p "$ENV_DIR" "$NGINX_DIR"
umask 077   # 이후 생성되는 파일을 소유자만 접근 가능하게 (시크릿 보호)

# color_state_path <kind> <color>  →  색깔별 상태 파일 경로 생성
#   예) color_state_path tag blue  →  .deploy/blue_tag
color_state_path() {
  echo "$STATE_DIR/${2}_${1}";
}

# read_or_default <file> <default>  →  파일이 있으면 내용, 없으면 기본값 출력
#   (첫 배포처럼 상태 파일이 아직 없을 때 대비)
read_or_default() {
  if [ -f "$1" ]; then
    cat "$1";
  else
    echo "$2";
  fi
}

# live_color  →  현재 라이브 색 반환 (첫 배포면 빈 문자열 "")
live_color() {
  read_or_default "$LIVE_COLOR_FILE" ""
}

# standby_color <color>  →  반대 색 반환 (blue↔green). 새 버전은 항상 반대 색에 올린다.
#   현재가 blue면 green, 그 외(green이거나 첫 배포로 빈 값)면 blue
standby_color() {
  if [ "$1" = "blue" ]; then
    echo "green"
  else
    echo "blue"
  fi
}

# compose_service <color>  →  docker compose 서비스명 (blue → leisure_blue)
#   도메인이 아니라 도커 내부 네트워크에서 통하는 호스트명. compose에 이 서비스가 정의돼 있어야 한다.
compose_service() {
  echo "leisure_${1}"
}

# fetch_secrets_to_env <tag>  →  SSM 시크릿을 .deploy/env/<tag>.env 로 쓰고 그 경로를 반환
#   /leisure/prod/DB_PASSWORD  →  DB_PASSWORD=...  형태(경로 접두 제거)로 변환.
#   EC2 인스턴스 역할로 직접 읽으므로 시크릿이 CI/CD 파이프라인을 거치지 않는다.
#   필요 권한: ssm:GetParametersByPath + kms:Decrypt
fetch_secrets_to_env() {
  local env_path="$ENV_DIR/${1}.env"
  aws ssm get-parameters-by-path \
    --path "$SSM_SECRET_PATH" \
    --with-decryption \
    --recursive \
    --region "$AWS_REGION" \
    --query "Parameters[].{Name:Name,Value:Value}" \
    --output json \
  | jq -r --arg p "$SSM_SECRET_PATH" '.[] | (.Name | ltrimstr($p)) + "=" + .Value' > "$env_path"
  chmod 600 "$env_path"   # 시크릿 파일이라 소유자만 읽기
  echo "$env_path"
}

# point_nginx_to <color>  →  nginx upstream 파일을 해당 색 서비스로 교체 (트래픽 스위치의 실체)
#   이 파일 한 줄이 "어느 색 컨테이너로 프록시할지"를 결정한다.
point_nginx_to() {
  echo "proxy_pass http://$(compose_service "$1"):8080;" > "$UPSTREAM_FILE"
  chmod 600 "$UPSTREAM_FILE"
}

# require_tls_cert <server_name>  →  해당 도메인 TLS 인증서가 없으면 실패(return 1)
#   HTTPS 배포에서 인증서 없이 HTTP로 조용히 폴백하는 사고를 막는다.
require_tls_cert() {
  local server_name="$1"
  local cert="/etc/letsencrypt/live/${server_name}/fullchain.pem"
  local key="/etc/letsencrypt/live/${server_name}/privkey.pem"
  if [ ! -f "$cert" ] || [ ! -f "$key" ]; then
    echo "tls 인증서를 찾을 수 없습니다: $server_name" >&2
    return 1
  fi
}

# reload_nginx <server_name>  →  인증서 확인 후 nginx 무중단 리로드
#   nginx -s reload: 기존 연결을 끊지 않고 설정만 다시 읽는다.
reload_nginx() {
  require_tls_cert "$1"
  docker compose up -d nginx
  docker compose exec -T nginx nginx -s reload
}

# 1. 색 결정 — 현재 라이브의 반대 색(STANDBY)에 새 버전을 올린다
LIVE=$(live_color)                            # 현재 라이브 색 (첫 배포면 "")
STANDBY=$(standby_color "$LIVE")              # 새 버전을 올릴 대기 색
STANDBY_SERVICE=$(compose_service "$STANDBY") # 그 색의 compose 서비스명 (예: leisure_green)

# 2. 시크릿을 SSM에서 받아 이번 배포용 .env 파일 생성
ENV_FILE=$(fetch_secrets_to_env "$IMAGE_TAG")

# 3. 대기 색에 새 버전 기동 + 헬스체크 (통과해야 트래픽 전환으로 진행)
#    실패하면 여기서 종료 → 현재 라이브는 그대로라 사용자 영향 없음(무중단)
export IMAGE_TAG                              # compose가 ${IMAGE_TAG}로 이미지 태그 치환
export APP_ENV_FILE="$ENV_FILE"              # compose가 env_file로 컨테이너에 주입
docker compose pull "$STANDBY_SERVICE"
if ! docker compose up -d --wait --wait-timeout 60 --remove-orphans "$STANDBY_SERVICE"; then
  echo "새 버전 헬스체크 실패: $STANDBY_SERVICE" >&2
  docker compose logs --no-color --tail=200 "$STANDBY_SERVICE" || true  # 원인 파악용 로그
  docker compose stop "$STANDBY_SERVICE" || true
  exit 1
fi

# 4. nginx 트래픽을 대기 색으로 전환 (리로드 실패 시 이전 색으로 롤백)
point_nginx_to "$STANDBY"
if ! reload_nginx "$SERVER_NAME"; then
  echo "nginx 트래픽 전환 실패, 롤백: $LIVE" >&2
  if [ -n "$LIVE" ]; then                     # 첫 배포가 아니면(이전 색 존재) 되돌린다
    point_nginx_to "$LIVE"
    reload_nginx "$SERVER_NAME" || true
  fi
  exit 2
fi

# 5. 전환 성공 → 새 라이브 상태를 파일에 기록 (다음 배포가 읽는다)
echo "$STANDBY"   > "$LIVE_COLOR_FILE"
echo "$IMAGE_TAG" > "$(color_state_path tag "$STANDBY")"
echo "$ENV_FILE"  > "$(color_state_path env "$STANDBY")"

# 6. 직전 색 컨테이너는 stop만(삭제 X) → 문제 시 빠른 롤백 대비.
#    그다음 어떤 컨테이너도 참조하지 않는 옛 이미지만 정리(stop된 컨테이너의 이미지는 유지됨).
if [ -n "$LIVE" ]; then
  docker compose stop "$(compose_service "$LIVE")"
fi
docker image prune -a -f

echo "blue|green 배포 성공: $STANDBY -> $IMAGE_TAG"
