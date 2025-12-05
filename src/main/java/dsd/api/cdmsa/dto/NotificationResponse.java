package dsd.api.cdmsa.dto;

import java.time.Instant;

import dsd.api.cdmsa.model.Notification;
import dsd.api.cdmsa.model.Notification.NotificationType;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String details,
        Instant createdAt,
        boolean read,
        Long commentId,
        Long rfcId) {

    public static NotificationResponse from(Notification noti) {
        return new NotificationResponse(
                noti.getId(),
                noti.getType(),
                noti.getMessage(),
                noti.getDetails(),
                noti.getCreatedAt(),
                noti.isRead(),
                noti.getCommentId(),
                noti.getRfcId());
    }
}
