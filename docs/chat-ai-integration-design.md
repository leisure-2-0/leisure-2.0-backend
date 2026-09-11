# 채팅 API ↔ AI 서버 연동 설계

브라우저 채팅 → 우리 API → AI 서버(SSE 스트리밍) → 응답 반환. 대화 맥락(최근 3턴)을 함께 전달하기 위해 대화를 RDB에 영속화한다. **이 문서는 구현 전 설계**라, 마지막 "예상 이슈"는 겪은 트러블슈팅이 아니라 **대비할 지점**이다.

관련: AI 서버는 별도 저장소 `leisure-2.0-AI`(Gradle/Java, SSE 스트리밍 컨트롤러).

---

## 1. 문제 정의

브라우저가 채팅 메시지를 보내면, 우리 API가 **그 메시지 + 최근 대화 맥락을 AI 서버에 전달**하고, AI가 SSE로 흘려주는 응답을 받아 브라우저로 다시 흘린다. 핵심 두 가지:

- **단순 프록시가 아니라 문맥 유지** — AI가 이전 대화를 알아야 자연스러운 답이 나오므로 **최근 3턴을 함께** 보낸다.
- 그 문맥을 만들려면 **대화 내역을 RDB에 영속화**해야 한다(저장 + 최근 N 조회).

즉 **"사용자 메시지 저장 → 최근 맥락 조립 → AI 스트리밍 호출 → 응답 저장 → 브라우저로 스트림"** 파이프라인.

---

## 2. 요구사항 분석

**기능**
| # | 요구사항 |
|---|---|
| F1 | 채팅 메시지 전송 엔드포인트(사용자 메시지 수신) |
| F2 | 사용자 메시지 RDB 저장(role=USER) |
| F3 | 해당 방의 최근 3턴(=6메시지) 조회 |
| F4 | [현재 메시지 + 최근 3턴]을 AI 서버로 전달 |
| F5 | AI 응답 수신(SSE) → RDB 저장(role=ASSISTANT) |
| F6 | AI 응답을 브라우저로 스트리밍 반환 |

**비기능 / 제약**
- **지연** — LLM 응답은 수 초. 한 방에 받지 않고 **토큰 단위 스트리밍(SSE)**.
- **AI 장애** — 타임아웃·스트림 중단·에러 시 상태 표시 정책 필요.
- **소유권** — 내 대화만 조회·저장(인증 필수).

---

## 3. 데이터 흐름

```
브라우저 ── 메시지 ──▶ [우리 API]
                         1) 사용자 메시지 저장(role=USER) ──▶ 커밋
                         2) 방의 최근 3턴(6개) 조회
                         3) [메시지 + 최근3턴] ──▶ [AI 서버 /SSE]
                                                    ◀── 토큰 스트림
                         4) 스트림을 브라우저로 흘리며(6)
                            끝나면 assistant 메시지 저장(role=ASSISTANT) ──▶ 커밋
```

⚠️ **AI 호출(3~4)은 트랜잭션 밖.** 느린 외부 스트리밍에 DB 커넥션을 물리지 않게(ES 색인 때와 같은 원칙). 그래서 tx는 "사용자 메시지 저장"과 "assistant 저장" 둘로 나뉘고, 그 사이 AI 스트림은 tx 없이 돈다.

---

## 4. 설계 결정과 트레이드오프

### 4.1 대화 단위 = `chat_room` (사용자별 여러 방)

"어디까지를 한 덩어리 대화로 볼 거냐"를 `chat_room`이 정한다 — 방 하나 = 대화 하나(ChatGPT 사이드바의 각 대화).

- **왜 방 단위인가** — "최근 3턴"을 자를 때 **어느 방의 3턴이냐**가 중요하다. 방 A 맥락에 방 B가 섞이면 AI가 헷갈린다 → 최근 3턴은 **`room_id` 기준** 조회.
- 대안: "사용자당 대화 하나(단일 연속)"도 가능하지만, 대화 스레드 분리(제목·목록)를 위해 **방 여러 개**를 택함(확장성).
- **최근 3턴 조회** — `WHERE room_id=? ORDER BY created_at DESC LIMIT 6` 후 시간순으로 뒤집기. 인덱스 `(room_id, created_at)`가 받친다.

### 4.2 WebClient vs RestClient → **WebClient**

AI 서버가 **SSE 스트리밍**(토큰 단위)이라, 응답을 **흐름으로 소비**해야 한다.

- `RestClient` = 동기 블로킹. 응답을 다 받고 반환 → 스트림을 토큰 단위로 흘리기에 부적합.
- `WebClient` = 리액티브. `Flux`로 SSE 이벤트를 받아 브라우저로 그대로 중계 가능.

```java
// 설계 예시 — AI SSE 소비 (WebClient)
Flux<String> stream = webClient.post()
        .uri("/chat/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .bodyValue(new AiChatRequest(message, recentTurns))   // 현재 메시지 + 최근 3턴
        .retrieve()
        .bodyToFlux(String.class);                            // 토큰 스트림
```

- **트레이드오프** — 프로젝트 나머지는 동기(RestClient/RestTemplate 계열)인데 여기만 리액티브가 들어온다. 스트리밍이 요구사항이라 불가피. WebFlux 전체 전환이 아니라 **이 연동 지점만** WebClient를 쓰는 국소 도입.

### 4.3 저장 모델 — 정규화(방 1:N 메시지), `referenced_post_ids`만 JSON

```sql
CREATE TABLE chat_room (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,              -- id-only(FK 없음)
    title      VARCHAR(100),                 -- 첫 질문 요약, 목록 표시
    created_at TIMESTAMP,
    updated_at TIMESTAMP,                     -- 최근 대화순 정렬
    INDEX idx_user (user_id, updated_at DESC)
);

CREATE TABLE chat_message (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id  BIGINT NOT NULL,                 -- id-only(FK 없음)
    role     ENUM('USER','ASSISTANT') NOT NULL,
    content  TEXT NOT NULL,
    status   ENUM('COMPLETED','INTERRUPTED','FAILED') DEFAULT 'COMPLETED',
    referenced_post_ids JSON,                 -- AI 답변 근거 게시글(비정규화)
    created_at TIMESTAMP,
    INDEX idx_room (room_id, created_at)
);
```

**왜 방↔메시지는 비정규화(JSON 임베드)하면 안 되나** — 메시지에 실제로 하는 작업이 전부 JSON 임베드에 불리하다:

| 작업 | 메시지를 JSON 배열로(비정규화) | 별도 테이블(정규화) |
|---|---|---|
| 메시지 추가 | 전체 배열 읽고→붙이고→통째 재기록(경합) | INSERT 한 방 |
| 최근 3턴 조회 | 전체 대화 로드 후 앱에서 슬라이스 | 인덱스 `ORDER BY created_at DESC LIMIT 6` |
| status 갱신 | 블롭 통째 재기록 | UPDATE 한 행 |
| 대화가 길어지면 | 블롭 무한정 커짐(크기·성능 절벽) | 행만 증가 |

→ 메시지는 **append·slice·update가 잦고 무한 증가**하는 프로파일이라 **정규화가 맞다.**

**왜 `referenced_post_ids`는 JSON(비정규화)이 맞나** — 정반대 프로파일이다: **작고 유한**(한 답변의 근거 몇 개), **write-once**, **메시지와 항상 같이 읽힘**, **독립 질의 없음**. 이런 데이터는 별도 테이블(조인)보다 **메시지에 JSON으로 붙이는 게 자연스럽다**. (태그를 1:N 테이블로 간 것과 반대 케이스 — 태그는 모든 카드 노출+검색 대상이라 정규화.)

> **원리**: 비정규화는 "같이 읽히고, 안 변하고, 작고 유한한" 데이터에. 방 안의 메시지는 그 반대라 정규화.

### 4.4 참조는 id-only (FK 없음)

`user_id`·`room_id`·`referenced_post_ids` 모두 **id만** 저장(DB FK 없음) — 프로젝트 전체 컨벤션.

- **DDD 경계로 보면** — room↔message는 "애그리거트 안"(방이 메시지를 소유, 생명주기 공유)이라 **교과서적으론 연관관계(+FK cascade)가 정당한 유일한 자리**다. 반면 message→post, room→user는 "밖"이라 id-only가 자연스럽다.
- **그럼에도 id-only로 간 이유** — 이 프로젝트는 `open-in-view=false` + QueryDSL 프로젝션 + N+1/Lazy 회피 스타일이라 **JPA 연관관계와 상성이 나쁘고**, 코드베이스에 연관관계가 하나도 없다. 여기만 연관관계를 넣으면 **외딴 예외**가 되어 유지보수 부담. **일관성**을 택했다.
- **cascade 대체** — 방 삭제 시 `deleteByRoomId` 벌크로 메시지 정리(post 삭제가 태그를 정리하듯). FK-cascade 없이 같은 효과.
- **삭제된 근거 게시글** — `referenced_post_ids`의 id가 붕 뜨는 경우(글 삭제)는 **읽을 때 처리** — 근거 카드를 하이드레이트할 때 살아있는 것만 표시(검색 hydration의 drift-skip과 동일).

### 4.5 실패 처리 — 롤백 대신 `status`

사용자 메시지 저장 후 AI가 실패해도 **롤백하지 않는다.** `status`로 표시:

- 사용자 메시지: 항상 `COMPLETED`(다 친 메시지).
- assistant 메시지:
  - 정상 → `COMPLETED`
  - **스트림 중단**(브라우저 끊김·네트워크) → 받은 데까지 부분 저장 + `INTERRUPTED`
  - **AI 에러/타임아웃** → `FAILED`
- **왜 롤백 안 하나** — 사용자 메시지를 지우면 "무엇을 물었는지"가 사라진다. 채팅 UX는 "답변 실패, 재시도"를 보여주지 메시지를 증발시키지 않는다.
- **재시도** — 기본은 **수동**(사용자가 재전송). LLM 호출은 비싸고 중복 위험이 있어 자동 재시도는 지양(필요 시 뒤에).

### 4.6 트랜잭션 경계 — AI 스트림은 tx 밖

- `tx1`: 사용자 메시지 저장 → **커밋**.
- (tx 없음) AI SSE 스트림 소비 + 브라우저로 중계.
- `tx2`: assistant 메시지 저장 → **커밋**.
- **왜** — AI 스트리밍은 수 초짜리 외부 IO다. tx가 이를 감싸면 그동안 DB 커넥션을 점유해 풀을 낭비한다(ES 색인 tx-밖 원칙과 동일).

---

## 5. 예상 이슈 & 대비 (구현 시 조심할 것)

- **SSE 중단** — 브라우저가 스트림 도중 끊으면 assistant 응답이 미완성. → 받은 토큰까지 저장 + `INTERRUPTED`. 서버가 이를 감지(WebClient `Flux` 취소/에러 시그널)해 상태를 기록.
- **AI 타임아웃** — WebClient 응답/유휴 타임아웃 설정. 초과 시 `FAILED`.
- **tx가 스트림을 감싸는 실수** — 서비스 메서드에 `@Transactional`을 크게 걸면 AI 호출까지 tx에 들어감. 저장 tx를 잘게 나누고 AI 호출은 tx 밖.
- **최근 3턴 경계** — `LIMIT 6`은 "user/assistant 교대"를 가정. 실패로 assistant가 빠진 턴이 섞이면 6개가 3쌍이 아닐 수 있음 → 필요하면 "완료된 턴만" 필터 고려.
- **방↔메시지 비정규화 유혹** — "조인 피하자"고 메시지를 JSON으로 임베드하면 append/slice/update가 다 비싸짐(4.3). 정규화 유지.
- **리액티브 국소 도입** — WebClient만 들어오므로 나머지 동기 코드와 경계에서 블로킹/논블로킹 혼용 주의(중계 지점 설계).

---

## 6. 미구현 / 향후

- 엔드포인트·서비스·엔티티·리포지토리 구현(현재 설계만).
- Flyway 마이그레이션(`chat_room`/`chat_message`).
- 방 목록/대화 조회 API(내 대화 목록, 방별 메시지 페이지네이션).
- 자동 재시도·타임아웃 정책 세부.
- `title` 자동 생성(첫 질문 요약 — AI로?) 정책.
