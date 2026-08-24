package com.collabos.backend.dto;

import com.collabos.backend.entity.AiAgentType;
import com.collabos.backend.entity.AiFeedbackRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiFeedbackRequest(
        @NotNull AiAgentType agentType,
        String question,
        @NotBlank String answer,
        @NotNull AiFeedbackRating rating,
        String correction
) {
}
