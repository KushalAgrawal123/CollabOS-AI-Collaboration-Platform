package com.collabos.backend.service;

import com.collabos.backend.dto.NotificationResponse;
import com.collabos.backend.entity.Membership;
import com.collabos.backend.entity.Notification;
import com.collabos.backend.entity.NotificationType;
import com.collabos.backend.entity.Organization;
import com.collabos.backend.exception.ApiException;
import com.collabos.backend.repository.MembershipRepository;
import com.collabos.backend.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MembershipRepository membershipRepository;

    public NotificationService(NotificationRepository notificationRepository, MembershipRepository membershipRepository) {
        this.notificationRepository = notificationRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * The fan-out: one event in, one Notification row per other org member out.
     * Called from Kafka consumers, which run as a trusted background process —
     * there's no "current user" here to gate against, unlike everywhere else
     * MembershipRepository gets used.
     */
    public void notifyOrganizationMembersExcept(
            Long organizationId, Long actingUserId, NotificationType type, String message, String link) {
        List<Membership> memberships = membershipRepository.findByOrganizationIdOrderByJoinedAtAsc(organizationId);
        if (memberships.isEmpty()) {
            return;
        }
        Organization organization = memberships.get(0).getOrganization();

        for (Membership membership : memberships) {
            if (membership.getUser().getId().equals(actingUserId)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setOrganization(organization);
            notification.setRecipient(membership.getUser());
            notification.setType(type);
            notification.setMessage(message);
            notification.setLink(link);
            notificationRepository.save(notification);
        }
    }

    public List<NotificationResponse> listForUser(Long organizationId, Long userId) {
        return notificationRepository.findTop30ByRecipientIdAndOrganizationIdOrderByCreatedAtDesc(userId, organizationId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long unreadCount(Long organizationId, Long userId) {
        return notificationRepository.countByRecipientIdAndOrganizationIdAndReadFalse(userId, organizationId);
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllRead(Long organizationId, Long userId) {
        notificationRepository.markAllRead(userId, organizationId);
    }
}
