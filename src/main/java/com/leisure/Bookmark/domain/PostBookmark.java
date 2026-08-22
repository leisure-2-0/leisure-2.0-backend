package com.leisure.Bookmark.domain;

import com.leisure.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_bookmarks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_post_bookmark_member_post",
                columnNames = {"member_id", "post_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostBookmark extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_bookmark_id")
    private Long postBookmarkId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    private PostBookmark(Long memberId, Long postId) {
        this.memberId = memberId;
        this.postId = postId;
    }

    public static PostBookmark of(Long memberId, Long postId) {
        return new PostBookmark(memberId, postId);
    }
}
