package com.collabos.backend.repository;

import com.collabos.backend.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // Most-recent-first + a page size is how the service asks for "last 50" without
    // loading a channel's entire history; the service reverses the page for display.
    @EntityGraph(attributePaths = "author")
    List<ChatMessage> findByChannelIdOrderByCreatedAtDesc(Long channelId, Pageable pageable);
}
