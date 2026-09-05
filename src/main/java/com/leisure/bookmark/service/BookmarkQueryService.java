package com.leisure.bookmark.service;

import com.leisure.bookmark.assembler.BookmarkedPostResponseAssembler;
import com.leisure.bookmark.domain.BookmarkedPostSort;
import com.leisure.bookmark.dto.response.BookmarkedPostListResponse;
import com.leisure.bookmark.dto.response.BookmarkedPostResponse;
import com.leisure.bookmark.dto.result.BookmarkedPostResult;
import com.leisure.bookmark.repository.BookmarkRepository;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.member.service.MemberReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkQueryService {

    private final MemberReader reader;

    private final BookmarkRepository repository;

    private final BookmarkedPostResponseAssembler assembler;

    @Transactional(readOnly = true)
    public BookmarkedPostListResponse getBookmarkedPosts(String publicId, BookmarkedPostSort sort, Integer page, Integer size) {
        Long memberId = reader.getMemberByPublicId(publicId).getMemberId();

        int pageNumber = validatePage(page);
        int pageSize = validateSize(size);

        long offset = (long) pageNumber * pageSize;

        List<BookmarkedPostResult> results = repository.findBookmarkedPosts(memberId, sort, offset, pageSize);

        List<BookmarkedPostResponse> bookmarkedPosts = assembler.assembleBookmarkedPosts(results);

        long totalElements = repository.countBookmarkedPosts(memberId);

        int totalPages = calculateTotalPages(totalElements, pageSize);

        boolean hasNext = pageNumber + 1 < totalPages;

        return new BookmarkedPostListResponse(pageNumber, pageSize, totalElements, totalPages, hasNext, bookmarkedPosts);
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
