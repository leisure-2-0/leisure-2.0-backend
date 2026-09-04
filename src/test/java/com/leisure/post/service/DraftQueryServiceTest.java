package com.leisure.post.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.domain.Member;
import com.leisure.member.service.MemberReader;
import com.leisure.post.assembler.PostResponseAssembler;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.response.DraftDetailResponse;
import com.leisure.post.dto.response.DraftListResponse;
import com.leisure.post.dto.result.DraftDetailResult;
import com.leisure.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("내 임시저장 조회 (PostQueryService.getMyDrafts / getMyDraftDetail)")
class DraftQueryServiceTest {

    @Mock
    private MemberReader reader;

    @Mock
    private PostRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PostResponseAssembler assembler;

    @InjectMocks
    private PostQueryService postQueryService;

    private static final String PUBLIC_ID = "public-id";
    private static final Long MEMBER_ID = 1L;
    private static final Long POST_ID = 10L;

    private Member member() {
        Member member = Member.create("user@leisure.com", "ENCODED", "nick");
        ReflectionTestUtils.setField(member, "memberId", MEMBER_ID);
        return member;
    }

    private DraftDetailResult result() {
        return new DraftDetailResult(
                POST_ID, "제목", "본문", PostCategory.RESTAURANT, LocalDateTime.now(),
                new DraftDetailResult.LocationResult("강릉", "장소명", "주소", 37.5, 127.0)
        );
    }

    @Test
    @DisplayName("초안 목록은 memberId로 조회한 결과를 그대로 반환한다")
    void getMyDrafts_success() {
        DraftListResponse draft = new DraftListResponse(POST_ID, "제목", PostCategory.RESTAURANT, LocalDateTime.now());
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMyDrafts(MEMBER_ID)).willReturn(List.of(draft));

        List<DraftListResponse> response = postQueryService.getMyDrafts(PUBLIC_ID);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).postId()).isEqualTo(POST_ID);
    }

    @Test
    @DisplayName("초안 상세는 조회 후 어셈블러로 태그를 병합해 반환한다")
    void getMyDraftDetail_success() {
        DraftDetailResult result = result();
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMyDraftsDetail(MEMBER_ID, POST_ID)).willReturn(Optional.of(result));
        given(assembler.assembleDraftDetail(result)).willReturn(DraftDetailResponse.from(result, List.of("강릉")));

        DraftDetailResponse response = postQueryService.getMyDraftDetail(PUBLIC_ID, POST_ID);

        assertThat(response.postId()).isEqualTo(POST_ID);
        assertThat(response.tags()).containsExactly("강릉");
    }

    @Test
    @DisplayName("초안이 없으면(없거나 남의 것) POST_NOT_FOUND 예외를 던진다")
    void getMyDraftDetail_notFound() {
        given(reader.getMemberByPublicId(PUBLIC_ID)).willReturn(member());
        given(repository.findMyDraftsDetail(MEMBER_ID, POST_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postQueryService.getMyDraftDetail(PUBLIC_ID, POST_ID))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
