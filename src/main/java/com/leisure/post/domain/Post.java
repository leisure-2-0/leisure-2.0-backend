package com.leisure.post.domain;

import com.leisure.global.entity.BaseSoftDeleteEntity;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Post extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PostStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private PostCategory category;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount;

    private String region;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    private Post(Long memberId) {
        this.memberId = memberId;
        this.status = PostStatus.WRITING;
    }

    public static Post startWriting(Long memberId) {
        return new Post(memberId);
    }

    public void applyContent(String title, String content, PostCategory category) {

        if (!isEditable()) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        if (title != null) {
            this.title = title.trim();
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
    }

    public void markAsDraft() {
        if (this.status == PostStatus.WRITING) {
            this.status = PostStatus.DRAFT;
        }
    }

    public void submitForApproval() {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.POST_TITLE_REQUIRED);
        }

        if (this.status != PostStatus.WRITING && this.status != PostStatus.DRAFT && this.status != PostStatus.REJECTED) {
            throw new BusinessException(ErrorCode.POST_NOT_SUBMITTABLE);
        }
        this.status = PostStatus.PENDING;
    }

    public void approve() {
        if (this.status != PostStatus.PENDING) {
            throw new BusinessException(ErrorCode.POST_NOT_PENDING);
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void publish() {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.POST_TITLE_REQUIRED);
        }

        if(this.status != PostStatus.WRITING && this.status != PostStatus.DRAFT) {
            throw new BusinessException(ErrorCode.POST_NOT_SUBMITTABLE);
        }
        this.status = PostStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject() {
        if (this.status != PostStatus.PENDING) {
            throw new BusinessException(ErrorCode.POST_NOT_PENDING);
        }
        this.status = PostStatus.REJECTED;
    }

    public boolean isWrittenBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    private boolean isEditable() {
        return status == PostStatus.WRITING || status == PostStatus.DRAFT || status == PostStatus.REJECTED;
    }

    public void editPublished(String title, String content, PostCategory category) {
        if (this.status != PostStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.POST_NOT_EDITABLE);
        }

        if (title != null) {
            String trimmedTitle = title.trim();
            if (trimmedTitle.isEmpty()) {
                throw new BusinessException(ErrorCode.POST_TITLE_REQUIRED);
            }
            this.title = trimmedTitle;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
    }
}
