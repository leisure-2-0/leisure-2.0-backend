package com.leisure.search.engine;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.util.List;

@Document(indexName = "posts-v1", createIndex = false)
@Setting(settingPath = "elasticsearch/post-setting.json")
@Mapping(mappingPath = "elasticsearch/post-mapping.json")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class PostSearchDocument {

    @Id
    private Long postId;

    private String title;

    private List<String> tags;

    private String category;

    private String region;

    private PostSearchDocument(Long postId, String title, List<String> tags, String category, String region) {
        this.postId = postId;
        this.title = title;
        this.tags = tags;
        this.category = category;
        this.region = region;
    }

    public static PostSearchDocument of(Long postId, String title, List<String> tags, String category, String region) {
        return new PostSearchDocument(postId, title, tags, category, region);
    }
}
