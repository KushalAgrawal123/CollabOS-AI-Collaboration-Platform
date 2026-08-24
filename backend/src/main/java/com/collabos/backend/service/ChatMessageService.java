package com.collabos.backend.service;

import com.collabos.backend.dto.ChatMessageResponse;
import com.collabos.backend.entity.Channel;
import com.collabos.backend.entity.ChannelType;
import com.collabos.backend.entity.ChatMessage;
import com.collabos.backend.entity.Role;
import com.collabos.backend.entity.User;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.ChatMessageRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.websocket.ChatBroadcastService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ChatMessageService {

    private static final int HISTORY_PAGE_SIZE = 50;

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChannelService channelService;
    private final MembershipService membershipService;
    private final ChatBroadcastService chatBroadcastService;

    public ChatMessageService(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            ChannelService channelService,
            MembershipService membershipService,
            ChatBroadcastService chatBroadcastService) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.channelService = channelService;
        this.membershipService = membershipService;
        this.chatBroadcastService = chatBroadcastService;
    }

    public List<ChatMessageResponse> list(Long organizationId, Long channelId, Long currentUserId) {
        channelService.getVerifiedChannel(organizationId, channelId, currentUserId);

        List<ChatMessage> recent = chatMessageRepository.findByChannelIdOrderByCreatedAtDesc(
                channelId, PageRequest.of(0, HISTORY_PAGE_SIZE));
        Collections.reverse(recent);
        return recent.stream().map(ChatMessageResponse::from).toList();
    }

    public ChatMessage create(Long organizationId, Long channelId, AuthenticatedUser currentUser, String body) {
        Channel channel = channelService.getVerifiedChannel(organizationId, channelId, currentUser.id());
        if (membershipService.requireMembership(organizationId, currentUser.id()).getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot send messages");
        }

        User author = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Posting in a public channel you haven't explicitly joined counts as joining —
        // keeps the member list meaning "who's actually participating" without a
        // separate join step getting in the way of just replying.
        if (channel.getType() == ChannelType.PUBLIC) {
            channelService.addMemberIfAbsent(channel, author);
        }

        ChatMessage message = new ChatMessage();
        message.setChannel(channel);
        message.setAuthor(author);
        message.setBody(body);
        message = chatMessageRepository.save(message);

        chatBroadcastService.broadcast(organizationId, channelId, ChatMessageResponse.from(message));
        return message;
    }
}
