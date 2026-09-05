package com.leisure.post.event;

/**
 * 게시글 게시 완료 도메인 이벤트. AFTER_COMMIT 리스너가 받아 작성자 포인트 적립으로 넘긴다.
 *
 * @param authorId 작성자(포인트 수령자) 회원 ID
 * @param postId   게시된 게시글 ID (적립 트리거 대상 = sourceId)
 */
public record PostPublishedEvent(Long authorId, Long postId) {
}
