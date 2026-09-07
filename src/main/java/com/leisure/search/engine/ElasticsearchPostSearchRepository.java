package com.leisure.search.engine;

import com.leisure.search.engine.PostSearchDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ElasticsearchPostSearchRepository implements PostSearchRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void initIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(PostSearchDocument.class);

        if (!indexOps.exists()) {
            indexOps.createWithMapping();
        }
    }

    @Override
    public void index(Collection<PostSearchDocument> documents) {

        if (documents.isEmpty()) {
            return;
        }

        elasticsearchOperations.save(documents);
    }

    @Override
    public void delete(Collection<Long> postIds) {

        if (postIds.isEmpty()) {
            return;
        }

        List<String> stringIds = postIds.stream().map(obj -> String.valueOf(obj)).toList();

        Query idsQuery = NativeQuery.builder()
                .withQuery(q -> q.ids(ids -> ids.values(stringIds)))
                .build();

        DeleteQuery deleteQuery = DeleteQuery.builder(idsQuery).build();

        elasticsearchOperations.delete(deleteQuery, PostSearchDocument.class);
    }
}
