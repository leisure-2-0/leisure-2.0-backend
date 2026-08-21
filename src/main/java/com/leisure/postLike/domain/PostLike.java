package com.leisure.postLike.domain;

import com.leisure.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_likes",
    uniqueConstraints = @UniqueConstraint(
            name = "uk_post_like_member_post",
            columnNames = {"member_id", "post_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostLike extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long postLikeId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    private PostLike(Long memberId, Long postId) {
        this.memberId = memberId;
        this.postId = postId;
    }

    public static PostLike of(Long memberId, Long postId) {
        return new PostLike(memberId, postId);
    }
}
