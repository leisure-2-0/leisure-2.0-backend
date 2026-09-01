package com.leisure.postlike.service;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import com.leisure.postlike.assembler.LikedPostResponseAssembler;
import com.leisure.postlike.domain.LikedPostSort;
import com.leisure.postlike.dto.response.LikedPostListResponse;
import com.leisure.postlike.dto.response.LikedPostResponse;
import com.leisure.postlike.dto.result.LikedPostResult;
import com.leisure.postlike.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostLikeQueryService {

    private final MemberReader reader;

    private final PostLikeRepository repository;

    private final LikedPostResponseAssembler assembler;

    @Transactional(readOnly = true)
    public LikedPostListResponse getLikedPosts(String publicId, LikedPostSort sort, Integer page, Integer size) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        int pageNumber = validatePage(page);
        int pageSize = validateSize(size);

        long offset = (long) pageNumber * pageSize;

        List<LikedPostResult> results = repository.findLikedPosts(memberId, sort, offset, pageSize);

        List<LikedPostResponse> likedPosts = assembler.assembleLikedPosts(results);

        long totalElements = repository.countLikedPosts(memberId);

        int totalPages = calculateTotalPages(totalElements, pageSize);

        boolean hasNext = pageNumber + 1 < totalPages;

        return new LikedPostListResponse(likedPosts, pageNumber, pageSize, totalElements, totalPages, hasNext);
    }

    private int validatePage(Integer page) {
        if (page == null) {
            return 0;
        }

        if (page < 0) {
            throw new BusinessException(ErrorCode.PAGE_INVALID);
        }

        return page;
    }

    private int validateSize(Integer size) {
        if (size == null) {
            return 10;
        }

        if (size < 1 || size > 30) {
            throw new BusinessException(ErrorCode.PAGE_SIZE_INVALID);
        }

        return size;
    }

    private int calculateTotalPages(long totalElements, int size) {
        double result = (double) totalElements / size;
        return (int) Math.ceil(result);
    }
}
