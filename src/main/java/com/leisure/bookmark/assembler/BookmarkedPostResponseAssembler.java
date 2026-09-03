package com.leisure.bookmark.assembler;

import com.leisure.bookmark.dto.response.BookmarkedPostResponse;
import com.leisure.bookmark.dto.result.BookmarkedPostResult;
import com.leisure.tag.service.TagReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookmarkedPostResponseAssembler {

    private final TagReader tagReader;

    public List<BookmarkedPostResponse> assembleBookmarkedPosts(List<BookmarkedPostResult> results) {
        // 빈 목록이면 태그 조회 없이 즉시 빈 리스트
        if (results.isEmpty()) {
            return List.of();
        }

        // postId들 -> { postId : [태그이름들] } 지도 (태그 조회 1번)
        Map<Long, List<String>> tagMap = tagReader.findTagMap(results.stream()
                .map(bookmarkedPostResult -> bookmarkedPostResult.postId())
                .toList());

        // 각 카드에 자기 postId의 태그를 병합 (태그 없는 글은 빈 리스트)
        return results.stream()
                .map(r -> BookmarkedPostResponse.from(r, tagMap.getOrDefault(r.postId(), List.of())))
                .toList();
    }
}
