# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

`leisure` — Spring Boot 4.0.6 / Java 21 백엔드 (Spring MVC + JPA/Hibernate + MySQL 8). 공통 인프라는 `com.leisure.global` 아래에 있고, 첫 도메인으로 `com.leisure.member`(회원)가 개발 중이다 — `domain`(엔티티) / `repository` / `service` / `dto.request` / `dto.response` 레이어 구조를 따른다.

## 명령어

빌드와 테스트는 Gradle 래퍼(`./gradlew`)를 사용한다. 테스트는 JUnit 5 태그로 분리되어 있다.

```bash
./gradlew test                          # 단위 테스트 — @Tag("integration")을 제외한 전부 실행
./gradlew integrationTest               # 통합 테스트 — @Tag("integration")만 실행, 실행 중인 MySQL 필요
./gradlew test --tests "com.leisure.SomeTest"            # 단일 테스트 클래스
./gradlew test --tests "com.leisure.SomeTest.methodName" # 단일 테스트 메서드
./gradlew build                         # 컴파일 + 전체 빌드 (test도 함께 실행)
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 실행
```

참고: `@SpringBootTest` 컨텍스트 테스트는 반드시 `@Tag("integration")`을 붙여야 한다(`LeisureApplicationTests` 참고). 그래야 `./gradlew test`가 DB 없이 빠르게 돈다. CI(`.github/workflows/leisure-backend-pr-ci.yml`)는 `dev` 대상 PR에서 `./gradlew test`만 실행한다.

## 로컬 데이터베이스

앱은 `localhost:3306`의 MySQL 8, 데이터베이스 `leisure`, 계정/비번 `root`/`root`가 필요하다(`application-local.yml` 참고). README.md에 Colima + Docker(macOS) 기반 세팅 방법이 정리되어 있다. `local` 프로파일은 `ddl-auto: create`라서 **매 기동 시 모든 테이블을 삭제 후 재생성한다** — 중요한 데이터를 `local` 프로파일에 연결하지 말 것. 기본(default) 프로파일용 datasource가 없으므로, `--spring.profiles.active=local`(혹은 다른 프로파일) 없이 실행하면 JPA가 커넥션을 요구하는 순간 기동에 실패한다.

## 아키텍처 규칙

공통 요소는 전부 `com.leisure.global` 아래에 둔다. 기능 코드는 `com.leisure`의 형제 패키지(예: `com.leisure.<도메인>`)에 작성한다.

**API 응답** — 모든 컨트롤러는 `ApiResponse<T>`(`global/response`)를 반환한다. `{ success, code, message, data }` 형태의 record이며, `ApiResponse.success(message, data)` 또는 `ApiResponse.fail(code, message)`로 생성한다.

**예외 처리** — 흐름은: 서비스가 `BusinessException(ErrorCode)`를 던짐 → `GlobalExceptionHandler`(`@RestControllerAdvice`)가 잡아서 `ApiResponse.fail`로 변환한다.
- 도메인 에러는 `ErrorCode` enum(`global/exception`)에 상수로 정의하며, 각각 `HttpStatus status`, `int code`, `String message`를 가진다. 회원 관련 상수(`EMAIL_DUPLICATE`, `NICKNAME_DUPLICATE`, `PASSWORD_MISMATCH`, `USER_NOT_FOUND` 등)가 이미 정의돼 있다. 새 도메인 에러가 필요하면 여기에 항목을 추가한다.
- 주의: `ErrorCode`의 필드와 게터가 `messgae` / `getMessgae()`로 오타가 있다. 프로젝트 전체에서 이름을 고치기 전까지는 기존 사용을 그대로 따를 것.
- `BusinessException`은 성능을 위해 `fillInStackTrace()`를 오버라이드해 스택 캡처를 생략한다 — 버그가 아니라 의도된 동작.
- 표준/프레임워크 예외(검증, 파싱 오류, 미디어 타입, 업로드 크기, 폴백 `Exception`)는 `GlobalExceptionHandler`에서 하드코딩된 응답으로 처리한다. 이런 것들은 `ErrorCode` enum에 넣지 말 것 — 클래스 Javadoc에 비즈니스 외 예외는 상수 또는 별도 enum으로 관리하라고 명시되어 있다.
- 검증 실패(`MethodArgumentNotValidException`)는 Spring 기본값 400이 아니라 의도적으로 **422**를 반환한다.

**JPA 베이스 엔티티**(`global/entity`) — 엔티티에 필요한 것에 맞춰 상속한다.
- `BaseCreatedEntity` → `created_at`만 (`@CreatedDate` + `AuditingEntityListener` 사용)
- `BaseTimeEntity extends BaseCreatedEntity` → `updated_at` 추가 (`@LastModifiedDate`)
- `BaseSoftDeleteEntity extends BaseTimeEntity` → `deleted_at`와 소프트 삭제용 `delete()` / `isDeleted()` 추가

감사(auditing)는 `AuditingEntityListener`에 의존하며, 이를 켜는 `@EnableJpaAuditing`은 메인 클래스 `LeisureApplication`에 이미 적용되어 있다. 따라서 `created_at`/`updated_at`은 자동으로 채워진다.

`spring.jpa.open-in-view`가 전역으로 꺼져 있으므로(`false`), 지연 로딩 연관관계는 뷰/컨트롤러가 아니라 트랜잭션/서비스 계층 안에서 해결해야 한다.

## Git & CI 워크플로우

브랜치 흐름은 `feature/*` → `dev` (→ `main`). `dev`로의 PR은 CI 워크플로우를 트리거한다: 단위 테스트 실행 후, 자동 Claude Sonnet 코드 리뷰가 한국어 리뷰 댓글을 남긴다. 커밋 메시지는 `[브랜치][타입]: 설명` 형식을 따른다(예: `[feature/ci][feat]: ...`), 타입은 `feat` / `fix` / `chore` 등, 설명은 한국어로.
