package com.leisure.member.service;

import com.leisure.bookmark.repository.BookmarkRepository;
import com.leisure.member.repository.MemberRepository;
import com.leisure.pointhistory.repository.PointHistoryRepository;
import com.leisure.post.repository.PostRepository;
import com.leisure.postlike.repository.PostLikeRepository;
import com.leisure.tag.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPurgeService {

    private final MemberRepository memberRepository;

    private final PostRepository postRepository;

    private final TagRepository tagRepository;

    private final PostLikeRepository postLikeRepository;

    private final BookmarkRepository bookmarkRepository;

    private final PointHistoryRepository pointHistoryRepository;


    @Transactional
    public int purgeWithdrawnMembers() {

        LocalDateTime threshold = LocalDateTime.now().minusDays(30);

        List<Long> memberIds = memberRepository.findMemberIdsByDeletedAtBefore(threshold);

        if (memberIds.isEmpty()) {
            return 0;
        }

        List<Long> postIds = postRepository.findPostIdsByMemberIdIn(memberIds);

        if (!postIds.isEmpty()) {
            tagRepository.deleteByPostIdIn(postIds);

            postLikeRepository.deleteByPostIdIn(postIds);

            bookmarkRepository.deleteByPostIdIn(postIds);
        }

        postLikeRepository.deleteByMemberIdIn(memberIds);

        bookmarkRepository.deleteByMemberIdIn(memberIds);

        pointHistoryRepository.deleteByMemberIdIn(memberIds);

        postRepository.deleteByMemberIdIn(memberIds);

        memberRepository.deleteByMemberIdIn(memberIds);

        return memberIds.size();
    }
}
