package com.paypal.notification_service.service;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;


    public NotificationServiceImpl(NotificationRepository notificationRepository){
        this.notificationRepository=notificationRepository;
    }

    @Override
    public Notification sendNotification(Notification notification) {
        notification.setDeliveredAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getNotificationByUser(String userId) {
        return notificationRepository.findByUserId(userId);
    }
}
