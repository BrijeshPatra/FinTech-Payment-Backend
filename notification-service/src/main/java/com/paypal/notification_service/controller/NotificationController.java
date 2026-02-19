package com.paypal.notification_service.controller;

import com.paypal.notification_service.entity.Notification;
import com.paypal.notification_service.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notify")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService){
        this.notificationService=notificationService;
    }
    @PostMapping
    public Notification sendNotification(@RequestBody Notification notification){
        return notificationService.sendNotification(notification);
    }

    @GetMapping("/{userId}")
    public List<Notification> getNotificationByUser(@PathVariable String userId){
        return notificationService.getNotificationByUser(userId);
    }

}
