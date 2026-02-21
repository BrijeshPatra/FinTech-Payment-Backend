package com.fintech_backend.reward_service.service;

import com.fintech_backend.reward_service.dto.TransactionEvent;
import com.fintech_backend.reward_service.entity.Reward;

public interface RewardService {

    void processReward(TransactionEvent event);

}
