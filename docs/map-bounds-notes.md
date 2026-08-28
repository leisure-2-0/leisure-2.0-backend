# 지도 Bounds 위경도 검증 정리

## 배경

`GET /maps/pins`는 현재 지도 화면에 보이는 게시글 핀을 조회하는 API다.

클라이언트는 지도의 현재 화면 영역(bounds)을 서버에 전달한다.

```http
GET /maps/pins?minLat=37.4&maxLat=37.6&minLng=126.9&maxLng=127.1
```

서버는 이 bounds 안에 포함되는 게시글 좌표를 조회한다.

## Bounds와 위경도

지도 bounds는 현재 화면에 보이는 사각형 영역이다.

방향과 파라미터 매핑은 다음과 같다.

| 방향 | 의미 | 파라미터 |
| --- | --- | --- |
| South | 남쪽 경계 위도 | `minLat` |
| North | 북쪽 경계 위도 | `maxLat` |
| West | 서쪽 경계 경도 | `minLng` |
| East | 동쪽 경계 경도 | `maxLng` |

위도(latitude)는 북쪽으로 갈수록 커지고, 남쪽으로 갈수록 작아진다.

경도(longitude)는 동쪽으로 갈수록 커지고, 서쪽으로 갈수록 작아진다.

따라서 정상적인 bounds에서는 다음 조건이 성립해야 한다.

```text
minLat <= maxLat
minLng <= maxLng
```

DB 조회 조건은 현재 코드처럼 해석된다.

```java
latitude >= minLat
latitude <= maxLat
longitude >= minLng
longitude <= maxLng
```

즉 게시글 좌표가 현재 지도 화면 사각형 안에 있는지 확인하는 조건이다.

## 좌표 유효 범위

위도와 경도에는 좌표계 자체의 물리적 유효 범위가 있다.

위도는 적도를 기준으로 남북 위치를 나타낸다.

```text
남극 = -90
적도 = 0
북극 = 90
```

따라서 위도는 다음 범위를 벗어나면 안 된다.

```text
-90 <= latitude <= 90
```

경도는 본초 자오선을 기준으로 동서 위치를 나타낸다.

```text
서쪽 최대 = -180
본초 자오선 = 0
동쪽 최대 = 180
```

따라서 경도는 다음 범위를 벗어나면 안 된다.

```text
-180 <= longitude <= 180
```

이 검증은 "우리 서비스가 어느 지역까지 지원할 것인가"를 정하는 정책 검증이 아니다.
클라이언트가 보낸 값이 좌표로 성립하는지를 보는 최소 검증이다.

## 기본 검증

`/maps/pins`는 네 좌표가 모두 있어야 정상 동작한다.

현재 컨트롤러는 `@RequestParam double`로 받기 때문에 누락된 필수 파라미터는 Spring이 먼저 막는다.

서비스에서는 좌표 값 자체와 bounds 관계를 검증한다.

```java
private static final double MIN_LATITUDE = -90.0;
private static final double MAX_LATITUDE = 90.0;
private static final double MIN_LONGITUDE = -180.0;
private static final double MAX_LONGITUDE = 180.0;

private void validateBounds(double minLat, double maxLat, double minLng, double maxLng) {
    if (!Double.isFinite(minLat) || !Double.isFinite(maxLat)
            || !Double.isFinite(minLng) || !Double.isFinite(maxLng)) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    if (minLat < MIN_LATITUDE || minLat > MAX_LATITUDE
            || maxLat < MIN_LATITUDE || maxLat > MAX_LATITUDE) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    if (minLng < MIN_LONGITUDE || minLng > MAX_LONGITUDE
            || maxLng < MIN_LONGITUDE || maxLng > MAX_LONGITUDE) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
    }

    if (minLat > maxLat || minLng > maxLng) {
        throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
    }
}
```

이 검증이 막는 값은 다음과 같다.

- `NaN`, `Infinity`
- 위도 범위 초과
- 경도 범위 초과
- 남쪽 위도가 북쪽 위도보다 큰 bounds
- 서쪽 경도가 동쪽 경도보다 큰 bounds

## Bounds 크기 제한

좌표 자체가 유효하더라도, 너무 넓은 bounds로 `/maps/pins`를 호출하면 문제가 된다.

예를 들어 다음 요청은 좌표로는 정상이다.

```http
GET /maps/pins?minLat=-90&maxLat=90&minLng=-180&maxLng=180
```

하지만 이 요청은 전 세계를 대상으로 개별 핀 조회를 시도한다.

문제는 다음과 같다.

- DB가 지나치게 넓은 범위를 훑을 수 있다.
- 서버가 `limit 500`으로 잘라도, 프론트는 결과가 전체인지 일부인지 알기 어렵다.
- 낮은 줌 레벨에서는 개별 핀보다 지역 클러스터가 UX에 맞다.

따라서 `/maps/pins`에는 한 번에 허용할 bounds 크기 제한을 두는 것이 좋다.

```java
// TODO: 프론트 줌 정책 확정 후 조정. 현재는 시 단위 화면 정도를 허용한다.
private static final double MAX_LATITUDE_SPAN = 0.7;
private static final double MAX_LONGITUDE_SPAN = 0.7;

if (maxLat - minLat > MAX_LATITUDE_SPAN || maxLng - minLng > MAX_LONGITUDE_SPAN) {
    throw new BusinessException(ErrorCode.INVALID_REQUEST_PARAMETER);
}
```

`maxLat - minLat`은 화면의 남북 높이다.

`maxLng - minLng`은 화면의 동서 너비다.

예를 들어:

```text
minLat = 37.4
maxLat = 37.6
minLng = 126.9
maxLng = 127.1

lat span = 0.2
lng span = 0.2
```

이는 위경도 기준으로 `0.2 x 0.2` 크기의 지도 화면을 의미한다.

다만 실제 거리로는 정사각형이 아닐 수 있다.

- 위도 1도는 어디서든 대략 111km다.
- 경도 1도는 위도에 따라 달라진다.
- 한국 위도에서는 경도 1도가 대략 88km 안팎이다.

따라서 한국 근처에서 `0.2 x 0.2` bounds는 아주 대략 세로 22km, 가로 18km 정도다.

현재 임시값 `0.7 x 0.7`은 시 단위 화면 정도를 허용하는 기준이다.
정확한 값은 프론트 지도 줌 정책과 실제 데이터량을 보고 조정해야 한다.

## Frontend와 Backend 역할

프론트는 지도 UX에 맞게 어떤 API를 호출할지 결정한다.

- 많이 줌아웃된 상태: `/maps/regions` 호출
- 충분히 줌인된 상태: `/maps/pins` 호출
- 지도 이동 중 매 프레임 호출하지 않고, `idle`, `dragend`, `zoom_changed` 이후 호출
- 연속 이동에는 debounce 적용
- 새 bounds 요청이 생기면 이전 요청 취소

백엔드는 직접 API 호출이나 프론트 버그에 대비해 최소 안전장치를 둔다.

- 좌표 유효성 검증
- bounds 순서 검증
- `/maps/pins` 전용 bounds 크기 제한

## 현재 추천 방향

초기 구현은 다음 순서가 적절하다.

1. `MapQueryService`에서 좌표 유효성 검증을 한다.
2. `MapQueryService`에서 bounds 크기 제한을 임시 정책값으로 둔다.
3. 컨트롤러는 기존 `@RequestParam` 방식 그대로 유지한다.
4. 프론트 줌 정책이 확정되면 `MAX_LATITUDE_SPAN`, `MAX_LONGITUDE_SPAN` 값을 조정한다.
5. 이후 필요하면 응답을 `pins`, `hasMore`, `limit` 형태로 감싸서 500개 제한에 걸렸는지도 알려준다.

## 정렬 없는 500개와 최신순 500개

`/maps/pins`는 현재 최대 500개만 반환한다.

중요한 지점은 bounds 안의 핀이 500개 이하인지, 500개를 초과하는지다.

```text
bounds 안 핀 <= 500
-> 정렬 없음과 최신순 정렬의 차이가 작다.
-> 어차피 조건에 맞는 핀 대부분 또는 전부를 내려준다.

bounds 안 핀 > 500
-> limit 때문에 일부만 내려줘야 한다.
-> 이때 "어떤 500개를 줄 것인가"가 API 정책이 된다.
```

정렬이 없는 현재 방식은 다음과 같다.

```java
.limit(MAP_PINS_LIMIT)
.fetch();
```

장점은 DB 부하가 상대적으로 낮다는 점이다.
DB는 조건에 맞는 행을 찾다가 500개를 채우면 멈출 수 있다.

단점은 어떤 500개가 내려오는지 설명하기 어렵다는 점이다.
SQL에서 `orderBy`가 없으면 결과 순서는 계약이 아니다.
같은 DB 상태에서는 비슷하게 나올 수 있지만, 보장되지는 않는다.

결과가 달라질 수 있는 이유는 다음과 같다.

- DB 실행 계획이나 선택한 인덱스가 달라질 수 있다.
- DB 통계 상태나 데이터 변경에 따라 먼저 읽히는 row가 달라질 수 있다.
- 디바이스 화면 크기와 비율 차이 때문에 같은 지역을 보더라도 bounds가 미세하게 다를 수 있다.
- 요청 사이에 새 게시글 등록, 삭제, 상태 변경이 발생할 수 있다.

따라서 orderBy 없이 500개를 자르면, 후보가 500개를 넘는 지역에서는 여러 디바이스가 비슷한 지역을 보고 있어도 일부 핀 구성이 달라질 수 있다.

최신순 정렬을 넣으면 다음과 같다.

```java
.orderBy(post.publishedAt.desc(), post.postId.desc())
.limit(MAP_PINS_LIMIT)
.fetch();
```

장점은 선택 기준이 명확해진다는 점이다.

```text
현재 지도 화면 안에서 가장 최근에 게시된 500개
```

같은 bounds와 같은 DB 상태라면 같은 결과를 기대할 수 있다.
`postId desc`는 `publishedAt`이 같은 경우의 tie-breaker다.

단점은 DB 정렬 비용이 생길 수 있다는 점이다.
정렬 비용은 500개를 정렬해서 생기는 것이 아니라, bounds 조건에 맞는 후보들을 찾고 그중 상위 500개를 고르는 과정에서 생긴다.

```text
후보 80개
-> 정렬 비용 거의 작음

후보 2,000개
-> 정렬 비용 발생

후보 50,000개
-> 인덱스 없이 부담이 커질 수 있음
```

정리하면 다음과 같다.

| 선택 | 장점 | 단점 | 의미 |
| --- | --- | --- | --- |
| 정렬 없음 | DB 부하가 가장 낮을 가능성이 큼 | 어떤 500개인지 설명하기 어려움 | 조건에 맞는 것 중 DB가 먼저 찾은 500개 |
| 최신순 정렬 | 결과 선택 기준이 명확함 | DB 정렬 비용이 생길 수 있음 | 현재 화면 안의 최신 게시글 500개 |

지도 핀은 리스트가 아니라 시각화이기 때문에, 최신순이 항상 정답은 아니다.
정말로 아무 500개면 충분한 MVP라면 정렬 없음도 타당하다.

다만 500개 제한이 자주 걸리고 "왜 이 핀은 보이고 저 핀은 안 보이는가"를 설명해야 한다면, 최신순이나 다른 명시적 선택 기준이 필요하다.

## 500개 제한값에 대한 관점

`MAP_PINS_LIMIT = 500`은 정답이 아니라 초기 상한값이다.

성능은 DB만 보면 안 된다.
지도 핀 조회는 다음 세 곳에서 비용이 생긴다.

- DB: bounds 조건, 정렬, limit 처리
- 네트워크: JSON 응답 크기
- 프론트: 지도 마커 렌더링

제한값별 트레이드오프는 다음과 같다.

| 제한값 | 장점 | 단점 |
| --- | --- | --- |
| 100 | DB, 네트워크, 프론트 렌더링이 가벼움 | 핀이 많은 지역에서 누락 체감이 클 수 있음 |
| 500 | 시 단위 bounds에서 비교적 넉넉함 | 데이터가 많은 지역에서는 여전히 잘릴 수 있음 |
| 1000 | 누락은 줄어듦 | 응답 크기와 마커 렌더링 부담이 커짐 |

현재 `0.7 x 0.7` bounds 제한과 함께라면, 500개는 MVP에서 시작하기 좋은 임시값이다.
실제 운영 데이터와 프론트 렌더링 성능을 보고 조정해야 한다.

추후 판단 기준은 다음처럼 잡을 수 있다.

- 프론트 마커 렌더링이 버거우면 300 또는 200으로 낮춘다.
- 누락 체감이 크면 `hasMore` 응답을 추가하고 1000까지 검토한다.
- DB 응답이 느리면 인덱스, 클러스터링, 격자 샘플링을 검토한다.
