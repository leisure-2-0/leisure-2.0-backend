package com.leisure.pointhistory.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.repository.MemberRepository;
import com.leisure.pointhistory.domain.PointType;
import com.leisure.pointhistory.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final MemberRepository memberRepository;

    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void earn(Long memberId, Long actorId, Long sourceId, PointType pointType) {

        int inserted = pointHistoryRepository.insertIfAbsent(memberId, actorId, sourceId, pointType.name(), pointType.getAmount());

        if (inserted == 0) {
            return;
        }

        int updated = memberRepository.addPoint(memberId, pointType.getAmount());

        if (updated == 0) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
