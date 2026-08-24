package com.collabos.backend.service;

import com.collabos.backend.dto.ChannelResponse;
import com.collabos.backend.entity.*;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.ChannelMemberRepository;
import com.collabos.backend.repository.ChannelRepository;
import com.collabos.backend.repository.UserRepository;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final UserRepository userRepository;
    private final OrganizationService organizationService;
    private final MembershipService membershipService;

    public ChannelService(
            ChannelRepository channelRepository,
            ChannelMemberRepository channelMemberRepository,
            UserRepository userRepository,
            OrganizationService organizationService,
            MembershipService membershipService) {
        this.channelRepository = channelRepository;
        this.channelMemberRepository = channelMemberRepository;
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.membershipService = membershipService;
    }

    public List<ChannelResponse> listForOrg(Long organizationId, Long currentUserId) {
        membershipService.requireMembership(organizationId, currentUserId);

        List<ChannelResponse> result = new ArrayList<>();
        for (Channel channel : channelRepository.findByOrganizationIdAndTypeOrderByNameAsc(organizationId, ChannelType.PUBLIC)) {
            result.add(ChannelResponse.of(channel, channel.getName()));
        }
        for (ChannelMember membership : channelMemberRepository.findByUserIdAndChannel_TypeOrderByJoinedAtAsc(currentUserId, ChannelType.DIRECT)) {
            Channel channel = membership.getChannel();
            if (!channel.getOrganization().getId().equals(organizationId)) {
                continue;
            }
            result.add(ChannelResponse.of(channel, resolveDirectDisplayName(channel.getId(), currentUserId)));
        }
        return result;
    }

    public Channel createPublicChannel(Long organizationId, AuthenticatedUser currentUser, String name) {
        requireEditor(organizationId, currentUser.id());
        User creator = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Channel channel = new Channel();
        channel.setOrganization(organizationService.getById(organizationId));
        channel.setName(name);
        channel.setType(ChannelType.PUBLIC);
        channel.setCreatedBy(creator);
        channel = channelRepository.save(channel);

        addMember(channel, creator);
        return channel;
    }

    public Channel openDirect(Long organizationId, AuthenticatedUser currentUser, Long otherUserId) {
        requireEditor(organizationId, currentUser.id());
        if (otherUserId.equals(currentUser.id())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You can't start a direct message with yourself");
        }
        // The other person must also be a member of this org — a DM is scoped to a shared workspace.
        membershipService.requireMembership(organizationId, otherUserId);

        Optional<Long> existingChannelId = channelMemberRepository.findDirectChannelId(organizationId, currentUser.id(), otherUserId);
        if (existingChannelId.isPresent()) {
            return channelRepository.findById(existingChannelId.get())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));
        }

        User me = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        Channel channel = new Channel();
        channel.setOrganization(organizationService.getById(organizationId));
        channel.setType(ChannelType.DIRECT);
        channel.setCreatedBy(me);
        channel = channelRepository.save(channel);

        addMember(channel, me);
        addMember(channel, other);
        return channel;
    }

    public Channel getVerifiedChannel(Long organizationId, Long channelId, Long currentUserId) {
        membershipService.requireMembership(organizationId, currentUserId);
        Channel channel = channelRepository.findByIdAndOrganizationId(channelId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Channel not found"));

        if (channel.getType() == ChannelType.DIRECT
                && !channelMemberRepository.existsByChannelIdAndUserId(channelId, currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You don't have access to this conversation");
        }
        return channel;
    }

    public String resolveDisplayName(Channel channel, Long currentUserId) {
        return channel.getType() == ChannelType.PUBLIC
                ? channel.getName()
                : resolveDirectDisplayName(channel.getId(), currentUserId);
    }

    void addMemberIfAbsent(Channel channel, User user) {
        if (!channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), user.getId())) {
            addMember(channel, user);
        }
    }

    private void addMember(Channel channel, User user) {
        ChannelMember member = new ChannelMember();
        member.setChannel(channel);
        member.setUser(user);
        channelMemberRepository.save(member);
    }

    private String resolveDirectDisplayName(Long channelId, Long currentUserId) {
        return channelMemberRepository.findByChannelId(channelId).stream()
                .map(ChannelMember::getUser)
                .filter(user -> !user.getId().equals(currentUserId))
                .map(User::getName)
                .findFirst()
                .orElse("Direct Message");
    }

    private void requireEditor(Long organizationId, Long userId) {
        if (membershipService.requireMembership(organizationId, userId).getRole() == Role.VIEWER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Viewers cannot do this");
        }
    }
}
