package com.paypal.notification_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paypal.notification_service.dto.TransactionEvent;
import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.enums.NotificationStatus;
import com.paypal.notification_service.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(NotificationConsumer.class);

    public NotificationConsumer(NotificationRepository notificationRepository,ObjectMapper objectMapper){
        this.notificationRepository=notificationRepository;
        this.objectMapper=objectMapper;
    }
    @KafkaListener(topics = "txn-initiated", groupId = "notification-group")
    public void listen(TransactionEvent event){
        if ("SUCCESS".equals(event.getStatus())){

            try{
            Notification notification=new Notification();
            notification.setUserId(notification.getUserId());
            notification.setMessage(notification.getMessage());
            notification.setDeliveredAt(LocalDateTime.now());
            notification.setNotificationStatus(NotificationStatus.SENT);

            notificationRepository.save(notification);

            log.info("Notification saved for user: {}",event.getReceiverId());

        }catch (Exception ex) {
                log.error("Error processing event: {}", event, ex);
                throw ex;
            }
        }
    }
}
