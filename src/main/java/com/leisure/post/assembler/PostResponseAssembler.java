package com.leisure.post.assembler;

import com.leisure.post.dto.response.MainFeedPostResponse;
import com.leisure.post.dto.response.MyPostResponse;
import com.leisure.post.dto.response.PostDetailResponse;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.MainFeedPostResult;
import com.leisure.post.dto.result.MyPostResult;
import com.leisure.post.dto.result.PostDetailResult;
import com.leisure.post.dto.result.PostResult;
import com.leisure.tag.service.TagReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PostResponseAssembler {

    private final TagReader tagReader;

    public PostDetailResponse assembleDetail(PostDetailResult result) {
        // 이 글의 태그이름 리스트 조회 (단건이라 그룹핑 불필요)
        List<String> tags = tagReader.findTags(result.postId());

        // 조회 결과(result) + 태그 -> 최종 응답으로 조립
        return PostDetailResponse.from(result, tags);
    }

    public List<PostResponse> assemblePosts(List<PostResult> results) {
        // 빈 목록이면 태그 조회 없이 즉시 빈 리스트
        if (results.isEmpty()) {
            return List.of();
        }

        // 목록의 postId들 -> { postId : [태그이름들] } 지도 (태그 조회 1번)
        Map<Long, List<String>> tagMap = tagReader.findTagMap(results.stream()
                .map(postResult -> postResult.postId())
                .toList());

        // 각 결과에 자기 postId의 태그를 병합 (태그 없는 글은 빈 리스트)
        return results.stream()
                .map(r -> PostResponse.from(r, tagMap.getOrDefault(r.postId(), List.of())))
                .toList();
    }

    public List<MainFeedPostResponse> assembleMainFeed(List<MainFeedPostResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }

        // postId들 -> 태그 지도 (태그 조회 1번)
        Map<Long, List<String>> tagMap = tagReader.findTagMap(results.stream()
                .map(mainFeedPostResult -> mainFeedPostResult.postId())
                .toList());

        // 각 카드에 태그 병합 (태그 없는 글은 빈 리스트)
        return results.stream()
                .map(r -> MainFeedPostResponse.from(r, tagMap.getOrDefault(r.postId(), List.of())))
                .toList();
    }

    public List<MyPostResponse> assembleMyPosts(List<MyPostResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }

        // postId들 -> 태그 지도 (태그 조회 1번)
        Map<Long, List<String>> tagMap = tagReader.findTagMap(results.stream()
                .map(myPostResult -> myPostResult.postId())
                .toList());

        // 각 카드에 태그 병합 (태그 없는 글은 빈 리스트)
        return results.stream()
                .map(r -> MyPostResponse.from(r, tagMap.getOrDefault(r.postId(), List.of())))
                .toList();
    }
}
