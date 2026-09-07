package com.leisure.search.engine;


import com.leisure.search.engine.PostSearchDocument;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PostSearchRepository {

    void initIndex();

    void index(Collection<PostSearchDocument> documents);

    void delete(Collection<Long> postIds);
}
