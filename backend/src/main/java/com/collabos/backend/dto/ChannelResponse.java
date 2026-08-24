package com.collabos.backend.dto;

import com.collabos.backend.entity.Channel;
import com.collabos.backend.entity.ChannelType;

public record ChannelResponse(Long id, String displayName, ChannelType type) {
    public static ChannelResponse of(Channel channel, String displayName) {
        return new ChannelResponse(channel.getId(), displayName, channel.getType());
    }
}
