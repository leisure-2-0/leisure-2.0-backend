package com.leisure.search.engine;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "posts-v1", createIndex = false, writeTypeHint = WriteTypeHint.FALSE)
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

    private Integer likeCount;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant publishedAt;

    private PostSearchDocument(Long postId, String title, List<String> tags, String category, String region,
                              Integer likeCount, Instant publishedAt) {
        this.postId = postId;
        this.title = title;
        this.tags = tags;
        this.category = category;
        this.region = region;
        this.likeCount = likeCount;
        this.publishedAt = publishedAt;
    }

    public static PostSearchDocument of(Long postId, String title, List<String> tags, String category, String region,
                                        Integer likeCount, Instant publishedAt) {
        return new PostSearchDocument(postId, title, tags, category, region, likeCount, publishedAt);
    }
}
