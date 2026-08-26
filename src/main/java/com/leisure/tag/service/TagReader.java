package com.leisure.tag.service;

import com.leisure.tag.domain.PostTag;
import com.leisure.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TagReader {

    private final TagRepository tagRepository;

    public List<String> findTags(Long postId) {
        return tagRepository.findByPostIdIn(List.of(postId)).stream()
                .map(postTag -> postTag.getTagName())   // PostTag -> 태그이름
                .toList();
    }

    // 평평하게 나온 태그 행들을 postId별로 묶어 { postId : [태그이름들] } 지도로 만든다.
    // 다건 조회 시 글마다 태그를 따로 조회하는 N+1을 피하기 위한 배치 병합용 지도.
    public Map<Long, List<String>> findTagMap(List<Long> postIds) {
        List<PostTag> rows = tagRepository.findByPostIdIn(postIds);

        Map<Long, List<String>> tagMap = new HashMap<>();

        for (PostTag postTag : rows) {
            Long postId = postTag.getPostId();      // 분류 기준(key)
            String tagName = postTag.getTagName();  // 담을 값(value)

            // 이 postId가 처음이면 빈 리스트부터 만들어 넣는다.
            if (!tagMap.containsKey(postId)) {
                tagMap.put(postId, new ArrayList<>());
            }

            // 해당 postId 리스트에 태그이름을 쌓는다.
            tagMap.get(postId).add(tagName);
        }

        return tagMap;
    }
}
