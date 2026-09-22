package com.leisure.member.event;

import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.index.PostIndexDirtyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberWithdrawnEventListener {

    private final RedisTokenStatusStore tokenStatusStore;

    private final RedisRefreshTokenStore  refreshTokenStore;

    private final PostRepository postRepository;

    private final PostIndexDirtyRepository postIndexDirtyRepository;


    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberWithdrawn(MemberWithdrawnEvent event) {
        tokenStatusStore.increaseInvalidationVersion(event.publicId());
        refreshTokenStore.remove(event.publicId());

        List<Long> postIds = postRepository.findPublishedPostIdsByMemberId(event.memberId());

        postRepository.softDeleteByMemberId(event.memberId());

        for (Long postId : postIds) {
            postIndexDirtyRepository.markDirty(postId);
        }
    }
}
