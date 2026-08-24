package com.collabos.backend.repository;

import com.collabos.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop30ByRecipientIdAndOrganizationIdOrderByCreatedAtDesc(Long recipientId, Long organizationId);

    long countByRecipientIdAndOrganizationIdAndReadFalse(Long recipientId, Long organizationId);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    @Modifying
    @Query("update Notification n set n.read = true where n.recipient.id = :recipientId and n.organization.id = :organizationId and n.read = false")
    void markAllRead(@Param("recipientId") Long recipientId, @Param("organizationId") Long organizationId);
}
