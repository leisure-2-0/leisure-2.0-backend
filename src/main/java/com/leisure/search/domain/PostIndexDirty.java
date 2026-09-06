package com.leisure.search.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "post_index_dirties")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostIndexDirty {

    @Id
    @Column(name = "post_id", comment = "재색인 대상 게시글 ID (글당 1행, upsert 자연키)")
    private Long postId;

    @Column(name = "version", nullable = false, comment = "ES external version 겸 삭제 가드 토큰 (markDirty마다 +1 단조 증가)")
    private long version;

    @Column(name = "retry_count", nullable = false, comment = "색인 연속 실패 횟수 (백오프/격리 판단)")
    private int retryCount;

    @Column(name = "dirtied_at", nullable = false, columnDefinition = "DATETIME(6)", comment = "마지막 더티 시각 (처리 순서/관측용)")
    private LocalDateTime dirtiedAt;

    @Column(name = "next_retry_time", nullable = false, columnDefinition = "DATETIME(6)", comment = "다음 처리 가능 시각 (백오프; 배치는 이 시각 지난 행만 처리)")
    private LocalDateTime nextRetryTime;
}
