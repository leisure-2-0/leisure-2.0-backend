package com.leisure.post.domain;

public enum PostStatus {
    WRITING,   // 작성 중
    DRAFT,     // 임시 저장(자동/수동)
    PENDING,   // 승인 대기
    PUBLISHED, // 게시 완료
    REJECTED,  // 승인 반려
}
