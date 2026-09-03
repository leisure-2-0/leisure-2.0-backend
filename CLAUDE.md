# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

`leisure` — Spring Boot 4.0.6 / Java 21 백엔드 (Spring MVC + JPA/Hibernate + MySQL 8, 토큰 저장소로 Redis). 공통 인프라는 `com.leisure.global` 아래에 있고, 기능 도메인으로 `com.leisure.member`(회원), `com.leisure.auth`(로그인/인증), `com.leisure.post`(게시글), `com.leisure.postlike`(좋아요), `com.leisure.bookmark`(북마크), `com.leisure.tag`(태그), `com.leisure.festival`(축제 — TourAPI 연동 배치), `com.leisure.region`(지역 — TourAPI 법정동 코드)가 개발 중이다 — `domain`(엔티티) / `repository` / `service` / `controller` / `dto.request` / `dto.response` / `dto.result` / `event` 레이어 구조를 따른다(도메인마다 필요한 레이어만 둔다). `dto.result`는 서비스가 컨트롤러에 돌려주는 내부 결과 객체로, 클라이언트에 나가는 `dto.response`와 구분한다(예: 로그인에서 서비스는 두 토큰을 담은 `LoginResult`를 반환하고, 컨트롤러가 access 토큰만 담은 `LoginResponse`로 변환). 단, 응답이 result와 사실상 동일하고 숨길 필드가 없는 단건 조회는 별도 `dto.response`를 두지 않고 `result`를 그대로 응답으로 내보내기도 한다. (게시글 상세가 원래 이 예외였으나, 태그가 붙으며 `PostDetailResult`(프로젝션)를 `PostDetailResponse`로 감싸게 되어 예외에서 빠졌다 — "태그" 참고. 규약은 "그때그때 최적"을 우선하며 상황이 바뀌면 갱신한다.) 인증은 JWT 기반이며 관련 공통 인프라는 `global/auth` 아래에 있다(아래 "인증 & 토큰" 참고).

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

서비스 계층은 Mockito 순수 유닛 테스트로 검증한다(`MemberServiceTest`, `AuthServiceTest`, `PostServiceTest` 등 참고) — `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`만 쓰고 Spring 컨텍스트·DB·Redis 없이 돌아가므로 `@Tag("integration")`을 붙이지 않는다. 주의: 엔티티의 `publicId`/`memberId`/`postId` 같은 PK·발급 필드는 `@PrePersist`나 DB에서 채워지는데 유닛 테스트에는 영속화가 없어 값이 비어 있다 — 필요하면 `ReflectionTestUtils.setField(entity, "field", ...)`로 주입하거나, `save()` 목이 발급을 흉내내도록 스텁한다.

테스트 클래스는 원칙적으로 서비스 1:1로 두되(`PostService`→`PostServiceTest`), 조회(Query) 서비스처럼 한 서비스가 여러 조회 유스케이스를 담으면 **유스케이스별로 클래스를 분리**해 이름에서 대상이 드러나게 한다(예: `PostQueryService.getMyPosts`→`MyPostQueryServiceTest`, 상세/피드/임시저장은 `PostDetailQueryServiceTest`/`PostFeedQueryServiceTest`/`DraftQueryServiceTest`). 도메인 순수 로직(setter 없는 전이·정규화 등)은 Mockito 없이 평범한 단위 테스트로 검증한다(예: `Member.normalizeEmail`→`MemberTest`). 현재 서비스·도메인 유닛 테스트는 있으나 **컨트롤러(MockMvc)·리포지토리(@DataJpaTest) 계층 테스트는 아직 없다** — `@RequestBody`/`@Valid` 누락 같은 바인딩 버그, QueryDSL 쿼리 정합성은 각 API 개발 시 함께 커버한다. (태그 병합 `TagReader`/어셈블러 전용 테스트도 미작성 — 쿼리서비스 테스트는 어셈블러를 목으로 두므로.)

## 로컬 데이터베이스

앱은 `localhost:3306`의 MySQL 8, 데이터베이스 `leisure-local`, 계정/비번 `root`/`root`가 필요하다(`application-local.yml` 참고). 로컬 인프라(MySQL 8 + Redis 7)는 `docker/docker-compose-local.yaml`로 함께 띄운다(`docker compose -f docker/docker-compose-local.yaml up -d`). MySQL은 `utf8mb4` + `utf8mb4_0900_ai_ci`(MySQL 8 기본, accent/case-insensitive) 콜레이션으로 뜬다 — 대소문자 무시 비교라 이메일 정규화와 이중 안전이 되고 닉네임 `John`/`john`도 중복 취급된다. README.md에 Colima + Docker(macOS) 기반 세팅이 정리돼 있으나 **수동 `docker run` 예시는 구버전(DB명 `leisure`)이라 현재는 위 compose 사용이 정본**이다. `local` 프로파일은 `ddl-auto: validate`다 — Hibernate가 스키마를 **생성하지 않고 엔티티와 일치하는지 검증만** 한다. **스키마 소스는 Flyway다**(`spring.flyway` 활성, `baseline-on-migrate: true`) — `src/main/resources/db/migration/V1__init.sql`이 전체 스키마 baseline이며(Hibernate 스키마 생성 스크립트로 뽑아 정리한 것), 빈 DB면 Flyway가 V1을 실행해 생성하고 기존 DB면 baseline 처리 후 validate가 검증한다. 스키마 변경은 Flyway 마이그레이션으로 관리한다(`ddl-auto: update`는 컬럼을 추가만 하고 삭제하지 않아 유령 컬럼이 남으므로 baseline 추출 외엔 지양). SQL 로깅은 `show-sql` 대신 **p6spy**(실제 바인딩 파라미터 + 실행시간)로 출력한다. 기본(default) 프로파일용 datasource가 없으므로, `--spring.profiles.active=local`(혹은 다른 프로파일) 없이 실행하면 JPA가 커넥션을 요구하는 순간 기동에 실패한다.

## 아키텍처 규칙

공통 요소는 전부 `com.leisure.global` 아래에 둔다. 기능 코드는 `com.leisure`의 형제 패키지(예: `com.leisure.<도메인>`)에 작성한다.

**API 응답** — 모든 컨트롤러는 `ApiResponse<T>`(`global/response`)를 반환한다. `{ success, code, message, data }` 형태의 record이며, `ApiResponse.success(message, data)` / `ApiResponse.fail(code, message)` / `ApiResponse.fail(code, message, data)`로 생성한다. 마지막 오버로드는 검증 실패처럼 부가 데이터(예: 필드별 에러 메시지 목록)를 `data`에 실을 때 쓴다.

**예외 처리** — 흐름은: 서비스가 `BusinessException(ErrorCode)`를 던짐 → `GlobalExceptionHandler`(`@RestControllerAdvice`)가 잡아서 `ApiResponse.fail`로 변환한다.
- 도메인 에러는 `ErrorCode` enum(`global/exception`)에 상수로 정의하며, 각각 `HttpStatus status`, `int code`, `String message`를 가진다. 회원 관련 상수(`EMAIL_DUPLICATE`, `NICKNAME_DUPLICATE`, `PASSWORD_MISMATCH`, `MEMBER_NOT_FOUND` 등)와 토큰 관련 상수(`TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_UNSUPPORTED`)가 이미 정의돼 있다. 새 도메인 에러가 필요하면 여기에 항목을 추가한다.
- 주의: `ErrorCode`의 필드와 게터가 `messgae` / `getMessgae()`로 오타가 있다. 프로젝트 전체에서 이름을 고치기 전까지는 기존 사용을 그대로 따를 것.
- `BusinessException`은 성능을 위해 `fillInStackTrace()`를 오버라이드해 스택 캡처를 생략한다 — 버그가 아니라 의도된 동작.
- 표준/프레임워크 예외(검증, 파싱 오류, 미디어 타입, 업로드 크기, 폴백 `Exception`)는 `GlobalExceptionHandler`에서 하드코딩된 응답으로 처리한다. 이런 것들은 `ErrorCode` enum에 넣지 말 것 — 클래스 Javadoc에 비즈니스 외 예외는 상수 또는 별도 enum으로 관리하라고 명시되어 있다.
- 검증 실패(`MethodArgumentNotValidException`)는 Spring 기본값 400이 아니라 의도적으로 **422**(`UNPROCESSABLE_CONTENT`)를 반환하며, 실패한 필드들의 메시지를 `data`에 `List<String>`으로 담아 내려준다. 필드별 메시지 문구는 `ValidationMessageConstants`(`global/exception`) 상수로 관리한다. (참고: `UNPROCESSABLE_ENTITY`는 deprecated이므로 `UNPROCESSABLE_CONTENT`를 쓴다.)
- **파라미터 검증(`ConstraintViolationException`)** — 컨트롤러에 `@Validated`를 붙이고 `@RequestParam`에 `@Min/@Max` 등을 걸면(예: 월별 축제 `month` 1~12) 위반 시 `ConstraintViolationException`이 나는데, `GlobalExceptionHandler`가 이를 잡아 **`MethodArgumentNotValidException`과 동일하게 422 + 실패 메시지 목록**으로 변환한다(둘은 짝: 전자=바디 검증, 후자=파라미터 검증). `@Validated` 없으면 파라미터 제약은 **무시**되니 주의.

**JPA 베이스 엔티티**(`global/entity`) — 엔티티에 필요한 것에 맞춰 상속한다.
- `BaseCreatedEntity` → `created_at`만 (`@CreatedDate` + `AuditingEntityListener` 사용)
- `BaseTimeEntity extends BaseCreatedEntity` → `updated_at` 추가 (`@LastModifiedDate`)
- `BaseSoftDeleteEntity extends BaseTimeEntity` → `deleted_at`와 소프트 삭제용 `delete()` / `isDeleted()` 추가

감사(auditing)는 `AuditingEntityListener`에 의존하며, 이를 켜는 `@EnableJpaAuditing`은 메인 클래스 `LeisureApplication`에 이미 적용되어 있다. 따라서 `created_at`/`updated_at`은 자동으로 채워진다.

**타임존** — `LeisureApplication.main`에서 `TimeZone.setDefault("Asia/Seoul")`로 **JVM 기본 tz를 KST 고정**한다(Spring 기동 전 첫 줄). 이유: 클라우드 서버는 보통 UTC라 `LocalDate.now()`/`LocalDateTime.now()`(naive)나 auditing이 서버 tz를 따라가면 자정 근처에 한국과 하루 어긋난다. 이 한 줄로 auditing(created/updated)·`publishedAt`·`deletedAt`·비즈니스 날짜(축제 "다가오는", 배치 연초 계산) 전부 KST로 통일된다(호출부마다 `ZoneId` 명시하는 대신 한 곳에서 결정). yaml의 `spring.jpa.properties.hibernate.jdbc.time_zone`/`spring.jackson.time-zone`(둘 다 `Asia/Seoul`)은 **DB 읽기·쓰기와 JSON 직렬화 경계**를 보정하는 별개 설정으로 상호보완이며, JVM 기본 tz는 바꾸지 못한다(그래서 `setDefault`가 따로 필요). `JwtTokenProvider`의 만료 계산은 `Instant`/`Duration` 기반이라 tz 무관(안전).

`spring.jpa.open-in-view`가 전역으로 꺼져 있으므로(`false`), 지연 로딩 연관관계는 뷰/컨트롤러가 아니라 트랜잭션/서비스 계층 안에서 해결해야 한다.

## 인증 & 토큰

JWT 기반 인증이며 관련 코드는 전부 `global/auth` 아래에 있다.

- **이메일 정규화** — 이메일은 `Member.normalizeEmail`(trim + `toLowerCase(Locale.ROOT)`)로 소문자 정규화해 **저장/중복검사/로그인 조회/이메일 중복확인 4곳이 모두 같은 형태**를 쓴다(대소문자 달라도 로그인·중복검사 일관). 콜레이션(`_ci`)과 무관하게 앱 레벨에서 결정적. (비밀번호 규칙은 영문(대소문자 무관)+숫자+특수문자 8~20자 — 대문자 강제는 제거됨.)
- **로그인** — `com.leisure.auth`. `POST /auth`(`AuthController.login`)가 이메일/비밀번호를 검증하고 access/refresh 토큰을 발급한다. `AuthService.login`은 회원 검증 → 무효화 버전 조회 → 토큰 발급 → refresh 토큰 저장(Redis) 순서로 처리하고 두 토큰을 담은 `LoginResult`를 반환한다. 컨트롤러는 refresh 토큰을 `CookieProvider.createRefreshTokenCookie` 쿠키(`Set-Cookie` 헤더)로 내려보내고, access 토큰만 `LoginResponse`에 담아 `ApiResponse`의 `data`로 반환한다(refresh 토큰은 본문에 노출하지 않는다).
- **로그아웃** — `DELETE /auth`(`AuthController.logout`)는 `@CurrentMember`로 얻은 `publicId`와 요청에서 뽑은 access 토큰으로 `AuthService.logout`을 호출한다. 서비스는 access 토큰을 남은 만료 시간(TTL)만큼 블랙리스트에 등록(`RedisBlacklistTokenStore.save`)하고 refresh 토큰을 Redis에서 제거(`RedisRefreshTokenStore.remove`)한다. 컨트롤러는 `CookieProvider.createClearRefreshTokenCookie`로 만든 빈 쿠키를 `Set-Cookie`로 내려 refresh 쿠키를 지우고 `204 No Content`를 반환한다.
- **토큰 재발급 (Refresh Token Rotation)** — `POST /auth/refresh`(`AuthController.reissue` → `AuthService.reissue`). 컨트롤러가 `RefreshTokenResolver`로 쿠키에서 refresh 토큰을 꺼내 서비스에 넘기면(추출 결과가 null이어도 컨트롤러는 그대로 전달하고, null/공백 방어는 `AuthService.reissue` 진입부에서 한다), 서비스는 토큰을 검증한 뒤 새 access/refresh 토큰을 발급하고 `refreshTokenStore.rotate(TokenRotationContext)`로 저장소의 refresh 토큰을 원자적으로 교체한다. rotate 결과에 따라 분기한다: `NOT_FOUND` → `REFRESH_TOKEN_NOT_FOUND`, `MISMATCHED`/`CONCURRENTLY_UPDATED` → 재사용/탈취로 간주해 refresh 토큰 제거 + `increaseInvalidationVersion`(해당 회원 전 세션 무효화) 후 `REFRESH_TOKEN_REUSE_DETECTED`, `SUCCESS` → 새 토큰(`ReissueResult`) 반환. 컨트롤러는 로그인과 동일하게 새 refresh 토큰은 쿠키(`Set-Cookie`)로, 새 access 토큰만 `ReissueResponse`에 담아 `ApiResponse.data`로 내려준다.
- **토큰 발급/검증** — `JwtTokenProvider`가 access/refresh 토큰을 발급(`issueAccessToken` / `issueRefreshToken`)하고, `JwtAuthenticationFilter`가 요청마다 토큰을 검증한다. 설정값은 `JwtProperties`(`jwt.secret`, `jwt.access-token-expiration`, `jwt.refresh-token-expiration`)로 바인딩된다. jjwt의 `issuedAt`/`expiration`은 `java.util.Date`를 받으므로 `Date.from(instant)`로 변환해 넘긴다. 토큰의 남은 만료 시간은 `TokenTtlResolver`로 계산하며(음수는 0으로 클램핑), 블랙리스트 등 Redis TTL에 사용한다.
- **role 클레임 & 인가** — 토큰 클레임은 `subject(publicId)`/`email`/`role`/`tokenInvalidationVersion`. **`role`**은 `MemberRole`(MEMBER/ADMIN, `com.leisure.member.domain`) enum을 `role.name()` 문자열로 싣는다(Member 엔티티의 `role`도 `@Enumerated(STRING)` not null, 가입 시 **MEMBER 고정**·요청 body로 안 받음 — 권한상승 방지). 발급 시 role 출처: **login·비밀번호변경 재발급 = `member.getRole()`**(fresh), **reissue = `provider.getRole(refreshToken)`**(옛 refresh 토큰 claim에서 이어받아 **DB 0회**). `JwtTokenProvider.getRole`은 claim이 없거나 이상값이면 `TOKEN_INVALID`. `JwtAuthenticationFilter`가 claim의 role을 **`SimpleGrantedAuthority("ROLE_"+role.name())`** 로 SecurityContext authorities에 심어 **요청당 DB 조회 없이** role 기반 인가를 준비한다(enum 값엔 접두 없음, authority 만들 때만 `ROLE_`). **실제 인가 강제 지점은 아직 없음(YAGNI)** — 어드민 API 도입 시 `SecurityConfiguration.hasRole`/`@PreAuthorize`로 연결. role 변경 즉시 반영이 필요하면 `increaseInvalidationVersion`으로 강제 재발급(기존 인프라 재사용). 컬럼은 Flyway `V3__add_member_role.sql`(not null default 'MEMBER'로 기존행 백필).
- **요청에서 토큰 추출** — `global/auth/resolver`. `AccessTokenResolver`는 `Authorization: Bearer` 헤더에서 access 토큰을, `RefreshTokenResolver`는 쿠키(`CookieProperties.name()`)에서 refresh 토큰을 꺼낸다. 둘 다 없으면 `null`을 반환하며(형제 클래스 간 계약 통일), null 방어는 이를 받는 서비스 진입부에서 한다.
- **컨트롤러에서 인증 회원 얻기** — 커스텀 애너테이션 `@CurrentMember`(파라미터에 부착)로 현재 회원의 `publicId`를 주입받는다. `CurrentMemberArgumentResolver`가 처리하며, `WebConfiguration`에 등록돼 있다.
- **Redis 토큰 저장소** — `global/auth/store`. `TokenStore` 인터페이스 아래 `RedisBlacklistTokenStore`(로그아웃 등으로 폐기된 access 토큰), `RedisRefreshTokenStore`(refresh 토큰), `RedisTokenStatusStore`(회원별 무효화 버전으로 access 토큰 즉시 무효화)가 있다. 토큰 원본을 그대로 키로 쓰지 않고 `TokenHasher.hash()`(SHA-256)로 해싱한 값을 키로 쓰며, 각 항목엔 남은 만료 시간만큼 TTL을 건다. Redis 연결/`StringRedisTemplate`은 `RedisConfiguration`에서 설정한다.
- **비밀번호 변경** — `PATCH /members/me/password`(`MemberService.changePassword`). 현재 비밀번호 확인 + 새 비밀번호 일치 검증 후 비밀번호를 교체하고, `increaseInvalidationVersion`으로 **해당 회원의 기존 세션을 전부 무효화**한다. 그다음 새 무효화 버전으로 access/refresh 토큰을 재발급해 **현재 세션만 다시 살린다**(refresh는 쿠키, access는 `ReissueResponse`로 반환 — 로그인/재발급과 동일 패턴). 즉 비밀번호를 바꾸면 다른 기기·세션은 로그아웃되고 요청을 보낸 세션만 유지된다.
- **회원 탈퇴 시 토큰 정리** — 서비스가 `MemberWithdrawnEvent`를 발행하면, `MemberWithdrawnEventListener`가 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 무효화 버전을 올리고 refresh 토큰을 제거한다. 트랜잭션 커밋 이후에만 도는 점에 유의.

`global/config`에는 `SecurityConfiguration`(Spring Security 필터체인 + `PasswordEncoder`), `CorsConfiguration`, `WebConfiguration`(리졸버 등록), `RedisConfiguration`이 있다.

## 게시글 (post)

`com.leisure.post`. 작성은 **즉시 생성 모델**이다 — `POST /posts`가 빈 글을 `WRITING` 상태로 생성하고 `post_id`를 즉시 발급(`Post.startWriting(memberId)`)한다. 이후 `PATCH /posts/{postId}`(저장, `saveDraft`)와 `PATCH /posts/{postId}/publish`(게시, `publish`)가 그 글을 갱신한다. 게시된 글 수정은 `PATCH /posts/{postId}/content`(`editPost`), 삭제는 `DELETE /posts/{postId}`(초안은 하드 삭제, 게시글은 소프트 삭제 — 아래 "삭제" 참고). 작성자는 `@CurrentMember`의 `publicId`로 주입되며 body에 넣지 않는다.

- **상태(`PostStatus`)** — `WRITING`/`DRAFT`/`PENDING`/`PUBLISHED`/`REJECTED`. 상태 변경은 반드시 도메인 전이 메서드로만 한다(setter 금지): `markAsDraft`(WRITING→DRAFT), `submitForApproval`(→PENDING), `approve`(PENDING→PUBLISHED), `reject`(PENDING→REJECTED), `publish`(WRITING/DRAFT→PUBLISHED). 내용 반영은 `applyContent(title, content, category, location)` — **null=유지, ""=비우기**의 부분 갱신이고, 편집 불가 상태(PENDING/PUBLISHED)면 `POST_NOT_EDITABLE`을 던진다(`isEditable`=WRITING/DRAFT/REJECTED).
- **저장(`saveDraft`)** — 무검증(permissive). 제목/본문/카테고리가 전부 비어도 성공하며 WRITING이면 DRAFT로 승격. `PostSaveRequest`는 `@Size(max=50)`만 두고 필수검증은 하지 않는다.
- **게시(`publish`)** — 제목 필수(`POST_TITLE_REQUIRED`). 게시 요청이 그 순간의 내용을 body로 실어보내 `applyContent` 후 상태 전이하므로, 저장을 한 번도 안 거쳐도 `WRITING`에서 바로 게시된다.
- **게시글 수정(`editPublished`)** — 이미 게시된(PUBLISHED) 글 전용 전이 메서드. **PUBLISHED가 아니면 `POST_NOT_EDITABLE`**, 제목을 빈값으로 만들려 하면 `POST_TITLE_REQUIRED`. 상태는 PUBLISHED 그대로 유지(재심사 없음, 티스토리식 즉시 반영). 임시저장 경로(`applyContent`)와 별도로 둔 이유는 AI 도입 시 게시글 수정만 재심사가 필요할 수 있어서다. 제목은 두 경로 모두 `trim()` 후 저장한다.
- **삭제(`deletePost`)** — 상태로 분기한다. **초안(`Post.isDraft()`=WRITING/DRAFT)은 하드 삭제** — 게시된 적 없어 좋아요/북마크 참조가 없으므로, 자식인 태그(`TagRepository.deleteByPostId`)만 정리하고 `repository.delete(post)`로 물리 삭제(같은 트랜잭션이라 원자적). **게시글(PUBLISHED)은 소프트 삭제**(`BaseSoftDeleteEntity.delete()`, `deleted_at` 기록). 소프트 삭제분 제외는 **모든 Post 조회 쿼리에 명시적으로** `deletedAt is null`(JPQL) / `post.deletedAt.isNull()`(QueryDSL)을 건다. `BaseSoftDeleteEntity`의 `@SQLRestriction("deleted_at is null")`은 **의도적으로 주석 처리(비활성)** 해 뒀다 — 전역 자동 필터 대신 쿼리별 명시 필터를 택한 것이므로, **새 조회 쿼리를 추가할 때 반드시 이 조건을 직접 붙여야 한다(빠뜨리면 삭제글이 노출된다)**. 연관 로딩·`findById`류에도 자동 제외가 없다는 점에 유의. TODO: 소프트 삭제된 게시글은 배치로 일괄 하드 삭제하고 태그·좋아요·북마크도 같은 생명주기로 함께 배치 삭제한다.
- **비정규화 카운트** — `viewCount`/`likeCount`/`bookmarkCount`는 Post에 두고, 증감은 도메인 메서드가 아니라 리포지토리의 원자적 `@Modifying` 쿼리(`increaseLikeCount` 등)로 처리한다. 이 증감 쿼리들은 `deleted_at is null` + `status=PUBLISHED` 조건을 함께 건다 — **소프트삭제 자동 필터(`@SQLRestriction`)를 끈 정책**이라 SELECT뿐 아니라 벌크 UPDATE에도 직접 조건을 걸어 삭제·비공개 글에는 반영되지 않게 한다.
- **위치(`PostLocation`)** — `@Embeddable` 값 객체(`region`/`placeName`/`address`/`latitude`/`longitude`)를 Post에 `@Embedded`로 인라인한다. 요청은 중첩 `LocationRequest`로 받아 `toPostLocation()`으로 도메인 값 객체로 변환하고, 서비스가 `request.location() == null`이면 null(유지)을 넘긴다. **null 계약: location 객체가 null이면 위치 유지, 오면 통째 교체**(내부 필드 null은 "그 정보 없음"으로 그대로 저장 — 카카오맵이 일부 필드를 못 채울 수 있음). `PostLocation.of(...)`는 전부 null이면 null을 반환한다(빈 껍데기 방지). 사용자에게 "위치 제거" 버튼은 없어 명시적 제거 유스케이스는 없다. 조회 노출: **상세는 전체 `LocationResult`, 목록(둘러보기/메인/내글/좋아요/북마크)은 `region`만**(카드에 "강릉"처럼 지역만 표시).
- **태그(`tags`)** — 저장/게시/수정 요청(`PostSaveRequest`/`PostPublishRequest`/`PostEditRequest`)이 `Set<String> tags`(`@Size(max=5)`, 개수만 제한)를 함께 받는다. 서비스는 `request.tags() != null`일 때만 `replaceTags(postId, tags)`로 **전체 삭제 후 재삽입**(`TagRepository.deleteByPostId` 벌크 delete → `PostTag.createAll` saveAll)한다. **null=태그 유지, 빈 Set=전체 제거**. 상세는 아래 "태그" 섹션 참고.
- **AI 심사는 MVP 이후로 보류** — 현재 `publish`는 `submitForApproval`이 아니라 `publish()`로 **바로 PUBLISHED**로 간다. `submitForApproval`/`approve`/`reject`와 `PENDING`/`REJECTED` 상태는 AI 연동 대비용 **죽은 코드**로 남겨둔 것이니 임의로 지우지 말 것. AI를 붙일 때 `publish()`를 `submitForApproval` + 이벤트/리스너 흐름으로 되돌린다.
- **소유권** — 서비스의 `getOwnedPost(publicId, postId)`가 글 조회(`findByPostIdAndDeletedAtIsNull`, 없으면 `POST_NOT_FOUND`) + 작성자 검증(`Post.isWrittenBy`, 불일치 시 `POST_FORBIDDEN`)을 묶어, 저장/게시/수정/삭제가 모두 재사용한다. 작성자 참조는 id-only(`memberId`)다.
- **내 게시글 목록(`GET /members/me/posts`, `PostQueryService.getMyPosts`)** — 오프셋(page/size) 기반. QueryDSL(`PostCustomImpl`)로 members·postLike·postBookmark를 조인해 작성자 정보와 isLiked/isBookmarked까지 `MyPostResult`로 프로젝션한 뒤(N+1 회피), `PostResponseAssembler.assembleMyPosts`로 태그를 병합해 `MyPostResponse`로 반환한다. `PublishED`만 노출하므로 초안(WRITING/DRAFT)은 안 보이며(초안은 아래 별도 엔드포인트). "내 글"이라 작성자 탈퇴 필터는 생략.
- **내 임시저장 목록(`GET /members/me/drafts`, `PostQueryService.getMyDrafts`)** — 인증 필수(본인만). `PostCustomImpl.findMyDrafts`가 `where memberId + status=DRAFT`로 `DraftListResponse`(postId/title/category/updatedAt)를 `updatedAt desc`로 프로젝션한다. **페이지네이션 없이 전체 반환**(개인 데이터라 규모 작음), 작성자 조인·태그 없음(단일 DTO). status는 서버가 `DRAFT`로 고정(요청에 노출 안 함). WRITING(작성만 시작한 빈 글)은 편집기가 붙잡고 있어 목록에 제외.
- **내 임시저장 상세(`GET /members/me/drafts/{postId}`, `PostQueryService.getMyDraftDetail`)** — 인증 필수, **수정 화면 로드용**(조회수 증가 없음, `readOnly`). `findMyDraftsDetail`이 `where postId+memberId+status=DRAFT`로 편집 내용(title/content/category/location 전체)을 `DraftDetailResult`로 프로젝션 → `PostResponseAssembler.assembleDraftDetail`로 태그 병합 → `DraftDetailResponse`. 소유권은 `memberId` 필터로 강제(남의 초안은 `POST_NOT_FOUND`, 존재 노출 X). `Location`은 전 필드 null이면 `null`로 접는다(`Location.of`, 공개 상세와 동일 규약).
- **게시글 상세 조회(`GET /posts/{postId}`, `PostQueryService.getPostDetail`)** — 비로그인 공개(`@CurrentMember(required=false)`). 리포지토리 `findPostDetail`이 QueryDSL로 작성자·isMine/isLiked/isBookmarked·location까지 `PostDetailResult`(dto.result)로 프로젝션한다. 서비스는 조회 후 `PostResponseAssembler.assembleDetail`로 **태그를 병합해 `PostDetailResponse`(dto.response)로 반환**한다(컨트롤러도 Response). 태그가 프로젝션에 안 담기므로 result/response를 나눴다("태그" 참고). 조회 성공 시 `increaseViewCount`(원자적 `@Modifying` UPDATE)로 조회수를 +1 한다(존재 확인 후 증가, 응답 viewCount는 증가 전 값). 조회수 어뷰징(중복 방지)·Redis 이관은 성능 측정 후로 보류(현재 TODO).
- **둘러보기 피드(`GET /posts`, `PostQueryService.getPosts`)** — 비로그인 공개, **커서 기반**(오프셋/`totalElements` 없음, `nextCursor`+`hasNext`만). 커서는 `PostCursor`를 JSON→Base64로 인코딩한 불투명 토큰이며 정렬(`PostSort` LATEST/POPULAR)에 맞는 필드를 담는다(LATEST=publishedAt, POPULAR=likeCount, 공통 tie-breaker postId). `limit+1`을 조회해 `hasNext`를 판정하고 초과분을 잘라낸다. **커서 조건(`cursorCondition`)과 orderBy가 정렬 기준별로 일치해야** 중복/누락이 없다. 리포지토리는 `PostResult`로 프로젝션하고, 서비스는 **초과분을 잘라낸 뒤**(버릴 행의 태그 조회 방지) `PostResponseAssembler.assemblePosts`로 태그를 병합해 `PostResponse`로 만든다. 커서 헬퍼는 `PostResult` 기준으로 동작한다.
- **메인 피드(`GET /posts/main`, `PostQueryService.getMainFeedPosts`)** — 비로그인 공개, 페이지네이션 없이 **최신/인기순 상위 18개 고정**(카테고리 필터 가능). 리포지토리는 `MainFeedPostResult`로 프로젝션하고, 서비스가 `PostResponseAssembler.assembleMainFeed`로 태그를 병합해 얇은 래퍼 없이 `List<MainFeedPostResponse>`를 직접 반환한다(둘러보기의 `PostResponse`와 필드는 같지만 결합도 분리를 위해 별도 DTO). `GET /posts/{postId}`(상세)와 경로가 겹치므로 상세는 `{postId:\\d+}`로 숫자만 매칭한다. 조회수·좋아요/북마크 정합성이나 Redis 캐싱은 성능 측정 후로 보류. **대표이미지**는 이미지 도메인 구현 후 채운다(태그는 반영 완료).
- **비로그인 개인화 처리** — 공개 조회(둘러보기/메인/상세)에서 `@CurrentMember(required=false)`로 받은 `publicId`가 null이면 `memberId=null`로 조회한다. QueryDSL `memberIdEq` 헬퍼가 memberId가 null일 때 `Expressions.FALSE`(어떤 좋아요/북마크 행도 매칭 안 함)를 반환해 isLiked/isBookmarked를 false로 만든다.

## 좋아요 / 북마크 (postlike, bookmark)

`com.leisure.postlike`, `com.leisure.bookmark`. 구조가 동일하다 — 회원↔게시글 조인테이블(`PostLike`/`PostBookmark`, id-only 참조) + 토글 API + 내 목록 조회.

- **토글** — `POST/DELETE /posts/{postId}/likes`(`PostLikeService.like`/`unlike`), `POST/DELETE /posts/{postId}/bookmarks`(`BookmarkService`). 흐름: `getPublishedPost`로 대상 확인(없거나 PUBLISHED 아니면 `POST_NOT_FOUND`) → 중복 검사(`existsBy...`, 이미 눌렀으면 `..._ALREADY_...`) → 저장/삭제 → Post의 비정규화 카운트를 원자적 `@Modifying`으로 증감 → 최신 카운트와 상태 플래그를 응답. 저장 경로는 `save`+`flush` 후 `DataIntegrityViolationException`을 잡아 동시성 안전망을 둔다. 취소는 `deleteBy...` 반환값이 0이면 `..._NOT_..._YET`.
- **내 목록 조회** — `GET /members/me/likes`, `GET /members/me/bookmarks`. 오프셋 기반이며 QueryDSL로 `LikedPostResult`/`BookmarkedPostResult`로 조인 프로젝션한 뒤, 각 도메인의 어셈블러(`LikedPostResponseAssembler`/`BookmarkedPostResponseAssembler`)가 태그를 병합해 `*Response`로 만든다. **목록 쿼리(QueryDSL)와 count 쿼리(JPQL)의 where 필터가 반드시 일치해야** totalElements와 실제 개수가 어긋나지 않는다(삭제글·탈퇴작성자·PUBLISHED 조건 동일하게 유지). 정렬 `POPULAR`(likeCount desc)는 값이 변해 오프셋 페이지네이션과 상성이 나쁘지만 개인 목록 규모라 트레이드오프로 수용.
- **페이지 파라미터 검증** — 각 QueryService가 `validatePage`(null→0, 음수→`PAGE_INVALID`)/`validateSize`(null→10, 1~30 밖→`PAGE_SIZE_INVALID`)로 방어한다.

## 태그 (tag)

`com.leisure.tag`. 게시글 태그는 **1:N id-only 엔티티**로 저장한다(`@ElementCollection` 아님). 설계 배경·트레이드오프 전문은 `docs/tag-design-decisions.md` 참고.

- **엔티티(`PostTag`)** — `post_id`(id-only 참조)/`post_tag_name` + 유니크 제약 `uk_post_tags_post_id_tag(post_id, post_tag_name)`. 정적 팩토리 `createAll(postId, tagNames)`가 정규화·중복제거를 책임진다: `trim` → 빈문자 필터 → **`distinct`**(반드시 trim 뒤에 — "강릉"/"강릉 " 충돌 방지) → 매핑. postId가 null이면 `POST_TAG_INVALID`.
- **리포지토리(`TagRepository`)** — `findByPostIdIn(Collection)`(배치 조회)와 벌크 `@Modifying deleteByPostId`. 벌크 delete를 쓰는 이유: Spring Data 파생 삭제는 SELECT 1 + DELETE N인데, 벌크는 SELECT 없이 DELETE 1. (벌크는 영속성 컨텍스트 우회 — 같은 트랜잭션 이후 조회 시 주의.)
- **왜 저장 모델이 1:N인가** — 태그를 **모든 목록 카드**에 노출해야 해 조회 비용이 실재한다. 검색/자동완성/통계는 **Elasticsearch**가 담당하므로 MySQL 저장 모델은 조회 병합에 유리한 쪽(1:N)을 택했다. 작성자처럼 참조는 id-only(`Long postId`)라 조인/프로젝션이 단순하다.
- **조회 노출 = result/response 분리 + 어셈블러** — 태그는 1:N이라 프로젝션(`Projections.constructor`)에 함께 못 담는다. 그래서 **프로젝션용 `*Result`(태그 없음)** 로 조회하고, **어셈블러**가 태그를 병합해 **응답용 `*Response`(태그 포함)** 로 재조립한다(`*Response.from(result, tags)` 정적 팩토리). record 불변이라 "빈 태그 객체를 채우는" 방식 대신 DTO를 나눴다.
  - **어셈블러** — post는 `post.assembler.PostResponseAssembler`(상세/둘러보기/메인/내글), 좋아요/북마크는 각 도메인의 `LikedPostResponseAssembler`/`BookmarkedPostResponseAssembler`. 서비스는 조회만 하고 조립은 어셈블러에 위임(서비스에 병합 헬퍼를 박지 않는다).
  - **태그 조회 공유(`tag.service.TagReader`)** — "태그 조회 + postId별 그룹핑"을 tag 도메인 리프에 모아 세 어셈블러가 공유한다. `findTags(postId)`(단건, 그룹핑 불필요) / `findTagMap(postIds)`(다건, `IN` 1쿼리 → `Map<postId, List<String>>`로 그룹핑, N+1 회피). 모든 어셈블러가 tag에만 의존해 **의존 방향이 건강**하다(post↔like 결합 없음).
  - **병합 규약** — 각 카드는 `tagMap.getOrDefault(postId, List.of())`로 태그를 꺼낸다. **태그 없는 글은 null이 아니라 빈 리스트**로 나가 프론트에 안전(항상 배열 보장).
- **태그 병합 테스트** — `TagReader`의 그룹핑(`findTagMap`/`findTags`)과 `PostTag.createAll` 정규화(trim→distinct, null postId 예외)는 `TagReaderTest`/`PostTagTest`로 검증한다. 어셈블러(병합 조립) 전용 테스트는 아직 없다(쿼리서비스 테스트가 어셈블러를 목으로 두므로, 향후 보강 대상).
- **미정** — 태그 **개별 길이 제한**(요청은 `@Size(max=5)`로 개수만 제한)과 DB 컬럼 length는 아직 안 정함. ES 색인 연동도 향후.

## 축제 (festival)

`com.leisure.festival`. **한국관광공사 TourAPI 연동 도메인. 목록 배치 구현·검증 완료(651건, 멱등), 상세 보강 배치(소개글·홈페이지/운영시간)도 구현(실적재는 쿼터 리셋 후). 캘린더 조회 API(월별/일별/다가오는 Top10)도 구현(아래 "캘린더 조회 API").** 클라이언트는 `com.leisure.global.external.tourapi`(RestClient), 도메인은 `domain`/`dto`/`dto.result`/`repository`/`service`/`controller`/`scheduler` 레이어를 갖춘다. 배치 설계 상세(쿼터 전략·트레이드오프·회고)는 별도 "축제 배치 설계" 문서 참고.

- **엔티티(`Festival`)** — `BaseTimeEntity` 상속(외부 동기화 데이터라 소프트 삭제 없음 — **종료된 축제도 남긴다**, 캘린더가 과거도 노출). `tour_content_id`(TourAPI contentid)를 `unique, not null, updatable=false`로 두어 **배치 upsert의 자연키**로 쓴다. 분류는 빈 값으로 오는 `cat2/cat3` 대신 **`lclsSystm2`(중분류 축제/공연/행사)·`lclsSystm3`(소분류)**, 지역은 **법정동 `ldong_regn_cd`·`ldong_signgu_cd`**(areacode/sigungucode는 축제 응답에서 빈 값). `tour_modified_at`(modifiedtime)은 상세 배치의 변경 감지 기준. 날짜(`event_start_date`/`event_end_date`)는 **nullable**(TourAPI가 빈 값을 줄 수 있어 관대). 위경도는 **mapx→longitude, mapy→latitude 교차 매핑**. 무 setter라 도메인 메서드 `create`/`updateFromList`(둘 다 `FestivalData`를 받음)로만 값을 채운다.
- **클라이언트/DTO** — `TourApiClient.fetchFestivals(eventStartDate)`가 `/searchFestival2`를 **페이지네이션**(numOfRows=100, `totalCount`로 종료 + 빈 페이지 방어)해 `List<FestivalListResponse.Item>`을 반환한다. `FestivalListResponse`는 4단 중첩 record DTO(소문자 JSON 키는 `@JsonProperty`로 매핑). `getWithRetry`(지수 백오프 재시도)·`buildFestivalUri`는 리전과 같은 클라이언트 공유.
- **파싱 위치 = 서비스** (엔티티·DTO는 순수, 로그도 안 함). `FestivalService.toData`가 `Item`(전부 문자열) → `FestivalData`(파싱된 `LocalDate`/`Double`/`LocalDateTime`)로 변환한다. `parseDate`/`parseCoord`/`parseDateTime`는 **실패 시 관대하게 null**(날짜는 contentId 실은 `warn` 로그)로 배치가 한 건에 죽지 않게 하고, `blankToNull`로 빈 문자열("")을 null로 통일한다(TourAPI가 "없음"을 ""로 줌). `FestivalData`는 파싱된 타입을 담아 서비스→업서터로 넘기는 중간 그릇.
- **서비스/업서터 — 네트워크-out-of-tx** (리전과 동일 패턴). `FestivalService.syncFestivalList`(트랜잭션 없음, fetch+파싱)가 `List<FestivalData>`를 `FestivalWriter.updates`(`@Transactional`, DB만)로 넘긴다. `FestivalWriter`(구 FestivalUpserter)는 `findByTourContentId`로 분기: 없으면 `Festival.create`+save, 있으면 `updateFromList`. **변경 감지** — `updateFromList`에서 API `modifiedtime`이 저장된 `tourModifiedAt`보다 최신이면 상세(`overview`/`homepageUrl`/`eventTime`)를 NULL로 리셋해 다음 상세 배치가 다시 채우게 한다. 반환은 `FestivalSyncResult(inserted, updated, total)`.
- **상세 보강 배치** — `FestivalService.syncOverviewAndHomepage`(`detailCommon2`, `overview IS NULL` 대상) / `syncEventTime`(`detailIntro2`, `event_time IS NULL` 대상). 축제 1건당 1호출이라 **`FestivalWriter`가 건별 `@Transactional`**(`updateDetailCommon`/`updateDetailIntro`) — 크래시 시 이미 쓴 쿼터를 보존(목록의 "전체 1tx"와 반대). 빈 응답=일시(`IllegalStateException`)→재시도, resultCode 에러=영구(`TourApiException`)→즉시 실패. **throttle(sleep)**로 연속호출 IP 차단 예방. `normalizeHomepage`가 `<a href>` 태그에서 URL만 추출. ⚠️ **상세 응답 DTO의 `resultCode()`는 반드시 헤더를 파싱할 것**(스텁 `return ""` 금지 — 전건 실패 + 재시도로 쿼터 3배 낭비 유발한 버그가 있었다).
- **트리거** — `FestivalScheduler`(`@Scheduled`, `zone="Asia/Seoul"`, `@EnableScheduling`은 `LeisureApplication`): 목록/소개글/운영시간 3종 크론, 실패는 catch+로그 후 다음 회차 재시도. **자정 경계 분할**(소개글은 자정 전, 운영시간은 자정 후)로 상세 2종을 다른 쿼터-일에 분산. 수동 트리거 컨트롤러(`POST /festivals/...`)는 검증용 임시로만 두고 제거한다.
- **대표이미지** — `thumbnail_url`(TourAPI `firstimage2` 썸네일 URL). 목록 응답에 함께 오므로 **목록 배치가 같이 적재(추가 쿼터 0)**. `blankToNull`로 ""→null 통일, 없는 축제는 null → 프론트가 기본이미지 처리. 원본(firstimage)이 아니라 썸네일이라 필드명은 `thumbnailUrl`(외부 DTO는 출처 그대로 `firstImage2`). 컬럼은 Flyway `V2__add_festival_thumbnail_url.sql`로 추가.
- **분류 enum(`FestivalCategory`)** — `FESTIVAL`=EV01 / `PERFORMANCE`=EV02 / `EVENT`=EV03. `getCode`(enum→lclsSystm2 코드)·`fromCode`(코드→enum, 미매칭/null은 관대하게 null). **클라는 enum명, DB는 코드**로 쓰고 enum이 다리. 조회 필터에서 `code==null`이면 전체.

### 캘린더 조회 API (`FestivalQueryController`/`FestivalQueryService`, 비로그인 공개)
3종 모두 `SecurityConfiguration`의 `GET /festivals/*` permitAll, `@CurrentMember`/`@SecurityRequirement` 없음. 컨트롤러에 `@Validated` + `@Min/@Max`로 파라미터 검증(위반은 `ConstraintViolationException`→**422**, 아래 "예외 처리" 참고).
- **월별(`GET /festivals/months?year&month&category`)** — `year+month`→`YearMonth`로 월초(`atDay(1)`)/월말(`atEndOfMonth()`, 윤년 자동) 계산은 **서비스**, 리포는 계산된 날짜만 받는다. JPQL. **의미는 팀 논의 중**: 현재 활성 쿼리는 "시작일 **또는** 종료일이 그 달"(핀 모델 — 시작달·종료달에 각각 노출), 겹침(overlap: `start≤월말 AND end≥월초`) 버전은 주석으로 보존(롤백 대비, 지우지 말 것). `MonthlyFestivalResponse` 직접 프로젝션(태그·조인 없음).
- **일별(`GET /festivals/days?date&category`)** — 그 날짜에 진행 중(겹침 `start≤date≤end`)인 축제를 **regions LEFT JOIN**(ldong 2코드 AND 조인, 축제 기준 LEFT라 지역 없어도 축제는 나옴)해 상세(개요/운영시간/홈페이지/지역명) 반환. QueryDSL.
- **다가오는(`GET /festivals/upcoming`)** — `eventStartDate > 오늘`(당일 제외, `gt`) 임박순(`start asc, festivalId asc`) **`limit(10)`**, regions LEFT JOIN. 파라미터 없는 고정 콘텐츠(홈). 서버 오늘(`LocalDate.now()`, JVM KST 고정)이 기준이라 클라가 날짜 안 보냄.
- **result/response 분리 + 서비스 변환** — 일별·다가오는은 프로젝션용 `*Result`(lclsSystm2 코드·signguName **원본**)로 조회하고, **서비스**가 `FestivalCategory.fromCode`(코드→enum)와 `toShortRegionName`(시군구 접미사 제거)으로 `*Response`를 조립한다. 변환 로직은 **서비스에 두고 DTO엔 안 둔다**(QueryDSL 프로젝션에 코드→enum·문자열 가공을 넣으면 DRY·SQL이 지저분해서). `toShortRegionName`: 공백 있으면(복합 "포항시 남구") 그대로, `세종특별자치시`→`세종`, 그 외 `^(..+)(시|군|구)$`로 **앞 2글자 이상 남을 때만** 접미사 제거(강릉시→강릉, 남구/중구는 유지). signguName null이면 null(LEFT JOIN 미매칭 방어). ⚠️ 프로젝션 필드 **순서/타입이 `*Result` 생성자와 정확히 일치**해야 함(어긋나면 런타임 실패). *(주의: 응답 DTO의 지역 필드가 일별=`signguName`/다가오는=`region`으로 이름이 갈려 있다 — 통일 대상.)*
- **테스트** — `FestivalCategoryTest`(getCode/fromCode), `MonthlyFestivalQueryServiceTest`(월 경계·code 변환), `DailyFestivalQueryServiceTest`·`UpcomingFestivalQueryServiceTest`(fromCode·지역 strip·매핑). Mockito 순수 유닛(리포 목). QueryDSL 쿼리 자체(@DataJpaTest)·컨트롤러(MockMvc)는 아직 없음.

- **미구현(향후)** — **재시도 정책 세분화**(현재 TourApiException만 빼고 다 재시도 → 429/4xx는 즉시 실패로). 상세 배치 **실적재**(overview/eventTime 651건)는 쿼터 리셋 후. 월별 캘린더 의미(핀 vs 겹침) 팀 확정 후 주석 정리.

## API 문서화 (Swagger / springdoc)

springdoc-openapi(webmvc-ui)로 문서를 생성한다. Swagger UI는 `/swagger-ui/index.html`, 스펙은 `/v3/api-docs`. 둘 다 `SecurityConfiguration`에서 `permitAll`이어야 UI가 스펙을 로드한다(`/v3/api-docs/**` 누락 시 "Failed to load API definition").

- **설정** — `global/config/SwaggerConfiguration`(`OpenAPI` 빈) + `global/properties/SwaggerProperties`(`@ConfigurationProperties("swagger")`, title/description/version/servers). 값은 `application-*.yml`에서 주입(프로파일별 문서 가능). `servers`가 `Map`이라 `@Value`가 아닌 `@ConfigurationProperties`를 쓴다.
- **`@CurrentMember` 파라미터 숨김** — `SwaggerConfiguration`의 static 초기화에서 `SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentMember.class)`로 문서에서 제외한다(토큰에서 주입되는 값이라 클라가 안 보냄).
- **인증 표기** — `BearerAuth`(HTTP bearer/JWT) 스킴을 `Components`에 정의하고, **인증이 필요한 엔드포인트에만 `@SecurityRequirement(name = "BearerAuth")`** 를 붙인다(스킴 이름 대소문자 정확히 일치해야 토큰이 실린다). **`@CurrentMember(required=false)`(비로그인 공개)엔 붙이지 않는다.**
- **엔드포인트 문서** — 컨트롤러에 `@Tag`(도메인 그룹), 메서드에 `@Operation(summary, description)`. description은 "이름으로 모르는 계약"만(상태 전이 조건, null 계약, 세션 무효화 등) — 뻔한 반복은 넣지 않는다.
- **파라미터/바디** — 쿼리·경로 파라미터는 `@Parameter`(의미 있는 것만: `sort`/`cursor`/`page`·`size`. required/default/enum값은 자동 노출이라 반복 금지), 요청 바디 필드는 `@Schema`(이름으로 모르는 **계약**만: `tags`의 null=유지/빈배열=제거, `location`의 null 계약 등). 응답 DTO·자명한 필드는 생략.

## Git & CI 워크플로우

브랜치 흐름은 `feature/*` → `dev` (→ `main`). `dev`로의 PR은 CI 워크플로우를 트리거한다: 단위 테스트 실행 후, 자동 Claude Sonnet 코드 리뷰가 한국어 리뷰 댓글을 남긴다. 커밋 메시지는 `[브랜치][타입]: 설명` 형식을 따른다(예: `[feature/ci][feat]: ...`), 타입은 `feat` / `fix` / `chore` 등, 설명은 한국어로.
