package com.leisure.search.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.properties.SearchProperties;
import com.leisure.member.service.MemberReader;
import com.leisure.post.assembler.PostResponseAssembler;
import com.leisure.post.domain.PostCategory;
import com.leisure.post.dto.response.PostResponse;
import com.leisure.post.dto.result.PostResult;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.dto.response.PostSearchResponse;
import com.leisure.search.engine.PostSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostSearchService {

    private final MemberReader memberReader;

    private final SearchProperties searchProperties;

    private final PostRepository postRepository;

    private final ElasticsearchOperations elasticsearchOperations;

    private final PostResponseAssembler postResponseAssembler;

    public PostSearchResponse search(String publicId, String searchTerm, PostCategory category, SearchSort sort, Integer page, Integer size) {

        if (searchTerm == null || searchTerm.isBlank()) {
            throw new BusinessException(ErrorCode.SEARCH_KEYWORD_REQUIRED);
        }

        Long memberId = null;

        if (publicId != null) {
            memberId = memberReader.getMemberByPublicId(publicId).getMemberId();
        }

        int pageNumber = validatePage(page);
        int pageSize = validateSize(size);

        if (((long) pageNumber + 1) * pageSize > 10000) {
            throw new BusinessException(ErrorCode.SEARCH_PAGE_TOO_DEEP);
        }

        NativeQuery nativeQuery = buildQuery(searchTerm, category, sort, pageNumber, pageSize);

        SearchHits<PostSearchDocument> hits = elasticsearchOperations.search(nativeQuery, PostSearchDocument.class);

        List<SearchHit<PostSearchDocument>> searchHits = hits.getSearchHits();

        long totalElements = hits.getTotalHits();

        int totalPages = calculateTotalPages(totalElements, pageSize);

        boolean hasNext = pageNumber + 1 < totalPages;

        return new PostSearchResponse(hydratePosts(memberId, searchHits), pageNumber, pageSize, totalElements, totalPages, hasNext);
    }

    private NativeQuery buildQuery(String searchTerm, PostCategory category, SearchSort sort, int page, int size) {

        List<String> fields = List.of(
                "title^" + searchProperties.boost().title(),
                "tags.text^" + searchProperties.boost().tags(),
                "region.text^" + searchProperties.boost().region()
        );

        Query multiMatch = MultiMatchQuery.of(m -> {
            return m.query(searchTerm)
                    .type(TextQueryType.BoolPrefix)
                    .fields(fields)
                    .fuzziness(searchProperties.fuzziness())
                    .maxExpansions(searchProperties.fuzz().maxExpansions())
                    .prefixLength(searchProperties.fuzz().prefixLength());
                    // .tieBreaker(0.2);

        })._toQuery();

        Query resultQuery = (category == null)? multiMatch : BoolQuery.of(b -> b.must(multiMatch)
                .filter(f -> f.term(t -> t.field("category").value(category.name()))))._toQuery();

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(resultQuery)
                .withPageable(PageRequest.of(page, size));

        if (sort == null) {
            sort = SearchSort.ACCURACY;
        }

        switch (sort) {
            case LATEST  -> builder.withSort(s -> s.field(f -> f.field("publishedAt").order(SortOrder.Desc)));
            case POPULAR -> builder.withSort(s -> s.field(f -> f.field("likeCount").order(SortOrder.Desc)));
            case ACCURACY -> builder.withSort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
        }
        builder.withSort(s -> s.field(f -> f.field("postId").order(SortOrder.Desc)));

        return builder.build();
    }

    private List<PostResponse> hydratePosts(Long memberId, List<SearchHit<PostSearchDocument>> hits) {

        List<Long> postIds = hits.stream()
                .map(m -> m.getContent().getPostId())
                .toList();

        if (postIds.isEmpty()) {
            return List.of();
        }

        List<PostResult> results = postRepository.findByPostIds(memberId, postIds);

        Map<Long, PostResult> postMap = results.stream()
                .collect(Collectors.toMap(m -> m.postId(), Function.identity()));

        List<PostResult> orderedPosts = postIds.stream()
                .map(postMap::get)
                .filter(obj -> Objects.nonNull(obj))
                .toList();

        return postResponseAssembler.assemblePosts(orderedPosts);
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
            return 15;
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



//        for (Long postId : postIds) {
//            PostResult r = postMap.get(postId);
//            if (r == null) continue;
//            list.add(r);
//        }

