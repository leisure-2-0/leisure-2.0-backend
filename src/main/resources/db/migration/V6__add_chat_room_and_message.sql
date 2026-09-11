create table chat_rooms (
    chat_room_id bigint not null auto_increment,
    member_id    bigint not null comment '방 소유자 회원 ID (id-only 참조, FK 없음)',
    title        varchar(255) not null comment '대화 제목 (첫 질문 요약, 목록 표시용)',
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null comment '최근 대화 시각 (내 대화 목록 정렬용)',
    primary key (chat_room_id)
) engine=InnoDB;

create table chat_messages (
    chat_message_id bigint not null auto_increment,
    chat_room_id    bigint not null comment '소속 대화방 ID (id-only 참조, FK 없음)',
    content         text not null comment '메시지 내용',
    message_role    enum('ASSISTANT','USER') not null comment '화자 (USER/ASSISTANT)',
    message_status  enum('COMPLETED','FAILED','INTERRUPTED') not null comment '스트림 상태 (완료/실패/중단)',
    created_at      datetime(6) not null,
    primary key (chat_message_id)
) engine=InnoDB;
