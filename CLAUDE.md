# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

`leisure` — Spring Boot 4.0.6 / Java 21 백엔드 (Spring MVC + JPA/Hibernate + MySQL 8, 토큰 저장소로 Redis). 공통 인프라는 `com.leisure.global` 아래에 있고, 기능 도메인으로 `com.leisure.member`(회원), `com.leisure.auth`(로그인/인증), `com.leisure.post`(게시글), `com.leisure.postLike`(좋아요), `com.leisure.Bookmark`(북마크)가 개발 중이다 — `domain`(엔티티) / `repository` / `service` / `controller` / `dto.request` / `dto.response` / `dto.result` / `event` 레이어 구조를 따른다(도메인마다 필요한 레이어만 둔다). `dto.result`는 서비스가 컨트롤러에 돌려주는 내부 결과 객체로, 클라이언트에 나가는 `dto.response`와 구분한다(예: 로그인에서 서비스는 두 토큰을 담은 `LoginResult`를 반환하고, 컨트롤러가 access 토큰만 담은 `LoginResponse`로 변환). 인증은 JWT 기반이며 관련 공통 인프라는 `global/auth` 아래에 있다(아래 "인증 & 토큰" 참고).

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

테스트 클래스는 원칙적으로 서비스 1:1로 두되(`PostService`→`PostServiceTest`), 조회(Query) 서비스처럼 한 서비스가 여러 조회 유스케이스를 담으면 **유스케이스별로 클래스를 분리**해 이름에서 대상이 드러나게 한다(예: `PostQueryService.getMyPosts`→`MyPostQueryServiceTest`, 이후 상세/피드는 `PostDetailQueryServiceTest`/`PostFeedQueryServiceTest`). 현재 서비스 유닛 테스트는 있으나 **컨트롤러(MockMvc) 계층 테스트는 아직 없다** — `@RequestBody`/`@Valid` 누락 같은 바인딩 버그는 각 API 개발 시 함께 커버한다.

## 로컬 데이터베이스

앱은 `localhost:3306`의 MySQL 8, 데이터베이스 `leisure`, 계정/비번 `root`/`root`가 필요하다(`application-local.yml` 참고). README.md에 Colima + Docker(macOS) 기반 세팅 방법이 정리되어 있다. `local` 프로파일은 `ddl-auto: create`라서 **매 기동 시 모든 테이블을 삭제 후 재생성한다** — 중요한 데이터를 `local` 프로파일에 연결하지 말 것. 기본(default) 프로파일용 datasource가 없으므로, `--spring.profiles.active=local`(혹은 다른 프로파일) 없이 실행하면 JPA가 커넥션을 요구하는 순간 기동에 실패한다.

## 아키텍처 규칙

공통 요소는 전부 `com.leisure.global` 아래에 둔다. 기능 코드는 `com.leisure`의 형제 패키지(예: `com.leisure.<도메인>`)에 작성한다.

**API 응답** — 모든 컨트롤러는 `ApiResponse<T>`(`global/response`)를 반환한다. `{ success, code, message, data }` 형태의 record이며, `ApiResponse.success(message, data)` / `ApiResponse.fail(code, message)` / `ApiResponse.fail(code, message, data)`로 생성한다. 마지막 오버로드는 검증 실패처럼 부가 데이터(예: 필드별 에러 메시지 목록)를 `data`에 실을 때 쓴다.

**예외 처리** — 흐름은: 서비스가 `BusinessException(ErrorCode)`를 던짐 → `GlobalExceptionHandler`(`@RestControllerAdvice`)가 잡아서 `ApiResponse.fail`로 변환한다.
- 도메인 에러는 `ErrorCode` enum(`global/exception`)에 상수로 정의하며, 각각 `HttpStatus status`, `int code`, `String message`를 가진다. 회원 관련 상수(`EMAIL_DUPLICATE`, `NICKNAME_DUPLICATE`, `PASSWORD_MISMATCH`, `MEMBER_NOT_FOUND` 등)와 토큰 관련 상수(`TOKEN_EXPIRED`, `TOKEN_INVALID`, `TOKEN_UNSUPPORTED`)가 이미 정의돼 있다. 새 도메인 에러가 필요하면 여기에 항목을 추가한다.
- 주의: `ErrorCode`의 필드와 게터가 `messgae` / `getMessgae()`로 오타가 있다. 프로젝트 전체에서 이름을 고치기 전까지는 기존 사용을 그대로 따를 것.
- `BusinessException`은 성능을 위해 `fillInStackTrace()`를 오버라이드해 스택 캡처를 생략한다 — 버그가 아니라 의도된 동작.
- 표준/프레임워크 예외(검증, 파싱 오류, 미디어 타입, 업로드 크기, 폴백 `Exception`)는 `GlobalExceptionHandler`에서 하드코딩된 응답으로 처리한다. 이런 것들은 `ErrorCode` enum에 넣지 말 것 — 클래스 Javadoc에 비즈니스 외 예외는 상수 또는 별도 enum으로 관리하라고 명시되어 있다.
- 검증 실패(`MethodArgumentNotValidException`)는 Spring 기본값 400이 아니라 의도적으로 **422**(`UNPROCESSABLE_CONTENT`)를 반환하며, 실패한 필드들의 메시지를 `data`에 `List<String>`으로 담아 내려준다. 필드별 메시지 문구는 `ValidationMessageConstants`(`global/exception`) 상수로 관리한다. (참고: `UNPROCESSABLE_ENTITY`는 deprecated이므로 `UNPROCESSABLE_CONTENT`를 쓴다.)

**JPA 베이스 엔티티**(`global/entity`) — 엔티티에 필요한 것에 맞춰 상속한다.
- `BaseCreatedEntity` → `created_at`만 (`@CreatedDate` + `AuditingEntityListener` 사용)
- `BaseTimeEntity extends BaseCreatedEntity` → `updated_at` 추가 (`@LastModifiedDate`)
- `BaseSoftDeleteEntity extends BaseTimeEntity` → `deleted_at`와 소프트 삭제용 `delete()` / `isDeleted()` 추가

감사(auditing)는 `AuditingEntityListener`에 의존하며, 이를 켜는 `@EnableJpaAuditing`은 메인 클래스 `LeisureApplication`에 이미 적용되어 있다. 따라서 `created_at`/`updated_at`은 자동으로 채워진다.

`spring.jpa.open-in-view`가 전역으로 꺼져 있으므로(`false`), 지연 로딩 연관관계는 뷰/컨트롤러가 아니라 트랜잭션/서비스 계층 안에서 해결해야 한다.

## 인증 & 토큰

JWT 기반 인증이며 관련 코드는 전부 `global/auth` 아래에 있다.

- **로그인** — `com.leisure.auth`. `POST /auth`(`AuthController.login`)가 이메일/비밀번호를 검증하고 access/refresh 토큰을 발급한다. `AuthService.login`은 회원 검증 → 무효화 버전 조회 → 토큰 발급 → refresh 토큰 저장(Redis) 순서로 처리하고 두 토큰을 담은 `LoginResult`를 반환한다. 컨트롤러는 refresh 토큰을 `CookieProvider.createRefreshTokenCookie` 쿠키(`Set-Cookie` 헤더)로 내려보내고, access 토큰만 `LoginResponse`에 담아 `ApiResponse`의 `data`로 반환한다(refresh 토큰은 본문에 노출하지 않는다).
- **로그아웃** — `DELETE /auth`(`AuthController.logout`)는 `@CurrentMember`로 얻은 `publicId`와 요청에서 뽑은 access 토큰으로 `AuthService.logout`을 호출한다. 서비스는 access 토큰을 남은 만료 시간(TTL)만큼 블랙리스트에 등록(`RedisBlacklistTokenStore.save`)하고 refresh 토큰을 Redis에서 제거(`RedisRefreshTokenStore.remove`)한다. 컨트롤러는 `CookieProvider.createClearRefreshTokenCookie`로 만든 빈 쿠키를 `Set-Cookie`로 내려 refresh 쿠키를 지우고 `204 No Content`를 반환한다.
- **토큰 재발급 (Refresh Token Rotation)** — `POST /auth/refresh`(`AuthController.reissue` → `AuthService.reissue`). 컨트롤러가 `RefreshTokenResolver`로 쿠키에서 refresh 토큰을 꺼내 서비스에 넘기면(추출 결과가 null이어도 컨트롤러는 그대로 전달하고, null/공백 방어는 `AuthService.reissue` 진입부에서 한다), 서비스는 토큰을 검증한 뒤 새 access/refresh 토큰을 발급하고 `refreshTokenStore.rotate(TokenRotationContext)`로 저장소의 refresh 토큰을 원자적으로 교체한다. rotate 결과에 따라 분기한다: `NOT_FOUND` → `REFRESH_TOKEN_NOT_FOUND`, `MISMATCHED`/`CONCURRENTLY_UPDATED` → 재사용/탈취로 간주해 refresh 토큰 제거 + `increaseInvalidationVersion`(해당 회원 전 세션 무효화) 후 `REFRESH_TOKEN_REUSE_DETECTED`, `SUCCESS` → 새 토큰(`ReissueResult`) 반환. 컨트롤러는 로그인과 동일하게 새 refresh 토큰은 쿠키(`Set-Cookie`)로, 새 access 토큰만 `ReissueResponse`에 담아 `ApiResponse.data`로 내려준다.
- **토큰 발급/검증** — `JwtTokenProvider`가 access/refresh 토큰을 발급(`issueAccessToken` / `issueRefreshToken`)하고, `JwtAuthenticationFilter`가 요청마다 토큰을 검증한다. 설정값은 `JwtProperties`(`jwt.secret`, `jwt.access-token-expiration`, `jwt.refresh-token-expiration`)로 바인딩된다. jjwt의 `issuedAt`/`expiration`은 `java.util.Date`를 받으므로 `Date.from(instant)`로 변환해 넘긴다. 토큰의 남은 만료 시간은 `TokenTtlResolver`로 계산하며(음수는 0으로 클램핑), 블랙리스트 등 Redis TTL에 사용한다.
- **요청에서 토큰 추출** — `global/auth/resolver`. `AccessTokenResolver`는 `Authorization: Bearer` 헤더에서 access 토큰을, `RefreshTokenResolver`는 쿠키(`CookieProperties.name()`)에서 refresh 토큰을 꺼낸다. 둘 다 없으면 `null`을 반환하며(형제 클래스 간 계약 통일), null 방어는 이를 받는 서비스 진입부에서 한다.
- **컨트롤러에서 인증 회원 얻기** — 커스텀 애너테이션 `@CurrentMember`(파라미터에 부착)로 현재 회원의 `publicId`를 주입받는다. `CurrentMemberArgumentResolver`가 처리하며, `WebConfiguration`에 등록돼 있다.
- **Redis 토큰 저장소** — `global/auth/store`. `TokenStore` 인터페이스 아래 `RedisBlacklistTokenStore`(로그아웃 등으로 폐기된 access 토큰), `RedisRefreshTokenStore`(refresh 토큰), `RedisTokenStatusStore`(회원별 무효화 버전으로 access 토큰 즉시 무효화)가 있다. 토큰 원본을 그대로 키로 쓰지 않고 `TokenHasher.hash()`(SHA-256)로 해싱한 값을 키로 쓰며, 각 항목엔 남은 만료 시간만큼 TTL을 건다. Redis 연결/`StringRedisTemplate`은 `RedisConfiguration`에서 설정한다.
- **비밀번호 변경** — `PATCH /members/me/password`(`MemberService.changePassword`). 현재 비밀번호 확인 + 새 비밀번호 일치 검증 후 비밀번호를 교체하고, `increaseInvalidationVersion`으로 **해당 회원의 기존 세션을 전부 무효화**한다. 그다음 새 무효화 버전으로 access/refresh 토큰을 재발급해 **현재 세션만 다시 살린다**(refresh는 쿠키, access는 `ReissueResponse`로 반환 — 로그인/재발급과 동일 패턴). 즉 비밀번호를 바꾸면 다른 기기·세션은 로그아웃되고 요청을 보낸 세션만 유지된다.
- **회원 탈퇴 시 토큰 정리** — 서비스가 `MemberWithdrawnEvent`를 발행하면, `MemberWithdrawnEventListener`가 `@TransactionalEventListener(AFTER_COMMIT)`로 받아 무효화 버전을 올리고 refresh 토큰을 제거한다. 트랜잭션 커밋 이후에만 도는 점에 유의.

`global/config`에는 `SecurityConfiguration`(Spring Security 필터체인 + `PasswordEncoder`), `CorsConfiguration`, `WebConfiguration`(리졸버 등록), `RedisConfiguration`이 있다.

## 게시글 (post)

`com.leisure.post`. 작성은 **즉시 생성 모델**이다 — `POST /posts`가 빈 글을 `WRITING` 상태로 생성하고 `post_id`를 즉시 발급(`Post.startWriting(memberId)`)한다. 이후 `PATCH /posts/{postId}`(저장, `saveDraft`)와 `PATCH /posts/{postId}/publish`(게시, `publish`)가 그 글을 갱신한다. 게시된 글 수정은 `PATCH /posts/{postId}/content`(`editPost`), 삭제는 `DELETE /posts/{postId}`(소프트 삭제). 작성자는 `@CurrentMember`의 `publicId`로 주입되며 body에 넣지 않는다.

- **상태(`PostStatus`)** — `WRITING`/`DRAFT`/`PENDING`/`PUBLISHED`/`REJECTED`. 상태 변경은 반드시 도메인 전이 메서드로만 한다(setter 금지): `markAsDraft`(WRITING→DRAFT), `submitForApproval`(→PENDING), `approve`(PENDING→PUBLISHED), `reject`(PENDING→REJECTED), `publish`(WRITING/DRAFT→PUBLISHED). 내용 반영은 `applyContent(title, content, category)` — **null=유지, ""=비우기**의 부분 갱신이고, 편집 불가 상태(PENDING/PUBLISHED)면 `POST_NOT_EDITABLE`을 던진다(`isEditable`=WRITING/DRAFT/REJECTED).
- **저장(`saveDraft`)** — 무검증(permissive). 제목/본문/카테고리가 전부 비어도 성공하며 WRITING이면 DRAFT로 승격. `PostSaveRequest`는 `@Size(max=50)`만 두고 필수검증은 하지 않는다.
- **게시(`publish`)** — 제목 필수(`POST_TITLE_REQUIRED`). 게시 요청이 그 순간의 내용을 body로 실어보내 `applyContent` 후 상태 전이하므로, 저장을 한 번도 안 거쳐도 `WRITING`에서 바로 게시된다.
- **게시글 수정(`editPublished`)** — 이미 게시된(PUBLISHED) 글 전용 전이 메서드. **PUBLISHED가 아니면 `POST_NOT_EDITABLE`**, 제목을 빈값으로 만들려 하면 `POST_TITLE_REQUIRED`. 상태는 PUBLISHED 그대로 유지(재심사 없음, 티스토리식 즉시 반영). 임시저장 경로(`applyContent`)와 별도로 둔 이유는 AI 도입 시 게시글 수정만 재심사가 필요할 수 있어서다. 제목은 두 경로 모두 `trim()` 후 저장한다.
- **삭제** — `BaseSoftDeleteEntity.delete()`로 소프트 삭제. 조회는 `findByPostIdAndDeletedAtIsNull`로 삭제분을 제외한다.
- **비정규화 카운트** — `viewCount`/`likeCount`/`bookmarkCount`는 Post에 두고, 증감은 도메인 메서드가 아니라 리포지토리의 원자적 `@Modifying` 쿼리(`increaseLikeCount` 등)로 처리한다.
- **AI 심사는 MVP 이후로 보류** — 현재 `publish`는 `submitForApproval`이 아니라 `publish()`로 **바로 PUBLISHED**로 간다. `submitForApproval`/`approve`/`reject`와 `PENDING`/`REJECTED` 상태는 AI 연동 대비용 **죽은 코드**로 남겨둔 것이니 임의로 지우지 말 것. AI를 붙일 때 `publish()`를 `submitForApproval` + 이벤트/리스너 흐름으로 되돌린다.
- **소유권** — 서비스의 `getOwnedPost(publicId, postId)`가 글 조회(`findByPostIdAndDeletedAtIsNull`, 없으면 `POST_NOT_FOUND`) + 작성자 검증(`Post.isWrittenBy`, 불일치 시 `POST_FORBIDDEN`)을 묶어, 저장/게시/수정/삭제가 모두 재사용한다. 작성자 참조는 id-only(`memberId`)다.
- **내 게시글 목록(`GET /members/me/posts`, `PostQueryService.getMyPosts`)** — 오프셋(page/size) 기반. QueryDSL(`PostCustomImpl`)로 members·postLike·postBookmark를 조인해 작성자 정보와 isLiked/isBookmarked까지 한 방에 프로젝션한다(N+1 회피). `PublishED`만 노출하므로 초안(WRITING/DRAFT)은 안 보이며, 초안 목록은 별도 엔드포인트로 둘 예정. "내 글"이라 작성자 탈퇴 필터는 생략.
- **게시글 상세 조회(`getPostDetail`)와 메인 피드(`GET /posts`, 커서 기반)는 아직 미구현** — 상세는 `PostCustomImpl.findPostDetail`(QueryDSL)까지는 있고 서비스가 주석 처리됨. 피드는 커서 기반으로 짤 예정. 대표이미지·태그는 관련 도메인(Tag/이미지) 구현 후 채운다.

## 좋아요 / 북마크 (postLike, Bookmark)

`com.leisure.postLike`, `com.leisure.Bookmark`. 구조가 동일하다 — 회원↔게시글 조인테이블(`PostLike`/`PostBookmark`, id-only 참조) + 토글 API + 내 목록 조회.

- **토글** — `POST/DELETE /posts/{postId}/likes`(`PostLikeService.like`/`unlike`), `POST/DELETE /posts/{postId}/bookmarks`(`BookmarkService`). 흐름: `getPublishedPost`로 대상 확인(없거나 PUBLISHED 아니면 `POST_NOT_FOUND`) → 중복 검사(`existsBy...`, 이미 눌렀으면 `..._ALREADY_...`) → 저장/삭제 → Post의 비정규화 카운트를 원자적 `@Modifying`으로 증감 → 최신 카운트와 상태 플래그를 응답. 저장 경로는 `save`+`flush` 후 `DataIntegrityViolationException`을 잡아 동시성 안전망을 둔다. 취소는 `deleteBy...` 반환값이 0이면 `..._NOT_..._YET`.
- **내 목록 조회** — `GET /members/me/likes`, `GET /members/me/bookmarks`. 오프셋 기반이며 QueryDSL로 조인 프로젝션한다. **목록 쿼리(QueryDSL)와 count 쿼리(JPQL)의 where 필터가 반드시 일치해야** totalElements와 실제 개수가 어긋나지 않는다(삭제글·탈퇴작성자·PUBLISHED 조건 동일하게 유지). 정렬 `POPULAR`(likeCount desc)는 값이 변해 오프셋 페이지네이션과 상성이 나쁘지만 개인 목록 규모라 트레이드오프로 수용.
- **페이지 파라미터 검증** — 각 QueryService가 `validatePage`(null→0, 음수→`PAGE_INVALID`)/`validateSize`(null→10, 1~30 밖→`PAGE_SIZE_INVALID`)로 방어한다.

## Git & CI 워크플로우

브랜치 흐름은 `feature/*` → `dev` (→ `main`). `dev`로의 PR은 CI 워크플로우를 트리거한다: 단위 테스트 실행 후, 자동 Claude Sonnet 코드 리뷰가 한국어 리뷰 댓글을 남긴다. 커밋 메시지는 `[브랜치][타입]: 설명` 형식을 따른다(예: `[feature/ci][feat]: ...`), 타입은 `feat` / `fix` / `chore` 등, 설명은 한국어로.
