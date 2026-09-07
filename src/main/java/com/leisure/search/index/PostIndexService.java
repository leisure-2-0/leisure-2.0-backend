package com.leisure.search.index;

import com.leisure.global.exception.BusinessException;
import com.leisure.global.exception.ErrorCode;
import com.leisure.global.properties.SearchProperties;
import com.leisure.post.domain.PostStatus;
import com.leisure.post.repository.PostRepository;
import com.leisure.search.engine.PostSearchDocument;
import com.leisure.search.engine.PostSearchRepository;
import com.leisure.tag.service.TagReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostIndexService {

    private final SearchProperties searchProperties;

    private final PostRepository postRepository;

    private final TagReader tagReader;

    private final PostSearchRepository postSearchRepository;

    private final PostIndexDirtyRepository postIndexDirtyRepository;


    public void sync() {

        List<PostIndexDirty> dirtyList = postIndexDirtyRepository.findProcessable(LocalDateTime.now(), searchProperties.sync().batchSize());
    }

}
