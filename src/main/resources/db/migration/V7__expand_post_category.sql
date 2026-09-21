-- 게시글 카테고리 enum 확장: 4개 → 9개, HOTEL → ACCOMMODATION 리네임
-- 기존: ACTIVITY, HOTEL, RESTAURANT, SCENERY
-- 신규: RESTAURANT(식당), CAFE(카페), ACCOMMODATION(숙소), ACTIVITY(액티비티),
--       EXPERIENCE(체험), SCENERY(풍경/명소), FESTIVAL(축제), EVENT(행사), ETC(기타)
--
-- ⚠️ DB enum 값은 알파벳순으로 나열한다 — Hibernate @Enumerated(STRING)이
--    네이티브 enum 컬럼을 validate할 때 알파벳순을 기대하기 때문(V1의 status/category와 동일 규약).

-- 1) 신규 값 추가 + 기존 HOTEL을 임시로 유지(데이터 이관을 위해)
ALTER TABLE posts
    MODIFY COLUMN category ENUM(
        'ACCOMMODATION','ACTIVITY','CAFE','ETC','EVENT','EXPERIENCE','FESTIVAL','HOTEL','RESTAURANT','SCENERY'
    );

-- 2) 기존 HOTEL 데이터를 ACCOMMODATION으로 이관
UPDATE posts SET category = 'ACCOMMODATION' WHERE category = 'HOTEL';

-- 3) HOTEL 제거 → 최종 9개(알파벳순)
ALTER TABLE posts
    MODIFY COLUMN category ENUM(
        'ACCOMMODATION','ACTIVITY','CAFE','ETC','EVENT','EXPERIENCE','FESTIVAL','RESTAURANT','SCENERY'
    );
