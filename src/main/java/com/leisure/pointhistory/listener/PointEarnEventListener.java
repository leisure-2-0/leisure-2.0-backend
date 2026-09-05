package com.leisure.pointhistory.listener;

import com.leisure.bookmark.event.PostBookmarkedEvent;
import com.leisure.global.event.dto.EventEnvelope;
import com.leisure.global.event.dto.EventType;
import com.leisure.global.event.publisher.DomainEventPublisher;
import com.leisure.global.utils.EventIdGenerator;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.dto.PointEarnMessage;
import com.leisure.post.event.PostPublishedEvent;
import com.leisure.postlike.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PointEarnEventListener {

    private final DomainEventPublisher domainEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostPublished(PostPublishedEvent event) {
        publish(EventType.POST_PUBLISHED,
                new PointEarnMessage(event.authorId(), event.authorId(), event.postId(), PointType.POST_PUBLISH));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikedEvent event) {
        if (event.authorId().equals(event.actorId())) {
            return;
        }
        publish(EventType.POST_LIKED,
                new PointEarnMessage(event.authorId(), event.actorId(), event.postId(), PointType.LIKE_RECEIVED));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostBookmarked(PostBookmarkedEvent event) {
        if (event.authorId().equals(event.actorId())) {
            return;
        }
        publish(EventType.POST_BOOKMARKED,
                new PointEarnMessage(event.authorId(), event.actorId(), event.postId(), PointType.BOOKMARK_RECEIVED));
    }

    private void publish(EventType eventType, PointEarnMessage message) {
        String eventId = EventIdGenerator.generate();
        domainEventPublisher.publish(EventEnvelope.of(eventId, eventType, eventId, message));
    }
}
