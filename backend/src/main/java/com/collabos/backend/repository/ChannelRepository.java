package com.collabos.backend.repository;

import com.collabos.backend.entity.Channel;
import com.collabos.backend.entity.ChannelType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    @EntityGraph(attributePaths = "createdBy")
    List<Channel> findByOrganizationIdAndTypeOrderByNameAsc(Long organizationId, ChannelType type);

    Optional<Channel> findByIdAndOrganizationId(Long id, Long organizationId);
}
