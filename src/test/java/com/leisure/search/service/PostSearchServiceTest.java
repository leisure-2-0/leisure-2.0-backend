package com.leisure.search.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.properties.SearchProperties;
import com.leisure.member.service.MemberReader;
import com.leisure.post.assembler.PostResponseAssembler;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.PostResult;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.dto.response.PostSearchResponse;
import com.leisure.search.engine.PostSearchDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("게시글 검색 (PostSearchService.search, ES + 오프셋)")
class PostSearchServiceTest {

    @Mock
    private MemberReader memberReader;

    @Mock
    private SearchProperties searchProperties;

    @Mock
    private PostRepository postRepository;

    @Mock
    private org.springframework.data.elasticsearch.core.ElasticsearchOperations elasticsearchOperations;

    @Mock
    private PostResponseAssembler postResponseAssembler;

    @InjectMocks
    private PostSearchService postSearchService;

    // --- 헬퍼 -------------------------------------------------------------

    private PostSearchDocument doc(long postId) {
        return PostSearchDocument.of(postId, "제목", List.of(), "RESTAURANT", "강릉", 0, null);
    }

    private PostResult post(long postId) {
        return new PostResult(
                postId, "제목", null, 0, 0, 0, false, false, "강릉", null,
                new PostResult.AuthorResult(1L, "nick", null)
        );
    }

    @SuppressWarnings("unchecked")
    private SearchHit<PostSearchDocument> hit(long postId) {
        SearchHit<PostSearchDocument> hit = mock(SearchHit.class);
        given(hit.getContent()).willReturn(doc(postId));
        return hit;
    }

    // buildQuery가 쓰는 프로퍼티만 스텁 (해피패스에서만 호출)
    private void stubSearchProperties() {
        given(searchProperties.boost()).willReturn(new SearchProperties.Boost(4f, 2f, 1f, 1f));
        given(searchProperties.fuzziness()).willReturn("AUTO");
        given(searchProperties.fuzz()).willReturn(new SearchProperties.Fuzz(25, 0));
    }

    @SuppressWarnings("unchecked")
    private void stubEs(List<SearchHit<PostSearchDocument>> hits, long totalHits) {
        SearchHits<PostSearchDocument> searchHits = mock(SearchHits.class);
        given(searchHits.getSearchHits()).willReturn(hits);
        given(searchHits.getTotalHits()).willReturn(totalHits);
        given(elasticsearchOperations.search(any(Query.class), eq(PostSearchDocument.class))).willReturn(searchHits);
    }

    // 어셈블러는 조회 결과를 순서 그대로 응답으로 넘긴다고 가정 (태그 병합은 어셈블러 테스트에서)
    private void stubAssembler() {
        given(postResponseAssembler.assemblePosts(any())).willAnswer(invocation -> {
            List<PostResult> results = invocation.getArgument(0);
            return results.stream().map(r -> PostResponse.from(r, List.of())).toList();
        });
    }

    // --- 검증 실패 (ES/프로퍼티 목 불필요 — 가드가 buildQuery 전에 던짐) --------------

    @Nested
    @DisplayName("검증 실패")
    class Validation {

        @Test
        @DisplayName("검색어가 비면 SEARCH_KEYWORD_REQUIRED")
        void blankKeyword() {
            assertThatThrownBy(() -> postSearchService.search(null, "  ", null, SearchSort.ACCURACY, 0, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SEARCH_KEYWORD_REQUIRED);
        }

        @Test
        @DisplayName("page가 음수면 PAGE_INVALID")
        void negativePage() {
            assertThatThrownBy(() -> postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, -1, 10))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAGE_INVALID);
        }

        @Test
        @DisplayName("size가 범위를 벗어나면 PAGE_SIZE_INVALID")
        void invalidSize() {
            assertThatThrownBy(() -> postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, 0, 31))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.PAGE_SIZE_INVALID);
        }

        @Test
        @DisplayName("from+size가 10000을 넘으면 SEARCH_PAGE_TOO_DEEP")
        void tooDeepPage() {
            // (1000+1) * 15 = 15015 > 10000
            assertThatThrownBy(() -> postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, 1000, 15))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SEARCH_PAGE_TOO_DEEP);
        }
    }

    // --- 검색 성공 ---------------------------------------------------------

    @Nested
    @DisplayName("검색 성공")
    class Success {

        @Test
        @DisplayName("ES가 준 순서를 RDB 결과 순서와 무관하게 복원한다")
        void restoresEsOrder() {
            stubSearchProperties();
            stubEs(List.of(hit(7L), hit(3L)), 2L);                 // ES 순서: 7, 3
            given(postRepository.findByPostIds(any(), any()))
                    .willReturn(List.of(post(3L), post(7L)));       // RDB는 뒤섞여 옴: 3, 7
            stubAssembler();

            PostSearchResponse response = postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, 0, 10);

            assertThat(response.content()).extracting(PostResponse::postId)
                    .containsExactly(7L, 3L);                       // ES 순서로 복원
            assertThat(response.totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("ES엔 있으나 RDB에 없는(삭제/비공개) postId는 건너뛴다")
        void skipsMissing() {
            stubSearchProperties();
            stubEs(List.of(hit(7L), hit(3L), hit(9L)), 3L);        // ES: 7, 3, 9
            given(postRepository.findByPostIds(any(), any()))
                    .willReturn(List.of(post(7L), post(3L)));       // 9번은 RDB에 없음

            stubAssembler();

            PostSearchResponse response = postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, 0, 10);

            assertThat(response.content()).extracting(PostResponse::postId)
                    .containsExactly(7L, 3L);                       // 9 제외
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 content + RDB 조회 안 함")
        void emptyResult() {
            stubSearchProperties();
            stubEs(List.of(), 0L);

            PostSearchResponse response = postSearchService.search(null, "강릉", null, SearchSort.ACCURACY, 0, 10);

            assertThat(response.content()).isEmpty();
            assertThat(response.totalElements()).isZero();
            assertThat(response.totalPages()).isZero();
            assertThat(response.hasNext()).isFalse();
        }
    }
}
