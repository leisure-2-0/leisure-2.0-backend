package com.leisure.postlike.event;

/**
 * 게시글 좋아요 도메인 이벤트. AFTER_COMMIT 리스너가 받아 작성자 포인트 적립으로 넘긴다(본인 좋아요는 리스너에서 제외).
 *
 * @param authorId 게시글 작성자(포인트 수령자) 회원 ID
 * @param actorId  좋아요를 누른 회원 ID
 * @param postId   대상 게시글 ID (적립 트리거 대상 = sourceId)
 */
public record PostLikedEvent(Long authorId, Long actorId, Long postId) {
}
