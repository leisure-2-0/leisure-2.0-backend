create table point_histories (
    point_history_id bigint not null auto_increment,
    member_id bigint not null comment '포인트 수령자(게시글 작성자) 회원 ID',
    amount integer not null comment '적립 포인트 (적립 시점 정책 점수 보존)',
    actor_id bigint not null comment '행위자 ID (좋아요/북마크 누른 사람, 게시는 작성자 본인)',
    source_id bigint not null comment '적립 트리거 대상 ID (게시글 ID)',
    point_type varchar(30) not null comment '적립 사유 (POST_PUBLISH/LIKE_RECEIVED/BOOKMARK_RECEIVED)',
    created_at datetime(6) not null,
    primary key (point_history_id),
    constraint uk_point_histories_actor_source_type unique (actor_id, source_id, point_type)
) engine=InnoDB;

alter table members
    add column point integer not null default 0 comment '보유 여가 포인트';
