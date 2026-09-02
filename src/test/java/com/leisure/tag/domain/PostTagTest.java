package com.leisure.tag.domain;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("게시글 태그 생성 (PostTag.createAll)")
class PostTagTest {

    @Test
    @DisplayName("trim 후 빈 문자열을 걸러내고 중복을 제거한다(trim 뒤에 distinct)")
    void normalize() {
        Set<String> input = new LinkedHashSet<>(List.of("강릉", "강릉 ", "  ", "맛집"));

        List<PostTag> tags = PostTag.createAll(1L, input);

        assertThat(tags).extracting(PostTag::getTagName).containsExactly("강릉", "맛집");
        assertThat(tags).allSatisfy(tag -> assertThat(tag.getPostId()).isEqualTo(1L));
    }

    @Test
    @DisplayName("모두 공백이면 빈 리스트를 반환한다")
    void allBlank() {
        List<PostTag> tags = PostTag.createAll(1L, new LinkedHashSet<>(List.of(" ", "   ")));

        assertThat(tags).isEmpty();
    }

    @Test
    @DisplayName("postId가 null이면 POST_TAG_INVALID를 던진다")
    void nullPostId() {
        assertThatThrownBy(() -> PostTag.createAll(null, Set.of("맛집")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_TAG_INVALID);
    }
}
