package com.collabos.backend.controller;

import com.collabos.backend.dto.ChannelResponse;
import com.collabos.backend.dto.CreateChannelRequest;
import com.collabos.backend.dto.OpenDirectRequest;
import com.collabos.backend.entity.Channel;
import com.collabos.backend.security.JwtService.AuthenticatedUser;
import com.collabos.backend.service.ChannelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{orgId}/channels")
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    @GetMapping
    public List<ChannelResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long orgId) {
        return channelService.listForOrg(orgId, currentUser.id());
    }

    @PostMapping
    public ResponseEntity<ChannelResponse> create(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @Valid @RequestBody CreateChannelRequest request) {
        Channel channel = channelService.createPublicChannel(orgId, currentUser, request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelResponse.of(channel, channel.getName()));
    }

    @PostMapping("/direct")
    public ResponseEntity<ChannelResponse> openDirect(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long orgId,
            @Valid @RequestBody OpenDirectRequest request) {
        Channel channel = channelService.openDirect(orgId, currentUser, request.userId());
        String displayName = channelService.resolveDisplayName(channel, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ChannelResponse.of(channel, displayName));
    }
}
