package com.leisure.postlike.assembler;

import com.leisure.postlike.dto.response.LikedPostResponse;
import com.leisure.postlike.dto.result.LikedPostResult;
import com.leisure.tag.service.TagReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LikedPostResponseAssembler {

    private final TagReader tagReader;

    public List<LikedPostResponse> assembleLikedPosts(List<LikedPostResult> results) {
        // 빈 목록이면 태그 조회 없이 즉시 빈 리스트
        if (results.isEmpty()) {
            return List.of();
        }

        // postId들 -> { postId : [태그이름들] } 지도 (태그 조회 1번)
        Map<Long, List<String>> tagMap = tagReader.findTagMap(results.stream()
                .map(likedPostResult -> likedPostResult.postId())
                .toList());

        // 각 카드에 자기 postId의 태그를 병합 (태그 없는 글은 빈 리스트)
        return results.stream()
                .map(r -> LikedPostResponse.from(r, tagMap.getOrDefault(r.postId(), List.of())))
                .toList();
    }
}
