package com.leisure.tag.service;

import com.leisure.tag.domain.PostTag;
import com.leisure.tag.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("태그 조회/그룹핑 (TagReader)")
class TagReaderTest {

    @Mock
    private TagRepository tagRepository;

    @InjectMocks
    private TagReader tagReader;

    private List<PostTag> tagsOf(Long postId, String... names) {
        return PostTag.createAll(postId, new LinkedHashSet<>(List.of(names)));
    }

    @Test
    @DisplayName("findTagMap은 평평한 태그 행들을 postId별 리스트로 묶는다(N+1 회피)")
    void findTagMap_groupsByPostId() {
        List<PostTag> rows = Stream.of(tagsOf(1L, "맛집", "강릉"), tagsOf(2L, "카페"))
                .flatMap(List::stream)
                .toList();
        given(tagRepository.findByPostIdIn(List.of(1L, 2L))).willReturn(rows);

        Map<Long, List<String>> map = tagReader.findTagMap(List.of(1L, 2L));

        assertThat(map.get(1L)).containsExactlyInAnyOrder("맛집", "강릉");
        assertThat(map.get(2L)).containsExactly("카페");
    }

    @Test
    @DisplayName("findTagMap은 태그가 없는 postId는 지도에 넣지 않는다(어셈블러가 빈 리스트로 처리)")
    void findTagMap_absentPostId() {
        given(tagRepository.findByPostIdIn(List.of(1L, 2L))).willReturn(tagsOf(1L, "맛집"));

        Map<Long, List<String>> map = tagReader.findTagMap(List.of(1L, 2L));

        assertThat(map).containsOnlyKeys(1L);
        assertThat(map.get(2L)).isNull();
    }

    @Test
    @DisplayName("findTags는 단건 글의 태그 이름 목록을 반환한다")
    void findTags_single() {
        given(tagRepository.findByPostIdIn(List.of(1L))).willReturn(tagsOf(1L, "맛집", "강릉"));

        List<String> tags = tagReader.findTags(1L);

        assertThat(tags).containsExactlyInAnyOrder("맛집", "강릉");
    }
}
