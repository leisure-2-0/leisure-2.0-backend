package com.leisure.member.event;

import com.leisure.global.auth.store.RedisRefreshTokenStore;
import com.leisure.global.auth.store.RedisTokenStatusStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberWithdrawnEventListener {

    private final RedisTokenStatusStore tokenStatusStore;

    private final RedisRefreshTokenStore  refreshTokenStore;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMemberWithdrawn(MemberWithdrawnEvent event) {
        tokenStatusStore.increaseInvalidationVersion(event.publicId());
        refreshTokenStore.remove(event.publicId());
    }
}
