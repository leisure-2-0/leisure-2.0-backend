package com.leisure.pointhistory.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.repository.MemberRepository;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.repository.PointHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("포인트 적립 (PointHistoryService.earn)")
class PointHistoryServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointHistoryService service;

    private static final Long MEMBER_ID = 1L;   // 수령자(작성자)
    private static final Long ACTOR_ID = 2L;    // 행위자
    private static final Long SOURCE_ID = 10L;  // 게시글
    private static final PointType TYPE = PointType.LIKE_RECEIVED;  // amount 2

    @Test
    @DisplayName("신규 적립이면 원장 삽입 후 작성자 포인트를 증가시킨다")
    void earn_success() {
        given(pointHistoryRepository.insertIfAbsent(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE.name(), TYPE.getAmount()))
                .willReturn(1);
        given(memberRepository.addPoint(MEMBER_ID, TYPE.getAmount())).willReturn(1);

        service.earn(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE);

        verify(memberRepository).addPoint(MEMBER_ID, TYPE.getAmount());
    }

    @Test
    @DisplayName("이미 적립된 건이면(유니크 위반 무시, 0행) 포인트를 증가시키지 않는다")
    void earn_alreadyGranted() {
        given(pointHistoryRepository.insertIfAbsent(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE.name(), TYPE.getAmount()))
                .willReturn(0);

        service.earn(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE);

        verify(memberRepository, never()).addPoint(anyLong(), anyInt());
    }

    @Test
    @DisplayName("수령자가 없거나 탈퇴해 증가가 0행이면 MEMBER_NOT_FOUND로 롤백시킨다")
    void earn_receiverGone() {
        given(pointHistoryRepository.insertIfAbsent(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE.name(), TYPE.getAmount()))
                .willReturn(1);
        given(memberRepository.addPoint(MEMBER_ID, TYPE.getAmount())).willReturn(0);

        assertThatThrownBy(() -> service.earn(MEMBER_ID, ACTOR_ID, SOURCE_ID, TYPE))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }
}
