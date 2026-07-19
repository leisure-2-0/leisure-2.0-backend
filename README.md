# Leisure — 로컬 개발 환경 세팅

> ⚠️ 이 문서는 macOS 전용입니다.
> Docker Desktop이 무거워서, 경량 도커 런타임인 Colima를 설치해 사용합니다.
> Colima는 macOS(및 Linux)용 도구라 Windows 개발 환경에는 맞지 않습니다.
> Windows에서는 Docker Desktop 또는 WSL2 기반 환경을 사용하세요.

Colima(경량 도커 엔진) + Docker로 MySQL 8 컨테이너를 띄워 개발합니다.

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

> 위 방법들도 가능하지만, 이 프로젝트는 아래 Colima + Docker 방식으로 진행했습니다.

---

## 1. 최초 1회 설치

처음 세팅할 때 한 번만 실행합니다.

```bash
# Colima(도커 엔진) + Docker CLI 설치
brew install colima docker

# 엔진 켜기
colima start

# 설치 확인
docker --version
docker ps
```

### Colima 기본 켜고 끄기 확인

```bash
colima start   # 엔진 켜기
docker ps      # 컨테이너 확인
colima stop    # 엔진 끄기
```

---

## 2. MySQL 컨테이너 생성

```bash
docker run --name mysql-leisure \
  -e MYSQL_ROOT_PASSWORD= \
  -e MYSQL_DATABASE=leisure \
  -p 3306:3306 \
  -v mysql-leisure-data:/var/lib/mysql \
  -d mysql:8
```

| 옵션 | 설명 |
|------|------|
| `--name mysql-leisure` | 컨테이너 이름 |
| `-e MYSQL_ROOT_PASSWORD=` | root 비밀번호 (실제 배포 땐 노출 주의) |
| `-e MYSQL_DATABASE=leisure` | 시작 시 `leisure` DB 자동 생성 |
| `-p 3306:3306` | 호스트 3306 ↔ 컨테이너 3306 포트 연결 |
| `-v mysql-leisure-data:/var/lib/mysql` | 데이터 볼륨 (컨테이너 지워도 데이터 유지) |
| `-d mysql:8` | MySQL 8 이미지, 백그라운드 실행 |

### 포트 리스닝 확인

```bash
netstat -an | grep LISTEN
```

### 초기화 로그 확인

```bash
docker logs mysql-leisure
```

정상이면 로그에 다음이 보입니다.

```
[Entrypoint]: Creating database leisure                     ← leisure DB 생성됨
[Entrypoint]: MySQL init process done. Ready for start up.  ← 준비 완료
```

---

## 3. 매일 켜고 끄기

### 켜기

```bash
colima start               # 1. 도커 엔진 켜기
docker start mysql-leisure # 2. MySQL 컨테이너 켜기
```

### 끄기

```bash
docker stop mysql-leisure  # 1. MySQL 컨테이너 끄기
colima stop                # 2. 도커 엔진 끄기 (자원 완전 해방)
```

---

## 4. 상태 확인 / DB 접속

```bash
# Colima(엔진) 상태 확인
colima status

# 실행 중인 컨테이너 보기
docker ps

# 멈춘 것까지 전부 보기
docker ps -a

# 컨테이너 로그 확인
docker logs mysql-leisure
```

### MySQL 접속

```bash
docker exec -it mysql-leisure mysql -uroot -p
```

```sql
SHOW DATABASES;   -- leisure DB가 보이면 정상 (주의: SHOW DATABASES; 가 정확한 문법)
EXIT;
```

---

## 참고

- 스프링 접속 설정은 `application-local.yml`의 `datasource.url` / `username` / `password`와 위 컨테이너 설정(포트 3306, DB `leisure`, 비번)이 일치해야 합니다.