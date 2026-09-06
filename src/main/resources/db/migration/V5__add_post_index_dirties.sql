create table post_index_dirties (
    post_id bigint not null comment '재색인 대상 게시글 ID (글당 1행, upsert 자연키)',
    version bigint not null comment 'ES external version 겸 삭제 가드 토큰 (markDirty마다 +1 단조 증가)',
    retry_count integer not null comment '색인 연속 실패 횟수 (백오프/격리 판단)',
    dirtied_at datetime(6) not null comment '마지막 더티 시각 (처리 순서/관측용)',
    next_retry_time datetime(6) not null comment '다음 처리 가능 시각 (백오프; 배치는 이 시각 지난 행만 처리)',
    primary key (post_id)
) engine=InnoDB;
