package com.fintech_backend.reward_service.kafka;

import com.fintech_backend.reward_service.dto.TransactionEvent;
import com.fintech_backend.reward_service.service.RewardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RewardConsumer {
    private final RewardService rewardService;
    private static final Logger log=LoggerFactory.getLogger(RewardConsumer.class);

    public RewardConsumer(RewardService rewardService){
        this.rewardService=rewardService;
    }

    @KafkaListener(topics = "txn-initiated",groupId = "reward-group")
    public void consume(TransactionEvent event){
        log.info("Recieved Event from Kafka:{}",event);
        rewardService.processReward(event);
    }
}
