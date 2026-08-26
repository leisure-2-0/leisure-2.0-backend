package com.leisure.tag.domain;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "post_tags",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_post_tags_post_id_tag",
                    columnNames = {"post_id", "post_tag_name"}
            )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_tag_id")
    private Long postTagId;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "post_tag_name", nullable = false)
    private String tagName;

    private PostTag(Long postId, String tagName) {
        this.postId = postId;
        this.tagName = tagName;
    }

    public static List<PostTag> createAll(Long postId, Set<String> tagNames) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.POST_TAG_INVALID);
        }

        return tagNames.stream()
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .map(name -> new PostTag(postId, name))
                .toList();
    }
}
