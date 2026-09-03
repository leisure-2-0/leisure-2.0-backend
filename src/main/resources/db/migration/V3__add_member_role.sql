alter table members
    add column role varchar(20) not null default 'MEMBER' comment '회원 권한 (MEMBER/ADMIN)';
