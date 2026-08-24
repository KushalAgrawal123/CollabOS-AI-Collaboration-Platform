package com.collabos.backend.dto;

import com.collabos.backend.entity.AiAgentType;
import com.collabos.backend.entity.AiFeedback;
import com.collabos.backend.entity.AiFeedbackRating;

import java.time.Instant;

public record AiFeedbackResponse(
        Long id,
        AiAgentType agentType,
        String question,
        String answer,
        AiFeedbackRating rating,
        String correction,
        String userName,
        Instant createdAt
) {
    public static AiFeedbackResponse from(AiFeedback feedback) {
        return new AiFeedbackResponse(
                feedback.getId(),
                feedback.getAgentType(),
                feedback.getQuestion(),
                feedback.getAnswer(),
                feedback.getRating(),
                feedback.getCorrection(),
                feedback.getUser().getName(),
                feedback.getCreatedAt());
    }
}
