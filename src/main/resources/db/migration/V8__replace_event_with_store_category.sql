-- 게시글 카테고리에서 EVENT(행사) 제거 → STORE(가게) 추가
-- (V7에서 EVENT로 넣었으나 요구사항 변경으로 행사 삭제·가게 신설)
-- 최종 9개: RESTAURANT, CAFE, ACCOMMODATION, ACTIVITY, EXPERIENCE, SCENERY, FESTIVAL, STORE, ETC
--
-- ⚠️ DB enum 값은 알파벳순 나열(Hibernate validate 규약).

-- 1) STORE 추가 + 기존 EVENT를 임시로 유지(데이터 이관을 위해)
ALTER TABLE posts
    MODIFY COLUMN category ENUM(
        'ACCOMMODATION','ACTIVITY','CAFE','ETC','EVENT','EXPERIENCE','FESTIVAL','RESTAURANT','SCENERY','STORE'
    );

-- 2) 기존 EVENT 데이터를 ETC(기타)로 이관 (행사 카테고리 폐기 — 가게와 의미가 달라 기타로 흡수)
UPDATE posts SET category = 'ETC' WHERE category = 'EVENT';

-- 3) EVENT 제거 → 최종 9개(알파벳순)
ALTER TABLE posts
    MODIFY COLUMN category ENUM(
        'ACCOMMODATION','ACTIVITY','CAFE','ETC','EXPERIENCE','FESTIVAL','RESTAURANT','SCENERY','STORE'
    );
