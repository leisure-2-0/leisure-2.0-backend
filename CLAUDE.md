# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

`leisure` — Spring Boot 4.0.6 / Java 21 백엔드 (Spring MVC + JPA/Hibernate + MySQL 8, 토큰 저장소로 Redis). 공통 인프라는 `com.leisure.global` 아래에 있고, 기능 도메인으로 `com.leisure.member`(회원), `com.leisure.auth`(로그인/인증), `com.leisure.post`(게시글), `com.leisure.postLike`(좋아요), `com.leisure.Bookmark`(북마크), `com.leisure.tag`(태그)가 개발 중이다 — `domain`(엔티티) / `repository` / `service` / `controller` / `dto.request` / `dto.response` / `dto.result` / `event` 레이어 구조를 따른다(도메인마다 필요한 레이어만 둔다). `dto.result`는 서비스가 컨트롤러에 돌려주는 내부 결과 객체로, 클라이언트에 나가는 `dto.response`와 구분한다(예: 로그인에서 서비스는 두 토큰을 담은 `LoginResult`를 반환하고, 컨트롤러가 access 토큰만 담은 `LoginResponse`로 변환). 단, 응답이 result와 사실상 동일하고 숨길 필드가 없는 단건 조회는 별도 `dto.response`를 두지 않고 `result`를 그대로 응답으로 내보내기도 한다. (게시글 상세가 원래 이 예외였으나, 태그가 붙으며 `PostDetailResult`(프로젝션)를 `PostDetailResponse`로 감싸게 되어 예외에서 빠졌다 — "태그" 참고. 규약은 "그때그때 최적"을 우선하며 상황이 바뀌면 갱신한다.) 인증은 JWT 기반이며 관련 공통 인프라는 `global/auth` 아래에 있다(아래 "인증 & 토큰" 참고).

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

앱은 `localhost:3306`의 MySQL 8, 데이터베이스 `leisure-local`, 계정/비번 `root`/`root`가 필요하다(`application-local.yml` 참고). 로컬 인프라(MySQL 8 + Redis 7)는 `docker/docker-compose-local.yaml`로 함께 띄운다(`docker compose -f docker/docker-compose-local.yaml up -d`). MySQL은 `utf8mb4` + `utf8mb4_0900_ai_ci`(MySQL 8 기본, accent/case-insensitive) 콜레이션으로 뜬다 — 대소문자 무시 비교라 이메일 정규화와 이중 안전이 되고 닉네임 `John`/`john`도 중복 취급된다. README.md에 Colima + Docker(macOS) 기반 세팅이 정리돼 있으나 **수동 `docker run` 예시는 구버전(DB명 `leisure`)이라 현재는 위 compose 사용이 정본**이다. `local` 프로파일은 `ddl-auto: create`라서 **매 기동 시 모든 테이블을 삭제 후 재생성한다** — 중요한 데이터를 `local` 프로파일에 연결하지 말 것. 기본(default) 프로파일용 datasource가 없으므로, `--spring.profiles.active=local`(혹은 다른 프로파일) 없이 실행하면 JPA가 커넥션을 요구하는 순간 기동에 실패한다.

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

- **이메일 정규화** — 이메일은 `Member.normalizeEmail`(trim + `toLowerCase(Locale.ROOT)`)로 소문자 정규화해 **저장/중복검사/로그인 조회/이메일 중복확인 4곳이 모두 같은 형태**를 쓴다(대소문자 달라도 로그인·중복검사 일관). 콜레이션(`_ci`)과 무관하게 앱 레벨에서 결정적. (비밀번호 규칙은 영문(대소문자 무관)+숫자+특수문자 8~20자 — 대문자 강제는 제거됨.)
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

`com.leisure.post`. 작성은 **즉시 생성 모델**이다 — `POST /posts`가 빈 글을 `WRITING` 상태로 생성하고 `post_id`를 즉시 발급(`Post.startWriting(memberId)`)한다. 이후 `PATCH /posts/{postId}`(저장, `saveDraft`)와 `PATCH /posts/{postId}/publish`(게시, `publish`)가 그 글을 갱신한다. 게시된 글 수정은 `PATCH /posts/{postId}/content`(`editPost`), 삭제는 `DELETE /posts/{postId}`(초안은 하드 삭제, 게시글은 소프트 삭제 — 아래 "삭제" 참고). 작성자는 `@CurrentMember`의 `publicId`로 주입되며 body에 넣지 않는다.

- **상태(`PostStatus`)** — `WRITING`/`DRAFT`/`PENDING`/`PUBLISHED`/`REJECTED`. 상태 변경은 반드시 도메인 전이 메서드로만 한다(setter 금지): `markAsDraft`(WRITING→DRAFT), `submitForApproval`(→PENDING), `approve`(PENDING→PUBLISHED), `reject`(PENDING→REJECTED), `publish`(WRITING/DRAFT→PUBLISHED). 내용 반영은 `applyContent(title, content, category, location)` — **null=유지, ""=비우기**의 부분 갱신이고, 편집 불가 상태(PENDING/PUBLISHED)면 `POST_NOT_EDITABLE`을 던진다(`isEditable`=WRITING/DRAFT/REJECTED).
- **저장(`saveDraft`)** — 무검증(permissive). 제목/본문/카테고리가 전부 비어도 성공하며 WRITING이면 DRAFT로 승격. `PostSaveRequest`는 `@Size(max=50)`만 두고 필수검증은 하지 않는다.
- **게시(`publish`)** — 제목 필수(`POST_TITLE_REQUIRED`). 게시 요청이 그 순간의 내용을 body로 실어보내 `applyContent` 후 상태 전이하므로, 저장을 한 번도 안 거쳐도 `WRITING`에서 바로 게시된다.
- **게시글 수정(`editPublished`)** — 이미 게시된(PUBLISHED) 글 전용 전이 메서드. **PUBLISHED가 아니면 `POST_NOT_EDITABLE`**, 제목을 빈값으로 만들려 하면 `POST_TITLE_REQUIRED`. 상태는 PUBLISHED 그대로 유지(재심사 없음, 티스토리식 즉시 반영). 임시저장 경로(`applyContent`)와 별도로 둔 이유는 AI 도입 시 게시글 수정만 재심사가 필요할 수 있어서다. 제목은 두 경로 모두 `trim()` 후 저장한다.
- **삭제(`deletePost`)** — 상태로 분기한다. **초안(`Post.isDraft()`=WRITING/DRAFT)은 하드 삭제** — 게시된 적 없어 좋아요/북마크 참조가 없으므로, 자식인 태그(`TagRepository.deleteByPostId`)만 정리하고 `repository.delete(post)`로 물리 삭제(같은 트랜잭션이라 원자적). **게시글(PUBLISHED)은 소프트 삭제**(`BaseSoftDeleteEntity.delete()`, `deleted_at` 기록). 소프트 삭제분 제외는 `BaseSoftDeleteEntity`의 `@SQLRestriction("deleted_at is null")`이 **모든 Post 조회에 자동 적용**한다(기존 QueryDSL의 명시적 `deletedAt.isNull()`은 이제 중복이지만 무해). TODO: 소프트 삭제된 게시글은 배치로 일괄 하드 삭제하고 태그·좋아요·북마크도 같은 생명주기로 함께 배치 삭제한다.
- **비정규화 카운트** — `viewCount`/`likeCount`/`bookmarkCount`는 Post에 두고, 증감은 도메인 메서드가 아니라 리포지토리의 원자적 `@Modifying` 쿼리(`increaseLikeCount` 등)로 처리한다.
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

## 좋아요 / 북마크 (postLike, Bookmark)

`com.leisure.postLike`, `com.leisure.Bookmark`. 구조가 동일하다 — 회원↔게시글 조인테이블(`PostLike`/`PostBookmark`, id-only 참조) + 토글 API + 내 목록 조회.

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
- **태그 병합 테스트는 아직 없음** — 쿼리서비스 유닛테스트는 어셈블러를 목으로 두고 조회 흐름만 검증하므로, `TagReader`/어셈블러의 **병합 로직 전용 테스트(예: `TagReaderTest`)는 미작성**이다(향후 보강 대상).
- **미정** — 태그 **개별 길이 제한**(요청은 `@Size(max=5)`로 개수만 제한)과 DB 컬럼 length는 아직 안 정함. ES 색인 연동도 향후.

## Git & CI 워크플로우

브랜치 흐름은 `feature/*` → `dev` (→ `main`). `dev`로의 PR은 CI 워크플로우를 트리거한다: 단위 테스트 실행 후, 자동 Claude Sonnet 코드 리뷰가 한국어 리뷰 댓글을 남긴다. 커밋 메시지는 `[브랜치][타입]: 설명` 형식을 따른다(예: `[feature/ci][feat]: ...`), 타입은 `feat` / `fix` / `chore` 등, 설명은 한국어로.
