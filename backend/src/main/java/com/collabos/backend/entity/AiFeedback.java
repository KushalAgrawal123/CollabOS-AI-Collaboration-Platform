package com.collabos.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "ai_feedback",
        indexes = @Index(name = "idx_ai_feedback_project_id", columnList = "project_id"))
@Getter
@Setter
@NoArgsConstructor
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false)
    private AiAgentType agentType;

    // Null for the Project Manager Agent, which isn't answering a specific question.
    @Column(columnDefinition = "text")
    private String question;

    @Column(nullable = false, columnDefinition = "text")
    private String answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiFeedbackRating rating;

    // Optional free-text correction when the user marks an answer DOWN —
    // this is the raw material for future prompt/eval improvements.
    @Column(columnDefinition = "text")
    private String correction;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
