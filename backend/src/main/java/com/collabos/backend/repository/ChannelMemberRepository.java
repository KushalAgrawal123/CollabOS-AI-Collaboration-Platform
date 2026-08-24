package com.collabos.backend.repository;

import com.collabos.backend.entity.ChannelMember;
import com.collabos.backend.entity.ChannelType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelMemberRepository extends JpaRepository<ChannelMember, Long> {

    Optional<ChannelMember> findByChannelIdAndUserId(Long channelId, Long userId);

    boolean existsByChannelIdAndUserId(Long channelId, Long userId);

    @EntityGraph(attributePaths = "channel")
    List<ChannelMember> findByUserIdAndChannel_TypeOrderByJoinedAtAsc(Long userId, ChannelType type);

    @EntityGraph(attributePaths = "user")
    List<ChannelMember> findByChannelId(Long channelId);

    // Looks up an existing 1:1 DIRECT channel between exactly these two users, so opening
    // a DM with someone you've already messaged reuses the same channel and history
    // instead of creating a fresh, empty one every time.
    @Query("""
            select cm1.channel.id from ChannelMember cm1
            join ChannelMember cm2 on cm2.channel.id = cm1.channel.id
            where cm1.channel.organization.id = :organizationId
              and cm1.channel.type = com.collabos.backend.entity.ChannelType.DIRECT
              and cm1.user.id = :userId1
              and cm2.user.id = :userId2
            """)
    Optional<Long> findDirectChannelId(
            @Param("organizationId") Long organizationId,
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);
}
