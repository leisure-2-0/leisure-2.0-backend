# Leisure — 로컬 개발 환경 세팅

> ⚠️ 이 문서는 macOS 전용입니다.
> Docker Desktop이 무거워서, 경량 도커 런타임인 Colima를 설치해 사용합니다.
> Colima는 macOS(및 Linux)용 도구라 Windows 개발 환경에는 맞지 않습니다.
> Windows에서는 Docker Desktop 또는 WSL2 기반 환경을 사용하세요.

Colima(경량 도커 엔진) + Docker Compose로 **MySQL 8 + Redis 7** 컨테이너를 한 번에 띄워 개발합니다.
로컬 인프라 정의는 `docker/docker-compose-local.yaml`에 있습니다.

---

## 0. 참고했던 다른 방법들

```bash
# Homebrew 서비스로 MySQL 직접 설치·실행하는 방법
brew services list          # brew로 관리되는 서비스 목록 확인
brew services start mysql   # MySQL 서비스 시작

# Docker Desktop(cask)으로 설치하는 방법
docker --version
brew install --cask docker    # Docker Desktop 설치
brew upgrade --cask docker    # 업그레이드
brew uninstall --cask docker  # 제거
```

> 위 방법들도 가능하지만, 이 프로젝트는 아래 Colima + Docker Compose 방식으로 진행했습니다.

---

## 1. 최초 1회 설치

처음 세팅할 때 한 번만 실행합니다.

```bash
# Colima(도커 엔진) + Docker CLI 설치
brew install colima docker docker-compose

# 엔진 켜기
colima start

# 설치 확인
docker --version
docker-compose version
docker ps
```

### Colima 기본 켜고 끄기 확인

```bash
colima start   # 엔진 켜기
docker ps      # 컨테이너 확인
colima stop    # 엔진 끄기
```

---

## 2. 인프라 컨테이너 생성 (MySQL + Redis)

`docker/docker-compose-local.yaml`이 MySQL 8과 Redis 7을 함께 정의합니다. 프로젝트 루트에서:

```bash
docker-compose -f docker/docker-compose-local.yaml up -d
```

`up -d`는 최초엔 이미지를 받아 컨테이너를 만들고, 이후엔 기존 컨테이너를 백그라운드로 띄웁니다.

| 서비스 | 컨테이너 이름 | 포트 | 비고 |
|--------|--------------|------|------|
| MySQL 8 | `projectY-mysql-local` | 3306 | DB `leisure-local` 자동 생성, root/root, `utf8mb4` + `utf8mb4_0900_ai_ci` |
| Redis 7 | `projectY-redis-local` | 6379 | 토큰 저장소 |

> 데이터는 `mysql-data` / `redis-data` 볼륨에 보존되어, 컨테이너를 내려도(`down`) 볼륨을 지우지 않는 한 유지됩니다.

### 상태 / 로그 확인

```bash
docker-compose -f docker/docker-compose-local.yaml ps        # 서비스 상태
docker-compose -f docker/docker-compose-local.yaml logs -f mysql   # MySQL 로그(초기화 확인)
```

MySQL 초기화가 끝나면 로그에 다음이 보입니다.

```
[Entrypoint]: Creating database leisure-local              ← DB 생성됨
[Entrypoint]: MySQL init process done. Ready for start up. ← 준비 완료
```

---

## 3. 매일 켜고 끄기

### 켜기

```bash
colima start                                                  # 1. 도커 엔진 켜기
docker-compose -f docker/docker-compose-local.yaml up -d       # 2. MySQL + Redis 켜기
```

### 끄기

```bash
docker-compose -f docker/docker-compose-local.yaml stop        # 1. 컨테이너 멈춤(데이터·컨테이너 유지)
colima stop                                                    # 2. 도커 엔진 끄기 (자원 완전 해방)
```

> 컨테이너 자체를 제거하려면 `stop` 대신 `down`을 씁니다(볼륨은 남음).
> 볼륨까지 지워 완전 초기화하려면 `down -v`.

---

## 4. 상태 확인 / DB 접속

```bash
# Colima(엔진) 상태 확인
colima status

# 실행 중인 컨테이너 보기
docker ps

# 멈춘 것까지 전부 보기
docker ps -a

# compose 서비스 로그
docker-compose -f docker/docker-compose-local.yaml logs -f
```

### MySQL 접속

```bash
docker exec -it projectY-mysql-local mysql -uroot -proot
```

```sql
SHOW DATABASES;   -- leisure-local DB가 보이면 정상
EXIT;
```

### Redis 접속

```bash
docker exec -it projectY-redis-local redis-cli
```

```
PING     -- PONG 이 오면 정상
```

---

## 참고

- 스프링 접속 설정은 `application-local.yml`의 `datasource.url`(DB `leisure-local`, 포트 3306) / `username`·`password`(root/root)와 `spring.data.redis`(localhost:6379)가 위 컨테이너 설정과 일치해야 합니다.
- `local` 프로파일은 `ddl-auto: validate`라 **스키마를 생성하지 않고 검증만** 합니다. 마이그레이션 도구가 아직 없으므로, **빈 DB로 처음 띄우면 테이블이 없어 기동에 실패**합니다 — 최초 1회 `create`로 스키마를 만든 뒤(볼륨에 유지) `validate`로 돌리거나 스키마 소스를 추가하세요. SQL은 p6spy로 로깅됩니다.
- 앱 실행 시 프로파일을 반드시 지정합니다: `./gradlew bootRun --args='--spring.profiles.active=local'` 또는 IDE Run Configuration의 Active profiles / 환경변수 `SPRING_PROFILES_ACTIVE=local`.
