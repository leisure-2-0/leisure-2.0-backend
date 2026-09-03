
    create table festivals (
        event_end_date date comment '행사 종료일',
        event_start_date date comment '행사 시작일',
        latitude float(53) comment '위도, TourAPI mapy',
        longitude float(53) comment '경도, TourAPI mapx',
        created_at datetime(6) not null,
        festival_id bigint not null auto_increment,
        tour_modified_at datetime(6) comment 'TourAPI 콘텐츠 수정일시, modifiedtime — 상세 보강 배치의 델타 감지 기준',
        updated_at datetime(6) not null,
        homepage_url varchar(1000) comment '공식 홈페이지 URL',
        overview varchar(3000) comment '축제/행사 소개',
        address varchar(255) comment '기본 주소, TourAPI addr1',
        content_type_id varchar(255) comment 'TourAPI 콘텐츠 타입 ID',
        detail_address varchar(255) comment '상세 주소, TourAPI addr2',
        event_time varchar(255) comment '행사 운영 시간',
        lcls_systm2 varchar(255) comment 'TourAPI 중분류 코드 lclsSystm2 (축제/공연/행사)',
        lcls_systm3 varchar(255) comment 'TourAPI 소분류 코드 lclsSystm3 (전시회/박람회/스포츠경기/기타행사)',
        ldong_regn_cd varchar(255) comment '법정동 광역 코드 (Region 조인 키)',
        ldong_signgu_cd varchar(255) comment '법정동 시군구 코드 (Region 조인 키)',
        name varchar(255) not null comment '축제/행사명',
        tour_content_id varchar(255) not null comment 'TourAPI 콘텐츠 ID, 배치 동기화 기준',
        primary key (festival_id)
    ) engine=InnoDB;

    create table members (
        created_at datetime(6) not null,
        deleted_at datetime(6),
        member_id bigint not null auto_increment,
        updated_at datetime(6) not null,
        public_id varchar(36) not null,
        nickname varchar(50) not null,
        profile_image_url varchar(500),
        email varchar(255) not null,
        password varchar(255) not null,
        primary key (member_id)
    ) engine=InnoDB;

    create table post_bookmarks (
        created_at datetime(6) not null,
        member_id bigint not null,
        post_bookmark_id bigint not null auto_increment,
        post_id bigint not null,
        primary key (post_bookmark_id)
    ) engine=InnoDB;

    create table post_likes (
        created_at datetime(6) not null,
        member_id bigint not null,
        post_id bigint not null,
        post_like_id bigint not null auto_increment,
        primary key (post_like_id)
    ) engine=InnoDB;

    create table post_tags (
        post_id bigint not null,
        post_tag_id bigint not null auto_increment,
        post_tag_name varchar(255) not null,
        primary key (post_tag_id)
    ) engine=InnoDB;

    create table posts (
        bookmark_count integer not null,
        latitude float(53),
        like_count integer not null,
        longitude float(53),
        view_count integer not null,
        created_at datetime(6) not null,
        deleted_at datetime(6),
        member_id bigint not null,
        post_id bigint not null auto_increment,
        published_at datetime(6),
        updated_at datetime(6) not null,
        title varchar(50),
        address varchar(255),
        content TEXT,
        place_name varchar(255),
        region varchar(255),
        category enum ('ACTIVITY','HOTEL','RESTAURANT','SCENERY'),
        status enum ('DRAFT','PENDING','PUBLISHED','REJECTED','WRITING') not null,
        primary key (post_id)
    ) engine=InnoDB;

    create table regions (
        created_at datetime(6) not null,
        region_id bigint not null auto_increment,
        updated_at datetime(6) not null,
        ldong_regn_cd varchar(255) not null comment '법정동 광역 코드 (예: 51 강원)',
        ldong_signgu_cd varchar(255) not null comment '법정동 시군구 코드(예: 150 강릉)',
        regn_name varchar(255) not null comment '광역명 (예: 강원특별자치도)',
        signgu_name varchar(255) not null comment '시군구명 (예: 강릉시)',
        primary key (region_id)
    ) engine=InnoDB;

    alter table festivals 
       add constraint UKc4awolrk6pms1fhr0dhtfdrc9 unique (tour_content_id);

    alter table members 
       add constraint UKd4soq3alpq6p09h1g5av6dov8 unique (public_id);

    alter table members 
       add constraint UKe6u9u9ypoc7oldnpxdjwcdx3 unique (nickname);

    alter table members 
       add constraint UK9d30a9u1qpg8eou0otgkwrp5d unique (email);

    alter table post_bookmarks 
       add constraint uk_post_bookmark_member_post unique (member_id, post_id);

    alter table post_likes 
       add constraint uk_post_like_member_post unique (member_id, post_id);

    alter table post_tags 
       add constraint uk_post_tags_post_id_tag unique (post_id, post_tag_name);

    alter table regions 
       add constraint uk_regions_area_sigungu unique (ldong_regn_cd, ldong_signgu_cd);
